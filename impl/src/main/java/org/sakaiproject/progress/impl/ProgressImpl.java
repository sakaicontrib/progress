package org.sakaiproject.progress.impl;

import java.util.List;
import java.util.Map;

import org.sakaiproject.progress.api.IProgress;
import org.sakaiproject.progress.api.ProgressRole;
import org.sakaiproject.progress.model.data.entity.ProgressConfigurationAttribute;
import org.sakaiproject.progress.model.data.entity.ProgressConfigurationAttributeValue;
import org.sakaiproject.progress.model.data.entity.ProgressItem;
import org.sakaiproject.progress.model.data.entity.ProgressSiteConfiguration;
import org.sakaiproject.service.gradebook.shared.GradebookFrameworkService;
import org.sakaiproject.service.gradebook.shared.GradebookNotFoundException;
import org.sakaiproject.service.gradebook.shared.GradebookService;
import org.sakaiproject.tool.gradebook.Gradebook;
import org.sakaiproject.user.api.User;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
public abstract class ProgressImpl implements IProgress {
    
    @Setter protected GradebookService gradebookService;
    
    @Setter protected GradebookFrameworkService gradebookFrameworkService;

    @Autowired CommonMethods commonMethods;

    /**
     * Gets the UUID for the current user
     * @param siteID - Describes the portlet context - generated with ToolManager.getCurrentPlacement().getContext()
     * @return
     */
	@Override
	public String getUser(String siteID) {
        return commonMethods.getUser(siteID);
	}

    /**
     * Gets the role of the current user
     * @param siteID - Describes the portlet context - generated with ToolManager.getCurrentPlacement().getContext()
     * @return
     */
	@Override
	public ProgressRole getRole(String siteID) {

       return commonMethods.getRole(siteID);
	}

    /**
     * Gets a list of Students in the class.
     * @param siteId - Describes the portlet context - generated with ToolManager.getCurrentPlacement().getContext()
     * @return Returns all students if it is an instructor or only one if the current user is a student.
     */
	@Override
	public List<User> getStudents(String siteId) {
        return commonMethods.getStudents(siteId);
	}
	
    /**
     * Gets a List of Student UUIDs for the current site.
     * @param siteID - Describes the portlet context - generated with ToolManager.getCurrentPlacement().getContext()
     * @return A list of student UUIDs
     */
    protected List<String> getStudentsUIDs(String siteID){
        return commonMethods.getStudentsUIDs(siteID);
    }
    
    /**
     * Gets the gradebook for the current site.
     * @param siteID - Describes the portlet context - generated with ToolManager.getCurrentPlacement().getContext()
     * @return A Gradebook for the current site
     */
    protected Gradebook getGradebook(String siteID){

        Gradebook gradebook = null;

        //Sets the gradebook or creates it if it does not exist.
        try {
            gradebook = (Gradebook) this.gradebookService.getGradebook(siteID);
        } catch (final GradebookNotFoundException e){
            this.gradebookFrameworkService.addGradebook(siteID, siteID);

            try {
                gradebook = (Gradebook) this.gradebookService.getGradebook(siteID);
            } catch (final GradebookNotFoundException e1) {
                log.error("Request made and could not add inaccessible gradebookUid={}", siteID);
            }
        }

        return gradebook;
    }

	@Override
	public abstract Map<String, Integer> getProgress(String siteID, ProgressSiteConfiguration progressSiteConfiguration);

	@Override
    public abstract ProgressSiteConfiguration getProgressItems(String siteID, ProgressSiteConfiguration progressSiteConfiguration);
}
