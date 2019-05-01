package org.sakaiproject.progress.api;

import org.sakaiproject.progress.model.data.entity.ProgressConfigurationType;
import org.sakaiproject.progress.model.data.entity.ProgressSiteConfiguration;
import org.sakaiproject.service.gradebook.shared.Assignment;

import java.util.List;
import java.util.Map;

public interface IConfiguration {

    /**
     * Gets the ProgressConfiguration from the database
     * @param siteID
     * @return ProgressConfiguration
     */
    public List<ProgressSiteConfiguration> getConfigurations(String siteID);

    /**
     * Sets the updated ProgressSiteConfiguration in the database.
     * @param config
     * @return whether or not the operation was successful.
     */
    public boolean setConfiguration(ProgressSiteConfiguration config);
    
    /**
     * Gets all ConfigurationTypes from the DB.
     * @return All the ProgressConfigurationTypes.
     */
    public List<ProgressConfigurationType> getAllConfigurations();
    
    /**
     * Gets a specific ProgressConfigurationType from the DB.
     * @param type The type to obtain.
     * @return A list containing the ProgressConfigurationType (should only have length 1).
     */
    public List<ProgressConfigurationType> getConfigType(String type);
    
    /**
     * Gets a specific ProgressSiteConfiguration from the DB.
     * @param siteID The SiteID for the config to obtain.
     * @param type The type of config to obtain.
     * @return A list containing the ProgressSiteConfiguration (should only have length 1).
     */
    public List<ProgressSiteConfiguration> getConfiguration(String siteID, String type);

}
