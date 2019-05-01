package org.sakaiproject.progress.api;

import org.sakaiproject.progress.model.data.entity.ProgressSiteConfiguration;
import org.sakaiproject.user.api.User;

import java.util.List;
import java.util.Map;

/**
 * <p>
 *     ImplementationControllerInterface is the service that calls to and orchestrates the implementations it sits
 *     between the implementations and the Main Controller.
 * </p>
 */

public interface ImplementationControllerInterface {

    /**
     * Gets all implementations for the current site set with their matching ProgressSiteConfiguration
     * @param siteID
     * @return
     */
    Map<ProgressSiteConfiguration, IProgress> getImplementation(String siteID);

    /**
     * Calculates completion percentages of each student based on all IProgress implementations calculations.
     * @param siteID
     * @return Map - Uid's matched to their overall completion percentage
     */
    Map<String, Integer> CalculateProgress(String siteID);

    /**
     * Gets a list of User objects for all students in a site
     * @param siteId
     * @return List of Users in a site
     */
    List<User> getStudents(String siteId);

    /**
     * Gets a List of the ProgressSiteConfigurations for a site.
     * @param SiteID
     * @return List of the ProgressSiteConfigurations for a site.
     */
    List<ProgressSiteConfiguration> getProgressSiteConfigurations(String SiteID);

    /**
     * Gets a List of all IProgress implementation beans.
     * @return List of all IProgress implementation beans
     */
    List<IProgress> getImplementations();

    /**
     * Gets the individual progress of a student and returns a JSON string with all Iprogress implementation completion
     * percentages, along with their ProgressItems, the Progress Item completion percentage, and the ProgressItem weight
     * @param SiteID
     * @param uid
     * @return JSON String
     */
    String getIndividualProgressInJSON(String SiteID, String uid);
}
