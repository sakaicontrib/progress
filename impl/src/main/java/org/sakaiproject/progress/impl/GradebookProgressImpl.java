package org.sakaiproject.progress.impl;

import lombok.extern.slf4j.Slf4j;

import org.sakaiproject.progress.api.IProgress;
import org.sakaiproject.progress.api.ProgressRole;
import org.sakaiproject.progress.api.ProgressServiceException;
import org.sakaiproject.progress.api.IProgress.Attribute;
import org.sakaiproject.progress.model.data.entity.*;
import org.sakaiproject.service.gradebook.shared.*;
import org.sakaiproject.tool.gradebook.GradeMapping;
import org.sakaiproject.tool.gradebook.Gradebook;
import org.sakaiproject.user.api.User;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.Serializable;
import java.util.*;

/**
 * Base Implementation of IProgress using the Gradebook
 */
@Slf4j
public class GradebookProgressImpl extends ProgressImpl implements IProgress, Serializable {

    @Autowired ConfigurationManager configurationManager;

    /**
     * Gives a progress percentage mapped to users within a specific course.
     * @param siteID - Describes the portlet context - generated with ToolManager.getCurrentPlacement().getContext()
     * @param configWideAttributes
     * @return A Map of Progress Percentage that is mapped to user's UUID.
     */
    @Override
    public Map<String, Integer> getProgress(String siteID, ProgressSiteConfiguration progressSiteConfiguration) {
    	List<ProgressItem> progressItems = progressSiteConfiguration.getProgressItems();
    	List<ProgressConfigurationAttributeValue> configWideAttributes = progressSiteConfiguration.getConfigValues();
    	
        Map<String, Integer> gradeList = new HashMap<>();

        boolean hasActiveProgressItems = false;

        boolean courseGradeUsed = false;
        boolean useGrade = false;

        //Checks to see if Course Grade is used as the measurement
        if(progressItems != null){
        	for(ProgressConfigurationAttributeValue attributeValue : configWideAttributes) {
        		if (attributeValue.getAttribute().getName().equalsIgnoreCase("Use Course Grade") && attributeValue.getValue().equalsIgnoreCase("True")) {
        			courseGradeUsed = true;
                }
        		if (attributeValue.getAttribute().getName().equalsIgnoreCase("Use Grade as Progress Score") && attributeValue.getValue().equalsIgnoreCase("True")) {
        			useGrade = true;
        		}
            }
        }

        if(progressItems != null && !courseGradeUsed){

            ProgressRole role = getRole(siteID);

            for(ProgressItem progressItem : progressItems){
                if(progressItem.isActive()){
                    hasActiveProgressItems = true;

                    boolean gradebookWeight = false;
                    int weight = 0;

                    //Checks for the weighting of the Progress Item
                    for(ProgressAttributeValue attribute : progressItem.getValues()){
                        String attributeName = attribute.getAttributeName();

                        if(attributeName.equalsIgnoreCase("Weight")){
                            weight = Integer.parseInt(attribute.getValue());
                        }
                        else if(attributeName.equals("Use Gradebook Weight")){
                            gradebookWeight = Boolean.getBoolean(attribute.getValue());
                        }
                    }

                    //Prevents a stuedent from viewing another students information
                    if(role == ProgressRole.INSTRUCTOR || role == ProgressRole.TA){
                        for(User student : getStudents(siteID)){
                            gradeList = calculateProgessOnGradebookItems(siteID, progressItem, student, gradeList, gradebookWeight, weight, useGrade);
                        }
                    }
                    else{
                        gradeList = calculateProgessOnGradebookItems(siteID, progressItem, commonMethods.getCurrentUser(siteID), gradeList, gradebookWeight, weight, useGrade);
                    }
                }

            }
        }

        //Ensures that the default use of the gradebook is used
        if(!hasActiveProgressItems || courseGradeUsed){
            gradeList = calculateCourseGrades(siteID);
        }

        return gradeList;
    }

    /**
     * Given a ProgressSiteConfiguration with an arbritary set of ProgressItems,
     * returns the same ProgressSiteConfiguration with the correct set of ProgressItems.
     * 
     * In practice this is used to get the original set of ProgressItems, and to update the set
     * whenever new ones need to be created (i.e. instructor adds a new assignment to the course.)
     * @param siteID
     * @return ProgressSiteConfiguration
     */
    @Override
    public ProgressSiteConfiguration getProgressItems(String siteID, ProgressSiteConfiguration progressSiteConfiguration) {

        Gradebook gradebook = this.getGradebook(siteID);

        List<Assignment> assignments = new ArrayList<>();

        ProgressRole role = getRole(siteID);

        //Ensures only an instructor or TA calls the getAssignments method. Will throw an error if a student calls it.
        if(role == ProgressRole.INSTRUCTOR || role == ProgressRole.TA) {
            assignments = gradebookService.getAssignments(getGradebook(siteID).getUid());

            ProgressBuilder progressBuilder = new ProgressBuilder();

            //checks that all assignments in the gradebook are added to the progress tables
            for(Assignment assignment : assignments){
                boolean found = false;

                for(ProgressItem pi : progressSiteConfiguration.getProgressItems()){
                    if(pi.getName().equals(assignment.getName())){
                        found = true;
                    }
                }

                if(!found){
                    ProgressItem temp = progressBuilder.buildProgressItem(assignment.getName(), progressSiteConfiguration);
                    progressSiteConfiguration.getProgressItems().add(temp);
                }
            }

            List<ProgressItem> itemsToRemove = new ArrayList<ProgressItem>();

            //Finds ProgressItems that should be removed from the database
            if(progressSiteConfiguration.getProgressItems() != null){
                for(ProgressItem pi : progressSiteConfiguration.getProgressItems()){
                    boolean found = false;

                    for(Assignment assignment : assignments){
                        if(pi.getName().equals(assignment.getName())){
                            found = true;
                        }
                    }

                    if(!found){
                        itemsToRemove.add(pi);
                    }
                }
            }

            //Removes unnecessary ProgressItems from the database
            if (itemsToRemove.size() > 0) {
                progressSiteConfiguration.getProgressItems().removeAll(itemsToRemove);
            }
        }

        return progressSiteConfiguration;
    }

    /**
     * returns the completion percentage for each progress item for a studentestert. This method should check to make sure the user requesting
     * the information is an instructor or a ta. If neither the current user should have the same uid as the uid param.
     * This is meant for situations where an item can be partially complete.
     * In many cases this will just be 100% (An item is either complete or it is not.)
     * @param siteID
     * @param uid
     * @param progressSiteConfiguration
     * @return Map
     */
    @Override
    public Map<ProgressItem, Integer> getCompletionPercentagesForStudent(String siteID, String uid, ProgressSiteConfiguration progressSiteConfiguration) {
        ProgressRole role = this.getRole(siteID);
        List<ProgressItem> progressItems = progressSiteConfiguration.getProgressItems();

        boolean courseGradeUsed = false;

        Map<ProgressItem, Integer> percentageMap = new HashMap<>();

        //Prevents students from viewing other student's grades
        if(role != ProgressRole.INSTRUCTOR && role != ProgressRole.TA){
            if(!(uid.equals(getUser(siteID)))){
                throw new ProgressServiceException("Current user does not have access to this student");
            }
        }

        //Checks for course grade usage
        if(progressItems != null){
        	for(ProgressConfigurationAttributeValue attributeValue: progressSiteConfiguration.getConfigValues()) {
        		if (attributeValue.getAttribute().getName().equalsIgnoreCase("Use Course Grade") && attributeValue.getValue().equalsIgnoreCase("True")) {
        			courseGradeUsed = true;
                    break;
        		}
        	}
        }

        if(progressItems != null && !courseGradeUsed){

            for(ProgressItem progressItem : progressItems){
                if(progressItem.isActive()){

                    Gradebook gradebook = getGradebook(siteID);

                    List<Assignment> assignments = gradebookService.getViewableAssignmentsForCurrentUser(gradebook.getUid());


                    List<String> users = new ArrayList<>();
                    users.add(uid);

                    for(Assignment assignment : assignments){

                        List<GradeDefinition> gradeDefinitions = new ArrayList<>();

                        if(progressItem.getName().equals(assignment.getName())){
                            //Will error if the user is a student
                            try{
                                gradeDefinitions = gradebookService.getGradesForStudentsForItem(gradebook.getUid(), assignment.getId(), users);
                            }
                            //Runs if the user is a student
                            catch(GradebookSecurityException e){
                                gradeDefinitions.add(gradebookService.getGradeDefinitionForStudentForItem(gradebook.getUid(), assignment.getId(), uid));
                            }

                            if(gradeDefinitions.size() > 0){
                            	if (progressSiteConfiguration.getAttributeValueByName("Use Grade as Progress Score").equalsIgnoreCase("true")) {
                                    for(GradeDefinition gradeDefinition : gradeDefinitions){
                                        if(gradeDefinition.getStudentUid().equals(uid)){
                                            String grade = "0";

                                            if(gradeDefinition.getGrade() != null){
                                                grade = gradeDefinition.getGrade();
                                            }

                                            int gradeType = gradebook.getGrade_type();

                                            if(gradeType == GradingType.PERCENTAGE.getValue()){
                                                percentageMap.put(progressItem, (int) Double.parseDouble(grade));
                                            }
                                            else if(gradeType == GradingType.POINTS.getValue()){
                                                percentageMap.put(progressItem, (int) Double.parseDouble(grade));
                                            }
                                            else if(gradeType == GradingType.LETTER.getValue()){

                                                /**
                                                 * Check for grade = 0 and sub with F
                                                 */
                                                GradeMapping gradeMapping = gradebook.getSelectedGradeMapping();

                                                percentageMap.put(progressItem, gradeMapping.getValue(grade).intValue());
                                            }
                                        }
                                    }
                                }
                            	else {
                            		//there are grade definitions so we assume it's graded, therefore it counts for progress
                            		percentageMap.put(progressItem, 100);
                            	}
                            }
                        }
                    }
                    //Sets the ProgressItem completion percentage for 0 in the event the User has not compeleted the item
                    if(!percentageMap.containsKey(progressItem)){
                        percentageMap.put(progressItem, 0);
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
     * @return Map
     */
    @Override
    public Map<ProgressItem, Integer> getProgressItemPercentageOfImplementation(String siteID, ProgressSiteConfiguration progressSiteConfiguration) {
            Map<ProgressItem, Integer> weights = new HashMap<>();

            List<ProgressItem> progressItems = progressSiteConfiguration.getProgressItems();


            boolean gradebookWeight = false;
            int weight = 0;

            boolean weightsNotSet = false;
            int numItems = 0;

            for(ProgressItem progressItem : progressItems){
                if(progressItem.isActive()){
                    for(ProgressAttributeValue attribute : progressItem.getValues()){
                        String attributeName = attribute.getAttributeName();

                        if(attributeName.equalsIgnoreCase("Weight")){
                            weight = Integer.parseInt(attribute.getValue());
                        }
                        else if(attributeName.equals("Use Gradebook Weight")){
                            gradebookWeight = Boolean.getBoolean(attribute.getValue());
                        }
                    }

                    Gradebook gradebook = getGradebook(siteID);

                    List<Assignment> assignments = gradebookService.getViewableAssignmentsForCurrentUser(gradebook.getUid());

                    for(Assignment assignment : assignments) {
                        if(assignment.getName().equalsIgnoreCase(progressItem.getName())){
                            int assignmentWeight = 1;

                            if (gradebookWeight && (assignment.getWeight() != null || assignment.getWeight() != 0)) {
                                assignmentWeight = assignment.getWeight().intValue();
                            } else if (weight != 0) {
                                assignmentWeight = weight;
                            }
                            else{
                                weightsNotSet = true;

                            }

                            weights.put(progressItem, assignmentWeight);
                            numItems++;
                            break;
                        }

                    }
                }
            }


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
     * @return Double
     */
    @Override
    public Double getImplementationCompletionPercentage(ProgressSiteConfiguration progressSiteConfiguration, String siteID, String uid) {

        Map<String, Integer> gradeList = new HashMap<>();

        List<ProgressItem> progressItems = progressSiteConfiguration.getProgressItems();

        if (progressItems != null) {
	        for(ProgressItem progressItem : progressItems){
	            if(progressItem.isActive()){
	
	                boolean gradebookWeight = false;
	                int weight = 0;
	
	                for(ProgressAttributeValue attribute : progressItem.getValues()){
	                    String attributeName = attribute.getAttributeName();
	
	                    if(attributeName.equalsIgnoreCase("Weight")){
	                        weight = Integer.parseInt(attribute.getValue());
	                    }
	                    else if(attributeName.equals("Use Gradebook Weight")){
	                        gradebookWeight = Boolean.getBoolean(attribute.getValue());
	                    }
	                }
	                for(User student : getStudents(siteID)){
	                    if(student.getId().equalsIgnoreCase(uid)){
	                        boolean useGrade = progressSiteConfiguration.getAttributeValueByName("Use Grade as Progress Score").equals("true");
	                    	gradeList = calculateProgessOnGradebookItems(siteID, progressItem, student, gradeList, gradebookWeight, weight, useGrade);
	                    }
	                }
	            }
	        }
        }
        if (gradeList.size() < 1 || gradeList.get(uid) == null) {
        	return 0.0;
        }
        else {
        	return gradeList.get(uid).doubleValue();
        }
    }

    /**
     * Returns the Name of the implementation (used as the identifier in the database.)
     * @return the name of the implementation.
     */
    @Override
	public String getName() {
		return "Gradebook";
	}

    /**
     * Gets the list of Attributes that this implementation uses for configurations.
     * @return The list of Attributes.
     */
	@Override
	public List<Attribute> getAttributes() {
        Attribute useGradebookWeight = new Attribute();
        useGradebookWeight.setDefaultValue("false");
        useGradebookWeight.setAttributeType("boolean");
        useGradebookWeight.setConfigWideAttribute(false);
        useGradebookWeight.setName("Use Gradebook Weight");

        Attribute weight = new Attribute();
        weight.setDefaultValue("0");
        weight.setAttributeType("double");
        weight.setConfigWideAttribute(false);
        weight.setName("Weight");

        Attribute courseGrade = new Attribute();
        courseGrade.setDefaultValue("false");
        courseGrade.setAttributeType("boolean");
        courseGrade.setConfigWideAttribute(true);
        courseGrade.setName("Use Course Grade");
        
        Attribute useGradeAsProgressScore = new Attribute();
        useGradeAsProgressScore.setDefaultValue("false");
        useGradeAsProgressScore.setAttributeType("boolean");
        useGradeAsProgressScore.setConfigWideAttribute(true);
        useGradeAsProgressScore.setName("Use Grade as Progress Score");

        List<Attribute> attributes = new ArrayList<>();
        attributes.add(useGradebookWeight);
        attributes.add(weight);
        attributes.add(courseGrade);
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

        double weight = 0;
        boolean foundCourseGrade = false;

        for(ProgressConfigurationAttribute attribute : progressItems.get(0).getSiteConfig().getConfigType().getAttributes()){
            if(attribute.isConfigWideAttribute() && attribute.getConfigType().getType().equalsIgnoreCase("Gradebook") && attribute.getName().equalsIgnoreCase("Use Course Grade")){
                if(!(Boolean.parseBoolean(attribute.getConfigAttributeValues().get(0).getValue()))){
                    for(ProgressItem progressItem : progressItems){
                        if(progressItem.isActive()){
                            if((Boolean.parseBoolean(progressItem.getAttributeValueByName("Use GradeBook Weight")))){
                               result.setValid(true);
                               return result;
                            }
                            else{
                                try {
                                    weight += Double.parseDouble(progressItem.getAttributeValueByName("Weight"));
                                }catch (NumberFormatException e){
                                    result.setValid(false);
                                    result.setErrorMessage(result.getErrorMessage() + "\nGradebook Item weights require numeric input only.");
                                    return result;
                                }
                            }

                        }
                    }
                    foundCourseGrade = true;
                }
            }
        }

        if(foundCourseGrade && weight != 100){
            result.setValid(false);
            result.setErrorMessage(result.getErrorMessage() + "\nGradebook item weights must add to 100.");
        }
        else{
            result.setValid(true);
        }

		return result;
	}

    /**
     * Gets a map of userid's to their course grade for a site
     * @param siteID
     * @return Map of UserId's to Course Grades
     */
    private Map<String, Integer> calculateCourseGrades(String siteID){
        Map<String, Integer> gradeList = new HashMap<>();

        final Gradebook gradebook = this.getGradebook(siteID);
        ProgressRole role = getRole(siteID);

        //Prevents a student from view the entire class.
        if(role == ProgressRole.INSTRUCTOR || role == ProgressRole.TA){
            List<String> userIds = getStudentsUIDs(siteID);

            Map<String, CourseGrade> courseGrades = this.gradebookService.getCourseGradeForStudents(gradebook.getUid(), userIds);

            for (Map.Entry<String, CourseGrade> entry : courseGrades.entrySet()){

                double tempGrade;

                try {
                    //Prevents errors caused by divisions by 0.
                    if (entry.getValue() == null || entry.getValue().getTotalPointsPossible() == 0) {
                        tempGrade = 0;
                    } else {
                        tempGrade = Double.parseDouble(entry.getValue().getCalculatedGrade());
                    }
                }catch(NullPointerException e){
                    tempGrade = 0;
                }

                gradeList.put(entry.getKey(), (int)tempGrade);
            }
        }
        else{
            final String userId = this.getUser(siteID);

            final CourseGrade courseGrade = this.gradebookService.getCourseGradeForStudent(gradebook.getUid(), userId);

            // handle the special case in the gradebook service where totalPointsPossible = -1
            if (courseGrade != null && (courseGrade.getTotalPointsPossible() == null || courseGrade.getTotalPointsPossible() == -1)) {
                courseGrade.setTotalPointsPossible(null);
                courseGrade.setPointsEarned(null);
            }

            double result;

            if(courseGrade != null && courseGrade.getCalculatedGrade() != null){
                try{
                    result = Double.parseDouble(courseGrade.getCalculatedGrade());
                }
                catch (NumberFormatException e){
                    result = 0;
                }

            }
            else{
                result = 0;
            }

            gradeList.put(userId, (int)result);
        }

        return gradeList;
    }

    /**
     * Calculates progress for items in the Gradebook.
     * @param siteID
     * @param progressItem
     * @param user
     * @param gradeList
     * @param gradebookWeighted
     * @param weight
     * @ Map
     */
    private Map<String, Integer> calculateProgessOnGradebookItems(String siteID, ProgressItem progressItem, User user, Map<String, Integer> gradeList, boolean gradebookWeighted, int weight, boolean useGrade){
        Gradebook gradebook = getGradebook(siteID);

        List<Assignment> assignments = gradebookService.getViewableAssignmentsForCurrentUser(gradebook.getUid());


        List<String> users = new ArrayList<>();
        users.add(user.getId());

        if (gradeList.get(user.getId()) == null) {
            gradeList.put(user.getId(), 0);
        }

        Assignment assignment = null;
        for (Assignment checkAssignment : assignments) {
            if (checkAssignment.getName().equalsIgnoreCase(progressItem.getName())) {
                assignment = checkAssignment;
            }
        }

        if (assignment != null) {
            //Double points = assignment.getPoints();
            int assignmentWeight = 100;

            if(gradebookWeighted && (assignment.getWeight() != null || assignment.getWeight() != 0)){
                assignmentWeight = assignment.getWeight().intValue();
            }
            else if(weight != 0){
                assignmentWeight = weight;
            }

            List<GradeDefinition> gradeDefinitions = new ArrayList<>();
            try{
                gradeDefinitions = gradebookService.getGradesForStudentsForItem(gradebook.getUid(), assignment.getId(), users);
            }
            catch(GradebookSecurityException e){
                gradeDefinitions.add(gradebookService.getGradeDefinitionForStudentForItem(gradebook.getUid(), assignment.getId(), user.getId()));
            }

            if(gradeDefinitions.size() > 0){
            	
            	if(useGrade) {
	                for(GradeDefinition gradeDefinition : gradeDefinitions){
	                    String grade = "0";
	                    if(gradeDefinition.getGrade() != null){
	                        grade = gradeDefinition.getGrade();
	                    }
	
	                    int gradeType = gradebook.getGrade_type();
	
	                    if(gradeType == GradingType.PERCENTAGE.getValue()){
	                        gradeList.put(user.getId(), gradeList.get(user.getId()) + (int) (Double.parseDouble(grade) * assignmentWeight/100.0));
	                    }
	                    else if(gradeType == GradingType.POINTS.getValue()){
	                        gradeList.put(user.getId(), gradeList.get(user.getId()) + (int) (Double.parseDouble(grade) * assignmentWeight/100.0));
	                    }
	                    else if(gradeType == GradingType.LETTER.getValue()){
	                        GradeMapping gradeMapping = gradebook.getSelectedGradeMapping();
	
	                        gradeList.put(user.getId(), gradeList.get(user.getId()) + (int) (gradeMapping.getValue(grade).intValue() * assignmentWeight/100.0));
	                    }
	                }
            	}
            	else {
            		//there are grade definitions so we assume it's graded, therefore it counts for progress
            		gradeList.put(user.getId(), gradeList.get(user.getId()) + (assignmentWeight));
            	}
            }

            return gradeList;
        }
        else {
            throw new ProgressServiceException("Unable to find assignment with name matching the progress item name. " + progressItem.getName());
        }
    }
}
