package org.sakaiproject.progress.impl;

import org.sakaiproject.assignment.api.model.Assignment;
import org.sakaiproject.assignment.api.model.AssignmentSubmission;
import org.sakaiproject.assignment.api.model.AssignmentSubmissionSubmitter;
import org.sakaiproject.progress.api.IProgress;
import org.sakaiproject.progress.api.ProgressRole;
import org.sakaiproject.progress.api.ProgressServiceException;
import org.sakaiproject.progress.model.data.entity.*;
import org.sakaiproject.assignment.api.AssignmentService;

import java.util.*;

import lombok.extern.slf4j.Slf4j;
import lombok.Setter;
import org.sakaiproject.user.api.User;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *<p>
 *     Uses the assignment service to calculate progress based on settings
 *</p>
 */

@Slf4j
public class AssignmentProgessImpl extends ProgressImpl implements IProgress {

    @Setter
    private AssignmentService assignmentService;

    @Autowired
    ConfigurationManager configurationManager;

    /**
     * Gives a progress percentage mapped to users within a specific course.
     * @param siteID - Describes the portlet context - generated with ToolManager.getCurrentPlacement().getContext()
     * @param configWideAttributes
     * @return A Map of Progress Percentage that is mapped to user's UUID.
     */
    @Override
    public Map<String, Integer> getProgress(String siteID, ProgressSiteConfiguration progressSiteConfiguration) {
    //public Map<String, Integer> getProgress(String siteID, List<ProgressItem> progressItems, List<ProgressConfigurationAttributeValue> configWideAttributes) {
    	List<ProgressItem> progressItems = progressSiteConfiguration.getProgressItems();

        Map<String, Integer> gradeList = new HashMap<>();

        ProgressRole role = getRole(siteID);

        //Prevents a student from viewing the entire class.
        if (role == ProgressRole.INSTRUCTOR || role == ProgressRole.TA) {

            for(User student : this.getStudents(siteID)){
                ProgressRole userRole = commonMethods.getRole(siteID, student.getId());

                //ensures only students get added to the Map
                if(userRole == ProgressRole.STUDENT){
                    gradeList.put(student.getId(), this.getImplementationCompletionPercentage(progressItems.get(0).getSiteConfig(), siteID, student.getId()).intValue());
                }
            }
        } else {
            final String userId = this.getUser(siteID);

            gradeList.put(userId, this.getImplementationCompletionPercentage(progressItems.get(0).getSiteConfig(), siteID, userId).intValue());
        }

        return gradeList;
    }

    /**
     * Returns a list of the appropriate progressItems.
     * @param siteID
     * @return
     */
    @Override
    public ProgressSiteConfiguration getProgressItems(String siteID, ProgressSiteConfiguration progressSiteConfiguration) {
        Collection<Assignment> assignments = assignmentService.getAssignmentsForContext(siteID);

        ProgressBuilder progressBuilder = new ProgressBuilder();

        //Ensures that all Assignments for the site are listed in the Progress Items.
        for (Assignment a : assignments) {
            boolean found = false;

            for (ProgressItem pi : progressSiteConfiguration.getProgressItems()) {
                if (pi.getName().equals(a.getTitle())) {
                    found = true;
                }
            }

            if (!found) {
                ProgressItem temp = progressBuilder.buildProgressItem(a.getTitle(), progressSiteConfiguration);
                progressSiteConfiguration.getProgressItems().add(temp);
            }
        }
        
        List<ProgressItem> itemsToRemove = new ArrayList<ProgressItem>();

        //Finds Progress Items that need to be removed
        if (progressSiteConfiguration.getProgressItems() != null) {
            for (ProgressItem pi : progressSiteConfiguration.getProgressItems()) {

                boolean found = false;

                for (Assignment a : assignments) {
                    if (pi.getName().equals(a.getTitle())) {
                        found = true;
                    }
                }
                if (!found) {
                    itemsToRemove.add(pi);
                }
            }
        }

        //Removes the Progress Items
        if (itemsToRemove.size() > 0) {
        	progressSiteConfiguration.getProgressItems().removeAll(itemsToRemove);
        }

        return progressSiteConfiguration;
    }

    /**
     * returns the completion percentage for each progress item for a student. This method should check to make sure the user requesting
     * the information is an instructor or a ta. If neither the current user should have the same uid as the uid param.
     * This is meant for situations where an item can be partially complete.
     * In many cases this will just be 100% (An item is either complete or it is not.)
     * @param siteID
     * @param uid
     * @param progressSiteConfiguration
     * @return
     */
    @Override
    public Map<ProgressItem, Integer> getCompletionPercentagesForStudent(String siteID, String uid, ProgressSiteConfiguration progressSiteConfiguration) {
        ProgressRole role = this.getRole(siteID);

        Map<ProgressItem, Integer> percentageMap = new HashMap<>();

        //Prevents a student from gaining access to another student's assignments
        if (role != ProgressRole.INSTRUCTOR && role != ProgressRole.TA) {
            if (!(uid.equals(getUser(siteID)))) {
                throw new ProgressServiceException("Current user does not have access to this student");
            }
        }

        Collection<Assignment> assignments = assignmentService.getAssignmentsForContext(siteID);
        List<ProgressItem> progressItems = progressSiteConfiguration.getProgressItems();

        for (Assignment a : assignments) {

            for (ProgressItem pi : progressItems) {

                //Ensures that only Assignments that are currently stored as progress items are used.
                if (pi.getName().equals(a.getTitle()) && pi.isActive()) {
                    Double weight = Double.parseDouble(pi.getAttributeValueByName("Weight"));

                    boolean found = false;

                    for (AssignmentSubmission submission : a.getSubmissions()) {
                    	//Looks like sakai creates submissions for every assignment even if the student hasn't submitted anything.
                    	//So check to see if the user actually submitted something, or if the instructor gave them a free grade
                    	if (submission.getUserSubmission() || submission.getGraded()) {
                            Set<AssignmentSubmissionSubmitter> submittersInit = submission.getSubmitters();

                            List<AssignmentSubmissionSubmitter> submitters = new ArrayList<>(submittersInit);

                            if (submitters.get(submitters.size() - 1).getSubmitter().equals(uid)) {

                                //if the weight is 0 then don't include it
                                if (Integer.parseInt(pi.getValues().get(0).getValue()) != 0) {
                                	if(progressSiteConfiguration.getAttributeValueByName("Use Grade as Progress Score").equals("true") && submission.getGraded()) {
                                        Double assignmentPercentage = getCalculatedPercentage(submission, submission.getAssignment().getTypeOfGrade().toString(), 100);
                                        percentageMap.put(pi, assignmentPercentage.intValue());
                                	}
                                	else {
                                		percentageMap.put(pi, 100);
                                	}

                                }
                                found = true;
                                break;
                            }
                    	}
                    }

                    //figures for an assignment that is not submitted.
                    if (!found) {
                        percentageMap.put(pi, 0);
                    }
                }
            }

        }

        return percentageMap;
    }

    /**
     * gets the total percentage of the Progress Item in the implementation. Ex. There are 10 Progress items and each progress item is equally weighted,
     * each progress item should be worth 10 percent.
     *
     * @param siteID
     * @param progressSiteConfiguration
     * @return
     */
    @Override
    public Map<ProgressItem, Integer> getProgressItemPercentageOfImplementation(String siteID, ProgressSiteConfiguration progressSiteConfiguration) {
        Map<ProgressItem, Integer> weights = new HashMap<>();

        List<ProgressItem> progressItems = progressSiteConfiguration.getProgressItems();

        boolean weightsNotSet = false;
        int numItems = 0;

        for (ProgressItem pi : progressItems) {
            if (pi.isActive()) {
                int weight = Integer.parseInt(pi.getValues().get(0).getValue());

                if (weight == 0) {
                    weightsNotSet = true;
                } else {
                    weights.put(pi, weight);
                }

                numItems++;
            }
        }

        //Error case of no weight set. This should not happen but could occur if the values are set manually in the db.
        if (weightsNotSet) {
            for (ProgressItem pi : progressItems) {
                weights.put(pi, 100 / numItems);
            }
        }

        return weights;
    }

    /**
     * This method returns the total completion percentage of the implementation for the specified user.
     * @param progressSiteConfiguration
     * @param siteID
     * @param uid
     * @return
     */
    @Override
    public Double getImplementationCompletionPercentage(ProgressSiteConfiguration progressSiteConfiguration, String siteID, String uid) {
        return getProgressForStudent(siteID, progressSiteConfiguration, uid);
    }

    /**
     * Returns the Name of the implementation (used as the identifier in the database.)
     * @return the name of the implementation.
     */
    @Override
    public String getName() {

        return "Assignment";
    }

    /**
     * Gets the list of Attributes that this implementation uses for configurations.
     * @return The list of Attributes.
     */
    @Override
    public List<Attribute> getAttributes() {

        //Specifies the Weight Attribute
        Attribute newAttribute = new Attribute();
        newAttribute.setDefaultValue("0");
        newAttribute.setAttributeType("double");
        newAttribute.setConfigWideAttribute(false);
        newAttribute.setName("Weight");
        
        Attribute useGradeAsProgressScore = new Attribute();
        useGradeAsProgressScore.setDefaultValue("false");
        useGradeAsProgressScore.setAttributeType("boolean");
        useGradeAsProgressScore.setConfigWideAttribute(true);
        useGradeAsProgressScore.setName("Use Grade as Progress Score");
        

        List<Attribute> attributes = new ArrayList<>();
        attributes.add(newAttribute);
        attributes.add(useGradeAsProgressScore);

        return attributes;
    }

    /**
     * Returns the validation result depending on whether the input siteConfig is valid.
     * This should check that each individual attribute has a valid value
     * and also should make sure that all the attribute values make sense together
     * (i.e. all weights add up to 100).
     * @param potentialSiteConfig
     * @return
     */
    @Override
    public ValidationResult isValid(ProgressSiteConfiguration potentialSiteConfig) {
        ValidationResult result = new ValidationResult();
        result.setErrorMessage("");
        List<ProgressItem> progressItems = potentialSiteConfig.getProgressItems();
        double totalWeight = 0;
        for (ProgressItem item : progressItems) {

            //checks for proper values input
            if (item.isActive()) {
                try {
                    double newVal = Double.parseDouble(item.getAttributeValueByName("Weight"));
                    totalWeight += newVal;
                } catch (NullPointerException | NumberFormatException ex) {
                    log.error("Invalid weight value passed in POST body.", ex);
                    result.setErrorMessage(result.getErrorMessage() + "\nAssignment item weights require numeric input only.");
                    result.setValid(false);
                }
            }
        }

        //Checks to make sure weights add up
        if (totalWeight != 100) {
            result.setErrorMessage(result.getErrorMessage() + "\nAll weights in Assignments must total 100.");
            result.setValid(false);
        } else {
            result.setValid(true);
        }

        return result;
    }

    /**
     * Returns the total progress completion percentage for all active assignment progress items.
     * @param siteID
     * @param progressSiteConfiguration
     * @param uid
     * @return double
     */
    private double getProgressForStudent(String siteID, ProgressSiteConfiguration progressSiteConfiguration, String uid) {

        Collection<Assignment> assignments = assignmentService.getAssignmentsForContext(siteID);
        List<ProgressItem> progressItems = progressSiteConfiguration.getProgressItems();

        double progress = 0;

        Map<ProgressItem, Integer> weights = getProgressItemPercentageOfImplementation(siteID, progressSiteConfiguration);

        //Loops through and adds the active assignments up to get the progress.
        for (Assignment a : assignments) {
            for (ProgressItem pi : progressItems) {

                //Only uses active assignment progress items.
                if (pi.getName().equals(a.getTitle()) && pi.isActive()) {
                    for (AssignmentSubmission submission : a.getSubmissions()) {
                    	//Looks like sakai creates submissions for every assignment even if the student hasn't submitted anything.
                    	//So check to see if the user actually submitted something, or if the instructor gave them a free grade
                    	if (submission.getUserSubmission() || submission.getGraded()) {
                            Set<AssignmentSubmissionSubmitter> submittersInit = submission.getSubmitters();

                            List<AssignmentSubmissionSubmitter> submitters = new ArrayList<>(submittersInit);

                            if (submitters.get(submitters.size() - 1).getSubmitter().equals(uid)) {
                            	if(progressSiteConfiguration.getAttributeValueByName("Use Grade as Progress Score").equals("true")) {
                            		if (submission.getGraded()) {
                            			progress += getCalculatedPercentage(submission, submission.getAssignment().getTypeOfGrade().toString(), weights.get(pi));	
                            		}
                            		else {
                            			//submissions only count if they are graded
                            			//this one isn't graded, so it doesn't count
                            		}
                            	}
                            	else {
                            		progress += weights.get(pi);
                            	}
                                break;
                            }
                    	}
                    }
                    break;
                }
            }
        }

        return progress;
    }

    /**
     * Calculates the percentage of completion for an individual assignment
     * @param submission
     * @param gradeType
     * @param weight
     * @return Double
     */
    private Double getCalculatedPercentage(AssignmentSubmission submission, String gradeType, Integer weight){
        Double assignmentPercentage = 0.0;

        try {
		        if(gradeType.equals("SCORE_GRADE_TYPE")){
		
		            if (submission.getGrade() != null) {
		                assignmentPercentage = ((Double.parseDouble(submission.getGrade()) * 100) / submission.getAssignment().getMaxGradePoint()) * (new Double(weight)/100);
		            }
		        }
		        else if(gradeType.equals("UNGRADED_GRADE_TYPE")){
		            if (submission.getSubmitted()) {
		                assignmentPercentage = new Double(100 * (new Double(weight)/100));
		            }
		        }
		        else if(gradeType.equals("LETTER_GRADE_TYPE") || gradeType.equals("PASS_FAIL_GRADE_TYPE") || gradeType.equals("CHECK_GRADE_TYPE")){
		
		            Double grade = 0.0;
		
		            if(submission.getGrade() != null){
		                switch(submission.getGrade()){
		                    case "A+":
		                        grade = 100.0;
		                        break;
		                    case "A":
		                        grade = 95.0;
		                        break;
		                    case "A-":
		                        grade = 90.0;
		                        break;
		                    case "B+":
		                        grade = 89.0;
		                        break;
		                    case "B":
		                        grade = 85.0;
		                        break;
		                    case "B-":
		                        grade = 80.0;
		                        break;
		                    case "C+":
		                        grade = 79.0;
		                        break;
		                    case "C":
		                        grade = 75.0;
		                        break;
		                    case "C-":
		                        grade = 70.0;
		                        break;
		                    case "D+":
		                        grade = 69.0;
		                        break;
		                    case "D":
		                        grade = 65.0;
		                        break;
		                    case "D-":
		                        grade = 60.0;
		                        break;
		                    case "pass":
		                        grade = 100.0;
		                        break;
		                    case "Checked":
		                        grade = 100.0;
		                        break;
		                }
		
		                assignmentPercentage = new Double(grade * (new Double(weight)/100));
		            }
		
		        }
	        }
        catch(NumberFormatException ex) {
        	log.error("Number format exception when parsing grade.", ex);
        }

        return assignmentPercentage;
    }
}
