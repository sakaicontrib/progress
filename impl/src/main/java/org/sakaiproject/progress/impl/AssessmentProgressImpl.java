package org.sakaiproject.progress.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.sakaiproject.progress.api.IProgress;
import org.sakaiproject.progress.api.ProgressRole;
import org.sakaiproject.progress.api.ProgressServiceException;
import org.sakaiproject.progress.model.data.entity.ProgressConfigurationAttribute;
import org.sakaiproject.progress.model.data.entity.ProgressConfigurationAttributeValue;
import org.sakaiproject.progress.model.data.entity.ProgressItem;
import org.sakaiproject.progress.model.data.entity.ProgressSiteConfiguration;
import org.sakaiproject.tool.assessment.data.dao.grading.AssessmentGradingData;
import org.sakaiproject.tool.assessment.facade.PublishedAssessmentFacade;
import org.sakaiproject.tool.assessment.services.assessment.PublishedAssessmentService;
import org.sakaiproject.user.api.User;
import org.springframework.beans.factory.annotation.Autowired;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AssessmentProgressImpl extends ProgressImpl implements IProgress {

	private PublishedAssessmentService publishedAssessmentService;

	@Autowired
	private ProgressBuilder progressBuilder;

	@Autowired
	ConfigurationManager configurationManager;

	public AssessmentProgressImpl() {
		// Samigo doesn't use Spring Beans so we have to do this the non-spring way
		publishedAssessmentService = new PublishedAssessmentService();
	}

	@Override
	public String getName() {
		return "Assessments";
	}

    /**
     * Gets the list of Attributes that this implementation uses for configurations.
     * @return The list of Attributes. 
     */
	@Override
	public List<Attribute> getAttributes() {
		List<Attribute> attributes = new ArrayList<Attribute>();
		attributes.add(new Attribute("double", "Weight", false, "0.0"));
		// This attribute determines if a submission is ok to count as complete for
		// progress calculations
		// or if it needs to be graded before it counts as complete
		
		//after testing, the comments regarding status in AssessmentGradingData do not appear to be accurate
		//seems like all assessments appear as status 1 == submitted even if they have been graded
		//so this attribute is pointless because we cannot differentiate between graded and ungraded submissions....
		//so I am commenting it out for now. Maybe a future developer can determine a way to make this differentiation. 
		
		//attributes.add(new Attribute("boolean", "Only Consider Graded Submissions", true, "true"));

		return attributes;

	}

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
		
		Map<String, Integer> progressMap = new HashMap<String, Integer>();

		ProgressRole role = getRole(siteID);
		if (role == ProgressRole.INSTRUCTOR || role == ProgressRole.TA) {
			List<User> courseStudents = getStudents(siteID);
			for (User student : courseStudents) {
				progressMap.put(student.getId(), getProgressForStudent(siteID, student.getId(), progressSiteConfiguration));
			}
		} else {
			String currentUserId = getUser(siteID);
			progressMap.put(currentUserId, getProgressForStudent(siteID, currentUserId, progressSiteConfiguration));
		}

		return progressMap;
	}

	/**
	 * Gets the progress score for an individual student.
	 * 
	 * @param siteID Describes the portlet context - generated with ToolManager.getCurrentPlacement().getContext()
	 * @param userID The ID of student.
	 * @param progressItems The list of progress items for the this progress type. 
	 * @param configWideAttributes The config wide attributes applied to this configuration. 
	 * @return The overall completion score (a number out of 100).
	 */
	private Integer getProgressForStudent(String siteID, String userID, ProgressSiteConfiguration progressSiteConfiguration) {
		
		double progressScore = 0.0;
		
		Map<ProgressItem, Integer> completionPercentages = getCompletionPercentagesForStudent(siteID, userID, progressSiteConfiguration);
		
		for (Map.Entry<ProgressItem, Integer> completionPercentage : completionPercentages.entrySet()) {
			//items can only be 100% or 0% complete
			if(completionPercentage.getValue() == 100) {
				double weight = Double.parseDouble(completionPercentage.getKey().getAttributeValueByName("Weight"));
				progressScore = progressScore + weight;
			}
		}
		
		return (int) Math.round(progressScore);
	}

    /**
     * Given a ProgressSiteConfiguration with an arbritary set of ProgressItems,
     * returns the same ProgressSiteConfiguration with the correct set of ProgressItems.
     * 
     * In practice this is used to get the original set of ProgressItems, and to update the set
     * whenever new ones need to be created (i.e. instructor adds a new assignment to the course.)
     * @param siteID
     * @return
     */
	@Override
	public ProgressSiteConfiguration getProgressItems(String siteID,
			ProgressSiteConfiguration progressSiteConfiguration) {
		List<PublishedAssessmentFacade> publishedAssessments = publishedAssessmentService
				.getBasicInfoOfAllPublishedAssessments2("startDate", true, siteID);

		//Add new Progress Items that aren't already present
		for (PublishedAssessmentFacade assessment : publishedAssessments) {
			boolean found = false;
			for (ProgressItem item : progressSiteConfiguration.getProgressItems()) {
				if (item.getName().equals(assessment.getTitle())) {
					found = true;
				}
			}
			if (!found) {
				progressSiteConfiguration.getProgressItems()
						.add(progressBuilder.buildProgressItem(assessment.getTitle(), progressSiteConfiguration));
			}
		}
		
		List<ProgressItem> itemsToRemove = new ArrayList<ProgressItem>();
		
		//Remove extra Progress Items which shouldn't be there
		for (ProgressItem item : progressSiteConfiguration.getProgressItems()) {
			boolean found = false;
			for (PublishedAssessmentFacade assessment : publishedAssessments) {
				if (item.getName().equals(assessment.getTitle())) {
					found = true;
				}
			}
			if (!found) {
                itemsToRemove.add(item);
			}
		}
		
        if (itemsToRemove.size() > 0) {
        	progressSiteConfiguration.getProgressItems().removeAll(itemsToRemove);
        }

		return progressSiteConfiguration;
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
			if (item.isActive()) {
				try {
					double newVal = Double.parseDouble(item.getAttributeValueByName("Weight"));
					totalWeight += newVal;
				}
				catch(NullPointerException | NumberFormatException ex) {
					log.error("Invalid weight value passed in POST body.", ex);
					result.setErrorMessage(result.getErrorMessage() + "\nAssessment item weights require numeric input only.");
					result.setValid(false);
				}
			}
		}
		if (totalWeight != 100) {
			result.setErrorMessage(result.getErrorMessage() + "\nAll weights in Assessments must total 100.");
			result.setValid(false);
		}
		else {
			result.setValid(true);
		}
		
		return result;
	}
	
	/**
     * returns the completion percentage for each progress item for a student. This method should check to make sure the user requesting
     * the information is an instructor or a ta. If neither the current user should have the same uid as the uid param.
     * This is meant for situations where an item can be partially complete.
     * In many cases this will just be 100% (An item is either complete or it is not.)
     * 
     * For assessments, items are either complete or not complete. 
     * @param siteID
     * @param uid
     * @param progressSiteConfiguration
     * @return
     */
	@Override
	public Map<ProgressItem, Integer> getCompletionPercentagesForStudent(String siteID, String uid, ProgressSiteConfiguration progressSiteConfiguration) {
		//An assessment is either complete or not complete. So it's either 100% or 0%.
		List<AssessmentGradingData> gradedAssessments = publishedAssessmentService
				.getBasicInfoOfLastOrHighestOrAverageSubmittedAssessmentsByScoringOption(uid, siteID, false);
		Map<ProgressItem, Integer> completionPercentages = new HashMap<ProgressItem, Integer>();
		for (ProgressItem item : progressSiteConfiguration.getProgressItems()) {
			if (item.isActive()) {
				boolean itemIsGraded = false;
				for (AssessmentGradingData gradedAssessment : gradedAssessments) {
					if (item.getName().equals(gradedAssessment.getPublishedAssessmentTitle())) {
						
						//COMMENTING OUT the chunk of code about assessment statuses because
						// it seems like the assessment statuses don't actually mean anything
						// but hopefully a future developer can fix this. (see comment on line 55)
						
						/*
						if (progressSiteConfiguration.getAttributeValueByName("Only Consider Graded Submissions").equalsIgnoreCase("True")){
							if (gradedAssessment.getStatus() == AssessmentGradingData.AUTO_GRADED
									|| gradedAssessment.getStatus() == AssessmentGradingData.NEED_HUMAN_ATTENTION) {
								itemIsGraded = true;
								completionPercentages.put(item, 100);
							}
						}
						else {
							if (gradedAssessment.getStatus() == AssessmentGradingData.AUTO_GRADED
									|| gradedAssessment.getStatus() == AssessmentGradingData.NEED_HUMAN_ATTENTION
									|| gradedAssessment.getStatus() == AssessmentGradingData.SUBMITTED) {
									*/
								itemIsGraded = true;
								completionPercentages.put(item, 100);							double weight = 0.0;
								weight = Double.parseDouble(progressItem.getAttributeValueByName("Weight"));
								if (weight == 0.0) {
									throw new ProgressServiceException("Progress Item " + progressItem.getName()
											+ " weight must be defined and cannot be 0.");
								}
								progressScore = progressScore + weight;
							//}						
						//}
					}
				}
				if (!itemIsGraded) {
					completionPercentages.put(item, 0);
				}
			}
		}
		return completionPercentages;
	}

    /**
     * gets the total percentage of the Progress Item in the implementation. Ex. There are 10 Progress items and each progress item is equally weighted,
     * each progress item should be worth 10 percent.
     * 
     * For Assessments, each item has a weight, so this method returns the weight of the items. 
     *
     * @param siteID
     * @param progressSiteConfiguration
     * @return
     */
    @Override
	public Map<ProgressItem, Integer> getProgressItemPercentageOfImplementation(String siteID, ProgressSiteConfiguration progressSiteConfiguration) {
		Map<ProgressItem, Integer> progressItemWeights = new HashMap<ProgressItem, Integer>();
		for (ProgressItem item : progressSiteConfiguration.getProgressItems()) {
			String newWeight = item.getAttributeValueByName("Weight");
			if (newWeight != null) {
				progressItemWeights.put(item, (int) Double.parseDouble(newWeight));
			}
		}
		
		return progressItemWeights;
	}

    /**
     * This method returns the total completion percentage of the im							double weight = 0.0;
							weight = Double.parseDouble(progressItem.getAttributeValueByName("Weight"));
							if (weight == 0.0) {
								throw new ProgressServiceException("Progress Item " + progressItem.getName()
										+ " weight must be defined and cannot be 0.");
							}
							progressScore = progressScore + weight;plementation for the specified user.
     * @param progressSiteConfiguration
     * @param siteID
     * @param uid
     * @return
     */
    @Override
	public Double getImplementationCompletionPercentage(ProgressSiteConfiguration progressSiteConfiguration, String siteID, String uid) {
		return (double) getProgressForStudent(siteID, uid, progressSiteConfiguration);

	}
}
