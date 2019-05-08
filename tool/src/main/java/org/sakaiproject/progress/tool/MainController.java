/****************************************************************************** 
 * Copyright 2015 sakaiproject.org Licensed under the Educational
 * Community License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 * 
 * http://opensource.org/licenses/ECL-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
package org.sakaiproject.progress.tool;

import lombok.extern.slf4j.Slf4j;
import org.sakaiproject.progress.api.*;
import org.sakaiproject.progress.api.IProgress.ValidationResult;
import org.sakaiproject.progress.model.ConfigForm;
import org.sakaiproject.progress.model.data.entity.ProgressSiteConfiguration;
import org.sakaiproject.tool.api.ToolManager;

import org.sakaiproject.user.api.User;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

//import org.sakaiproject.progress.impl.AssignmentProgessImpl;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.*;

/**
 * MainController
 * 
 * This is the controller used by Spring MVC to handle requests
 *
 * The concept here is to only hand marshalling data to and from the templates.
 * All the business logic should exist in the service.
 * 
 */
@Slf4j
@Controller
public class MainController {

	@Autowired
	private IProgressValidator progressValidator;

	@Resource(name = "org.sakaiproject.progress.impl.ImplementationChooserImpl")
	private ImplementationControllerInterface implementationChooser;

	@Resource(name = "org.sakaiproject.progress.impl.ConfigurationManager")
	private IConfiguration configurationManager;

	@Resource(name = "org.sakaiproject.tool.api.ToolManager")
	private ToolManager toolManager;

	/**
	 * Main index page mapping
	 * @param model - The model of data that is injected into the template
	 * @return String that determines what page should be loaded next
	 */
	@RequestMapping(value = {"/", "/index"}, method = RequestMethod.GET)
	public String pageIndex(Model model) {

		//Gets the context - Used as the siteID
		String context = toolManager.getCurrentPlacement().getContext();

		ProgressRole role = implementationChooser.getImplementations().get(0).getRole(context);
		if(role == ProgressRole.INSTRUCTOR || role == ProgressRole.TA){
			Map<String, Integer> progressMap = implementationChooser.CalculateProgress(context);

			List<User> users = implementationChooser.getStudents(context);

			model.addAttribute("role", "instructor");
			model.addAttribute("users", users);
			model.addAttribute("progress", progressMap);
		}
		else{
			Map<String, Integer> progressMap = implementationChooser.CalculateProgress(context);

			List<User> users = implementationChooser.getStudents(context);

			model.addAttribute("role", "student");
			model.addAttribute("users", users);
			model.addAttribute("progress", progressMap);
		}
		return "index";
	}

	/**
	 * Returns a Json String representation of the specified userId
	 * @param model
	 * @param uid
	 * @return JSON String of progress data for a user
	 */
	@ResponseBody
    @RequestMapping(value = "/indivProgress/{id}", method = RequestMethod.GET)
    public String getIndividualProgress(Model model, @PathVariable("id") String uid){
		String context = toolManager.getCurrentPlacement().getContext();

		String result = implementationChooser.getIndividualProgressInJSON(context, uid);

	    return result;
    }

	/**
	 * Mapping for when a get request is made to load the config page
	 * @param model - The model of data that is injected into the template
	 * @return String that determines what page should be loaded next
	 */
	@RequestMapping(value = {"/config"}, method = RequestMethod.GET)
	public String showConfig(Model model){
		String context = toolManager.getCurrentPlacement().getContext();

		ConfigForm configForm = new ConfigForm();
		configForm.setConfigs(implementationChooser.getProgressSiteConfigurations(context));
		model.addAttribute("configForm", configForm);

		return "config";
	}

	@RequestMapping(value = {"/config"}, method = RequestMethod.POST)
	public String postConfig(Model model, HttpServletRequest request) {
		
		//NOTE: because of issue with rendering th:field in thymeleaf templates
		//we were not able to take advantage of thymeleaf binding. So we had to write
		//an entire parser to parse the response and map it back to the config.
		//If somebody could figure out how to fix the th:field bug then they could get rid of the parser
		//and simplify the logic significantly. 

		String context = toolManager.getCurrentPlacement().getContext();
		

		//Uses HttpServletRequest due to error in th:field
		List<ProgressSiteConfiguration> updatedConfigs = RequestParamParser.updateProgressSiteConfigs(request.getParameterMap()
				,implementationChooser.getProgressSiteConfigurations(context));
		
		List<String> errorMessages = new ArrayList<String>();

		/**
		 * Performs checks on the post data to validate it
		 */
		double totalConfigWeight = 0.0;
		boolean allConfigsInactive = true;
		for (ProgressSiteConfiguration siteConfig : updatedConfigs) {
			if (siteConfig.isActive()){
				totalConfigWeight += siteConfig.getWeight();
				allConfigsInactive = false;
			}
		}
		
		if (allConfigsInactive) {
			errorMessages.add("No configurations are active. Progress page will display the progress scores from the gradebook.");
		}
		else if (totalConfigWeight != 100.0) {
			errorMessages.add("All configuration weights must add up to 100.");
		}
		else {		
			for (ProgressSiteConfiguration siteConfig : updatedConfigs) {
				try {
					if(siteConfig.isActive()) {
						ValidationResult validationResult = progressValidator.isValid(siteConfig);
						if (validationResult != null && validationResult.isValid()) {
							configurationManager.setConfiguration(siteConfig);
						} else {
							errorMessages.add(validationResult.getErrorMessage());
						}
					}
					else {
						configurationManager.setConfiguration(siteConfig);
					}
				}
				//we REALLY should be returning error 500 here.......
				catch (ClassNotFoundException ex) {
					log.error("Unable to find class of name " + siteConfig.getConfigType().getImplClassName());
					log.error(ex.getMessage(), ex);
					errorMessages.add("INTERNAL ERROR: Unable to find class of name " + siteConfig.getConfigType().getImplClassName());
				}
				catch (BeansException ex) {
					log.error("Unable to find or create bean of type " + siteConfig.getConfigType().getImplClassName());
					log.error(ex.getMessage(), ex);
					errorMessages.add("Unable to find or create bean of type " + siteConfig.getConfigType().getImplClassName());
				}
			}
		}
		
		ConfigForm newForm = new ConfigForm();
		newForm.setConfigs(implementationChooser.getProgressSiteConfigurations(context));
		model.addAttribute("configForm", newForm);
		model.addAttribute("errorMessages", errorMessages);
		
		return "config";
	}
}
