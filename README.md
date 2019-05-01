# Sakai Progress Tool
This tool allows instructors and students to measure how far they are along in a course.
There are different ways to measure progress - via Assignments, Tests and Quizzes, Forum Posts, etc. 
The project was designed with extensibility in mind so it is very easy to create new progress measurement types. 

## How to add new progress measurements
* The IProgress interface
  * Create a class that implements the IProgress interface. This class will define the business logic of how progress shall be measured for your measurement type.
  * Declare your new class as a bean in impl/src/main/webapp/WEB-INF/components.xml
  * You can also extend the abstract class ProgressImpl to get access to some common methods (reccomended!). 
  * Using Spring Dependency Injection, all classes that implement IProgress will be created and used at runtime.
  * The tool will call your methods and pass the appropriate data to the frontend and update the database with your data, so all you have to do is write the implementation of IProgress.
* Key Terms
  * Progress - the student's progress in the course, i.e. Alice is 75% complete.
  * Implementation - An implementation of IProgress. A progress measurement type, such as Assignments or Tests and Quizzes.
  * Configuration - The configuration of the implementation as a whole. The database representation of the implementation. 
  * ProgressItem - An individual unit of progress. I.e. a single assignment, or a single test. 
  * Attribute - A configuration attribute tied to an individual ProgressItem, or the implementation as a whole. For example, all ProgressItems must have a "Weight" attribute which defines their overall weight in the progress measurement.
* Important Methods
  * getProgress() - This method should return a Map<String, Integer> of each student's UUID mapped to their current progress score. 
  * getProgressItems() - This method should return a ProgressSiteConfiguration object with all of the ProgressItems added. We have builder methods in ProgressBuilder to help you create ProgressItems.
    * Active progress items are stored in the database, but there might be new progress items or inactive progress items that need to be passed to the front end.
    * So we will call this method and pass in an object ProgressSiteConfiguration which contains all the currently active progress items, and we rely on you to fetch the rest of the them and attach them to this object. 
    * You can use ProgressBuilder.buildProgressItem() to create new progress items, then call progressSiteConfiguration.getProgressItems().add(newItem) to add the new items.
  * getName() - This method just returns the name of the measurement type, so that this configuration can be stored in the database.
  * getAttributes() - This method should return a List<Attribute> containing all the attributes that are considered when you measure progress. Attribute is a simple POJO containing four variables:
    * defaultValue - the value that this attribute should default to.
    * attributeType - the data type of the attribute value. I.e. String, int, double.
    * configWideAttribute - boolean. True if the attribute applies to the entire configuration, false if the attribute applies to an individual progress item.
    * name - This string represents the actual name of the attribute, and is what will be displayed on the front end.
  * getCompletionPercentagesForStudent()
    * For some implementations, a progress item can be considered partially complete.
    * In those situations, this should return a map containing each progress item, mapped to its completion percentage.
    * If progress items are boolean (complete or not) then this should just return 100s or 0s. 
    * This is used to display data correctly to the student / instructor on the progress page.
  * getProgressItemPercentageOfImplementation()
    * For many implementations, each progress item has a "Weight" attribute representing the weight of that item in the overall calculation.
    * For those implementations, this should return a map containing each progress item mapped to its weight.
    * If progress items don't have weights, then the developer should decide here how it should be represented to the user on the progress page.
  * getImplementationCompletionPercentage()
    * This returns the progress percentage, except for a specific user instead of for a list of all users. 
    * It's needed to display data correctly to the user on the progress page.

## Database Structure / Hibernate Entities
* ProgressConfigurationType
   * Represents the data setup for a single implementation of IProgress.
   * Contains the class name of the IProgress implementation so that the code can find it.
   * Contains a list of ProgressConfigurationAttribute.
* ProgressConfigurationAttribute
   * Represents a single attribute for either configurations or progress items used by this particular IProgress implementation.
   * Contains a flag indicating if this attribute should apply to the entire configuration, or each individual progress item.
   * Contains the default value for this attribute.
   * Links to a ProgressAttributeType.
* ProgressAttributeType
   * Represents a Java data type.
   * When loading values of the specified ProgressConfigurationAttribute, the module (and the implementations) will attempt to cast the String pulled from the database to this data type. 
* ProgressSiteConfiguration
   * Represents the configuration for a single implementation at a single site.
   * Each site should have one for all the possible implementations.
   * Contains a flag determining whether or not the specified progress item is active. 
   * Contains the overall weight of this specific configuration for the overall site. 
   * Contains a list of ProgressItem, and a list of ProgressConfigurationAttributeValue.
* ProgressConfigurationAttributeValue
   * Represents a single configuration setting. 
   * Contains the value of the setting. 
   * Links back to the ProgressConfigurationAttribute that this setting is for. 
   * This table contains values only for ProgressConfigurationAttributes which apply to the entire configuration. (configWideAttribute==True)
* ProgressItem
   * Represents a single Progress Item. 
   * Contains a flag determining whether or not the specified progress item is active. 
   * Contains a list of ProgressAttributeValue.
* ProgressAttributeValue
   * Represents a single configuration setting. 
   * Contains the value of the setting. 
   * Links back to the ProgressConfigurationAttribute that this setting is for. 
   * This table contains values only for ProgressConfigurationAttributes which apply to individual ProgressItems. (configWideAttribute==False)
