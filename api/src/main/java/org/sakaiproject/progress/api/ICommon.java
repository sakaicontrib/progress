package org.sakaiproject.progress.api;

import org.sakaiproject.user.api.User;

import java.util.List;

/**
 * <p>
 *     ICommon is the service that contains common methods that are used between all of the implementations
 * </p>
 */

public interface ICommon {

    /**
     * Gets a list of all students in a site
     * @param siteId
     * @return List of Users
     */
    List<User> getStudents(String siteId);

    /**
     * Gets the uuid of the currently logged in user
     * @param siteID
     * @return String - uuid of the currently logged in user
     */
    String getUser(String siteID);

    /**
     * Gets the currently logged in user
     * @param siteID
     * @return User - the currently logged in user
     */
    User getCurrentUser(String siteID);

    /**
     * Gets the role of the current User, Should return Instructor, TA, Student, or None
     * @param siteID
     * @return ProgressRole - Enum with the currently logged in User's Role
     */
    ProgressRole getRole(String siteID);

    /**
     * Gets the role of specific user
     * @param siteID
     * @param userID
     * @return ProgressRole - Enum with the specified User's Role
     */
    ProgressRole getRole(String siteID, String userID);

    /**
     * Gets a list of uuid's for all students in a site
     * @param siteID
     * @return List - All uid's for students in the class.
     */
    List<String> getStudentsUIDs(String siteID);
}
