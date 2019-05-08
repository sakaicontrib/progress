package org.sakaiproject.progress.impl;

import com.google.gson.Gson;

import org.sakaiproject.progress.api.ImplementationControllerInterface;
import org.sakaiproject.progress.api.IProgress;

import lombok.extern.slf4j.Slf4j;
import org.sakaiproject.progress.api.ProgressServiceException;
import org.sakaiproject.progress.model.data.entity.ProgressItem;
import org.sakaiproject.progress.model.data.entity.ProgressSiteConfiguration;
import org.sakaiproject.user.api.User;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;

@Slf4j

/**
 * <p>
 *     Calls to and orchestrates the implementations it sits between the implementations and the Main Controller. Allows
 *     future development to implement their classes and logic without editing any code except their own.
 * </p>
 */
public class ImplementationController implements ImplementationControllerInterface {

    @Autowired private ConfigurationManager configurationManager;

    @Autowired private List<IProgress> progresses;

    @Autowired private CommonMethods commonMethods;
    
    @Autowired private ProgressBuilder progressBuilder;

    /**
     * Gets all implementations for the current site set with their matching ProgressSiteConfiguration
     * @param siteID
     * @return Map
     */
    @Override
    public Map<ProgressSiteConfiguration, IProgress> getImplementation(String siteID) {

            List<ProgressSiteConfiguration> configuration = this.getProgressSiteConfigurations(siteID);
            Map<ProgressSiteConfiguration, IProgress> chosenProgress = new HashMap<>();

            // If there is no current configuration, default to gradebook
            if (configuration.size() < 1) {
            	try {

            		ProgressSiteConfiguration gradebookProgress = configurationManager.getConfiguration(siteID, "Gradebook").get(0);
            		chosenProgress.put(gradebookProgress, getProgress("org.sakaiproject.progress.impl.GradebookProgressImpl"));
            	}
            	catch(Exception ex) {
            		throw new ProgressServiceException("Unable to find SiteConfiguration with type gradebook.");
            	}
            }
            else{
                for(ProgressSiteConfiguration pi : configuration){

                    //Only includes active ProgressSiteConfigurations and the corresponding IProgress Implementation
                    if(pi.isActive()){
                        IProgress temp = getProgress(pi.getConfigType().getImplClassName());

                        //Prevents adding a null implementation in the event of a db error
                        if(temp != null){
                            chosenProgress.put(pi, temp);
                        }
                    }
                }

                //If no configurations are active use the gradebook
                if(chosenProgress.size() == 0){
                	try {
                		ProgressSiteConfiguration gradebookProgress = configurationManager.getConfiguration(siteID, "gradebook").get(0);
                		gradebookProgress.setAttributeValueByName("Use Course Grade", "True"); //if the gradebook isn't active lets just display the course grade
                		gradebookProgress.setWeight(100.0);
                		chosenProgress.put(gradebookProgress, getProgress("org.sakaiproject.progress.impl.GradebookProgressImpl"));
                	}
                	catch(Exception ex) {
                		throw new ProgressServiceException("Unable to find SiteConfiguration with type gradebook.", ex);
                	}
                }

            }

            return chosenProgress;
    }

    /**
     * Calculates completion percentages of each student based on all IProgress implementations calculations.
     * @param siteID
     * @return Map - Uid's matched to their overall completion percentage
     */
    @Override
    public Map<String, Integer> CalculateProgress(String siteID) {

        Map<ProgressSiteConfiguration, IProgress> implementations = getImplementation(siteID);

        Map<String, Double> resultTemp = new HashMap<>();
        Map<String, Integer> result = new HashMap<>();

        /**
         * implementations should never be null without throwing an error, but prevents any errors in getting the implementations
         * from erroring out the progress tool
         */

        if(implementations != null && implementations.size() != 0){

            //Loops through all implementations
            for(Map.Entry<ProgressSiteConfiguration, IProgress> entry : implementations.entrySet()){

                Map<String, Integer> temp = entry.getValue().getProgress(siteID, entry.getKey());

                //Loops through all users and their progress items to apply the ProgressSiteConfiguration weight
                for(Map.Entry<String, Integer> tempEntry : temp.entrySet()){

                    Double tempValue = new Double(tempEntry.getValue());

                    //If the user has not been added to the resultTemp Map yet.
                    if(!resultTemp.containsKey(tempEntry.getKey())){
                        try {
                            if(entry.getKey().getWeight() != 0){
                                Double weight = new Double(entry.getKey().getWeight());
                                tempValue *= (weight/100);

                                resultTemp.put(tempEntry.getKey(), tempValue);
                            }
                            //Should not occur but figures for the event of no weight
                            else{
                                resultTemp.put(tempEntry.getKey(), tempValue);
                            }
                        }
                        //Should not occur but figures for the event of a bad value in the db
                        catch (NullPointerException npe){
                            resultTemp.put(tempEntry.getKey(), tempValue);
                        }
                    }
                    //Runs if the user is already in the resultTemp Map
                    else{
                        try {
                            if(entry.getKey().getWeight() != 0){
                                Double weight = new Double(entry.getKey().getWeight());
                                tempValue *= (weight/ 100);

                                resultTemp.put(tempEntry.getKey(), resultTemp.get(tempEntry.getKey()) + tempValue);
                            }
                            else{
                                resultTemp.put(tempEntry.getKey(), resultTemp.get(tempEntry.getKey()) + tempValue);
                            }
                        }
                        catch(NullPointerException npe){
                            resultTemp.put(tempEntry.getKey(), resultTemp.get(tempEntry.getKey()) + tempValue);
                        }
                    }
                }

            }
        }

        //Changes the percentages to Integers
        for(Map.Entry<String, Double> tempEntry : resultTemp.entrySet()){
            result.put(tempEntry.getKey(), tempEntry.getValue().intValue());
        }

        return result;
    }

    /**
     * Gets a list of User objects for all students in a site
     * @param siteId
     * @return List of Users in a site
     */
    @Override
    public List<User> getStudents(String siteId) {
        return commonMethods.getStudents(siteId);
    }

    /**
     * Gets a List of the ProgressSiteConfigurations for a site.
     * @param SiteID
     * @return List of the ProgressSiteConfigurations for a site.
     */
    @Override
    public List<ProgressSiteConfiguration> getProgressSiteConfigurations(String SiteID) {

        List<ProgressSiteConfiguration> configs = configurationManager.getConfigurations(SiteID);

        //If no configurations are found for the site build them for all IProgress implementations
        if(configs == null){
            for(IProgress p : progresses){
                configurationManager.setConfiguration(progressBuilder.buildSiteConfiguration(p.getName(), SiteID));
            }
        }
        else{

            //Sets configs for all IProgress implementations that have not been stored
            for(IProgress p: progresses){

                boolean found = false;

                for(ProgressSiteConfiguration config : configs){
                    if(config.getConfigTypeName().equalsIgnoreCase(p.getName())){
                        found = true;
                        break;
                    }
                }

                if(!found){
                    configurationManager.setConfiguration(progressBuilder.buildSiteConfiguration(p.getName(), SiteID));
                }
            }
        }

        //gets updated configurations
        configs = configurationManager.getConfigurations(SiteID);

        //Has each IProgress implementation set their Progress Items
        for(ProgressSiteConfiguration config : configs){

            try {
                ProgressSiteConfiguration progressSiteConfiguration = getProgress(config.getConfigType().getImplClassName()).getProgressItems(SiteID, config);
                configurationManager.setConfiguration(progressSiteConfiguration);
            }
            catch(NullPointerException e){
                System.out.println("Cannot set Progress Implementation. It has no items");
            }
        }

        return configurationManager.getConfigurations(SiteID);
    }

    /**
     * Gets a List of all IProgress implementation beans.
     * @return List of all IProgress implementation beans
     */
    @Override
    public List<IProgress> getImplementations() {
        return progresses;
    }

    /**
     * Gets the individual progress of a student and returns a JSON string with all Iprogress implementation completion
     * percentages, along with their ProgressItems, the Progress Item completion percentage, and the ProgressItem weight
     * @param SiteID
     * @param uid
     * @return JSON String
     */
    @Override
    public String getIndividualProgressInJSON(String SiteID, String uid) {
        Gson gson = new Gson();

        List<Map<String, Object>> toJSON = new ArrayList<>();

        Map<ProgressSiteConfiguration, IProgress> implementations = getImplementation(SiteID);

        //Loops through all IProgress implementations to get the overall progress and information for each progress item
        for(Map.Entry<ProgressSiteConfiguration, IProgress> entry : implementations.entrySet()){

            Map<String, Object> tempMap = new HashMap<>();
            tempMap.put("implementation", entry.getValue().getName());

            tempMap.put("percentageComplete", entry.getValue().getImplementationCompletionPercentage(entry.getKey(), SiteID, uid));

            Map<ProgressItem, Integer> individualProgressMap = entry.getValue().getCompletionPercentagesForStudent(SiteID, uid, entry.getKey());
            Map<ProgressItem, Integer> progressItemWeight = entry.getValue().getProgressItemPercentageOfImplementation(SiteID, entry.getKey());
            List<Map<String, Object>> progressItemList = new ArrayList();

            if(individualProgressMap != null){
                for(Map.Entry<ProgressItem, Integer> progressEntry : individualProgressMap.entrySet()){
                    Map<String, Object> progressItems = new HashMap<>();

                    progressItems.put("progressItem", progressEntry.getKey().getName());
                    progressItems.put("percentage", progressEntry.getValue());
                    progressItems.put("percentageImplementation", progressItemWeight.get(progressEntry.getKey()));

                    progressItemList.add(progressItems);
                }
            }

            tempMap.put("progressItems", progressItemList);


            toJSON.add(tempMap);
        }

        String result = gson.toJson(toJSON);
        return result;
    }

    /**
     * returns the IProgress implementation that is requested in the params.
     * @param className
     * @return IProgress
     */
    private IProgress getProgress(String className){

        for(IProgress p : progresses){
            String s = p.toString();
            if(s.contains(className)){
                return p;
            }
        }

        return null;
    }
}
