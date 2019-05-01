package org.sakaiproject.progress.api;

import org.sakaiproject.progress.model.data.entity.ProgressConfigurationAttribute;
import org.sakaiproject.progress.model.data.entity.ProgressConfigurationAttributeValue;
import org.sakaiproject.progress.model.data.entity.ProgressItem;
import org.sakaiproject.progress.model.data.entity.ProgressSiteConfiguration;
import org.sakaiproject.user.api.User;

import java.util.List;
import java.util.Map;

/**
 *<p>
 *     IProgress is the service that handles tracking Progress
 *</p>
 */
public interface IProgress {
	
	/**
	 * This inner Attribute class represents Attributes
	 * which need to be added to the database upon initialization.
	 */
	public class Attribute {
		
		//The data type of the attribute (String, int, bool, etc)
		private String attributeType;
		
		//The name of the attribute
		private String name;
		
		//If the attribute applies to the entire configuration, or just an individual progress item
		private boolean configWideAttribute;
		
		//The default value of the attribute
		private String defaultValue;
		
		public Attribute() {
			//default ctor
		}
		
		public Attribute(String attributeType, String name, boolean configWideAttribute, String defaultValue) {
			this.attributeType = attributeType;
			this.name = name;
			this.configWideAttribute = configWideAttribute;
			this.defaultValue = defaultValue;
		}

		public String getAttributeType() {
			return attributeType;
		}

		public void setAttributeType(String attributeType) {
			this.attributeType = attributeType;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public boolean isConfigWideAttribute() {
			return configWideAttribute;
		}

		public void setConfigWideAttribute(boolean configWideAttribute) {
			this.configWideAttribute = configWideAttribute;
		}

		public String getDefaultValue() {
			return defaultValue;
		}

		public void setDefaultValue(String defaultValue) {
			this.defaultValue = defaultValue;
		}
		
	}
	
	/**
	 *  This class is a simple POJO used to store the results of an isValid() call.
	 *  It contains a boolean representing whether or not it was valid
	 *  and an error message if the call wasn't valid. 
	 */
	public class ValidationResult{
		
		private boolean valid;
		
		private String errorMessage;

		public boolean isValid() {
			return valid;
		}

		public void setValid(boolean valid) {
			this.valid = valid;
		}

		public String getErrorMessage() {
			return errorMessage;
		}

		public void setErrorMessage(String errorMessage) {
			this.errorMessage = errorMessage;
		}
	}

    /**
     * Gets the UUID for the Current User
     * @param siteID - Describes the portlet context - generated with ToolManager.getCurrentPlacement().getContext()
     * @return UUID for the current user
     */
    String getUser(String siteID);

    /**
     * Gets the role of the current user.
     * @param siteID - Describes the portlet context - generated with ToolManager.getCurrentPlacement().getContext()
     * @return User's role Instructor, TA, or Student
     */
    ProgressRole getRole(String siteID);

    /**
     * Gets a list of Student Users for a class.
     * @param siteId - Describes the portlet context - generated with ToolManager.getCurrentPlacement().getContext()
     * @return Returns a List of the entire class if the current user is an Instructor or TA. Return just the current
     *      user if the Current User is a Student.
     */
    List<User> getStudents(String siteId);

    /**
     * Gives a progress percentage mapped to users within a specific course.
     * @param siteID - Describes the portlet context - generated with ToolManager.getCurrentPlacement().getContext()
     * @param configWideAttributes
	 * @return A Map of Progress Percentage that is mapped to user's UUID.
     */
    Map<String, Integer> getProgress(String siteID, ProgressSiteConfiguration progressSiteConfiguration);

    /**
     * Given a ProgressSiteConfiguration with an arbritary set of ProgressItems,
     * returns the same ProgressSiteConfiguration with the correct set of ProgressItems.
     * 
     * In practice this is used to get the original set of ProgressItems, and to update the set
     * whenever new ones need to be created (i.e. instructor adds a new assignment to the course.)
     * @param siteID
     * @return ProgressSiteConfiguration
     */
    ProgressSiteConfiguration getProgressItems(String siteID, ProgressSiteConfiguration progressSiteConfiguration);

	/**
	 * returns the completion percentage for each progress item for a student. This method should check to make sure the user requesting
	 * the information is an instructor or a ta. If neither the current user should have the same uid as the uid param.
	 * This is meant for situations where an item can be partially complete.
	 * In many cases this will just be 100% (An item is either complete or it is not.)
	 * @param siteID
	 * @param uid
	 * @param progressSiteConfiguration
	 * @return MapList<ProgressItem> progressItems, List<ProgressConfigurationAttributeValue> configWideAttributes
	 */
    Map<ProgressItem, Integer> getCompletionPercentagesForStudent (String siteID, String uid, ProgressSiteConfiguration progressSiteConfiguration);

	/**
	 * gets the total percentage of the Progress Item in the implementation. Ex. There are 10 Progress items and each progress item is equally weighted,
	 * each progress item should be worth 10 percent.
	 *
	 * @param siteID
	 * @param progressSiteConfiguration
	 * @return Map
	 */
    Map<ProgressItem, Integer> getProgressItemPercentageOfImplementation(String siteID, ProgressSiteConfiguration progressSiteConfiguration);

	/**
	 * This method returns the total completion percentage of the implementation for the specified user.
	 * @param progressSiteConfiguration
	 * @param siteID
	 * @param uid
	 * @return Double
	 */
    Double getImplementationCompletionPercentage(ProgressSiteConfiguration progressSiteConfiguration, String siteID, String uid);

	/**
	 * Returns the Name of the implementation (used as the identifier in the database.)
	 * @return the name of the implementation.
	 */

    String getName(); 
    
    /**
     * Gets the list of Attributes that this implementation uses for configurations.
     * @return The list of Attributes. 
     */
    List<Attribute> getAttributes();
    
    /**
     * Returns the validation result depending on whether the input siteConfig is valid.
     * This should check that each individual attribute has a valid value
     * and also should make sure that all the attribute values make sense together
     * (i.e. all weights add up to 100). 
     * @param potentialSiteConfig
     * @return
     */
    ValidationResult isValid(ProgressSiteConfiguration potentialSiteConfig);
    
}
