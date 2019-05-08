package org.sakaiproject.progress.impl;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.sakaiproject.authz.api.SecurityService;
import org.sakaiproject.exception.IdUnusedException;
import org.sakaiproject.progress.api.ICommon;
import org.sakaiproject.progress.api.ProgressRole;
import org.sakaiproject.progress.api.ProgressServiceException;
import org.sakaiproject.site.api.SiteService;
import org.sakaiproject.user.api.User;
import org.sakaiproject.user.api.UserDirectoryService;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * <p>
 *     Contains common methods that are used between all of the implementations
 * </p>
 */
@Slf4j
public class CommonMethods implements ICommon {

    @Setter private UserDirectoryService userDirectoryService;

    @Setter private SiteService siteService;

    @Setter private SecurityService securityService;

    /**
     * Gets a list of all students in a site
     * @param siteId
     * @return List of Users
     */
    @Override
    public List<User> getStudents(String siteId) {
        List<User> users = new ArrayList<>();
        ProgressRole role = getRole(siteId);

        //Prevents a student from viewing the entire class.
        if(role == ProgressRole.INSTRUCTOR || role == ProgressRole.TA){
            users = this.userDirectoryService.getUsers(this.getStudentsUIDs(siteId));
        }
        else{
            users.add(this.userDirectoryService.getCurrentUser());
        }

        return users;
    }

    /**
     * Gets the uuid of the currently logged in user
     * @param siteID
     * @return String - uuid of the currently logged in user
     */
    @Override
    public String getUser(String siteID) {
        return this.userDirectoryService.getCurrentUser().getId();
    }

    /**
     * Gets the currently logged in user
     * @param siteID
     * @return User - the currently logged in user
     */
    @Override
    public User getCurrentUser(String siteID){return this.userDirectoryService.getCurrentUser();}

    /**
     * Gets the role of the current User, Should return Instructor, TA, Student, or None
     * @param siteID
     * @return ProgressRole - Enum with the currently logged in User's Role
     */
    @Override
    public ProgressRole getRole(String siteID) {

        final String userId = this.getUser(siteID);

        return this.getUsersRole(siteID, userId);
    }

    /**
     * Gets the role of specific user
     * @param siteID
     * @param userID
     * @return ProgressRole - Enum with the specified User's Role
     */
    @Override
    public ProgressRole getRole(String siteID, String userID){
        return this.getUsersRole(siteID, userID);
    }

    /**
     * Gets a list of uuid's for all students in a site
     * @param siteID
     * @return List - All uid's for students in the class.
     */
    @Override
    public List<String> getStudentsUIDs(String siteID){
        Set<String> userUIDs = new HashSet<>();
        try {
            userUIDs = this.siteService.getSite(siteID).getUsersIsAllowed(ProgressRole.STUDENT.getValue());
        } catch (IdUnusedException e) {
            e.printStackTrace();
        }
        List<String> userIds = new ArrayList<String>();

        for(String id: userUIDs){
            userIds.add(id);
        }

        return userIds;
    }

    /**
     * Gets the role of specific user
     * @param siteID
     * @param userID
     * @return ProgressRole - Enum with the specified User's Role
     */
    private ProgressRole getUsersRole(String siteID, String userID){

        String siteRef;
        try {
            siteRef = this.siteService.getSite(siteID).getReference();
        } catch (final IdUnusedException e) {
            throw new ProgressServiceException(e.toString());
        }

        ProgressRole rval;

        if (this.securityService.unlock(userID, ProgressRole.INSTRUCTOR.getValue(), siteRef)) {
            rval = ProgressRole.INSTRUCTOR;
        } else if (this.securityService.unlock(userID, ProgressRole.TA.getValue(), siteRef)) {
            rval = ProgressRole.TA;
        } else if (this.securityService.unlock(userID, ProgressRole.STUDENT.getValue(), siteRef)) {
            rval = ProgressRole.STUDENT;
        } else {
            throw new ProgressServiceException("Current user does not have a valid section.role.x permission");
        }

        return rval;
    }
}
