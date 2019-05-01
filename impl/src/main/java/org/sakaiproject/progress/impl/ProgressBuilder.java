package org.sakaiproject.progress.impl;

import java.util.ArrayList;
import java.util.List;

import org.sakaiproject.progress.api.ProgressServiceException;
import org.sakaiproject.progress.model.data.entity.ProgressAttributeValue;
import org.sakaiproject.progress.model.data.entity.ProgressConfigurationAttribute;
import org.sakaiproject.progress.model.data.entity.ProgressConfigurationAttributeValue;
import org.sakaiproject.progress.model.data.entity.ProgressConfigurationType;
import org.sakaiproject.progress.model.data.entity.ProgressItem;
import org.sakaiproject.progress.model.data.entity.ProgressSiteConfiguration;
import org.springframework.beans.factory.annotation.Autowired;

public class ProgressBuilder {
	
	@Autowired private ConfigurationManager manager;
	
	/**
	 * Builds a new empty progress item. This is intended for use by the implementation
	 * so that it can make a new progress item if it's not already in the database. 
	 * 
	 * Note that it doesn't actually add the progress item to the siteConfig, it's up to 
	 * the implementation to do that. 
	 * 
	 * Note that all new items by default are not active.
	 * 
	 * @param name The name of the new progress item
	 * @param siteConfig The siteConfig that the progress item will be paired with
	 * @return The new progress item
	 * @throws ProgressServiceException If there is already a progress item with that name.
	 */
	public ProgressItem buildProgressItem(String name, ProgressSiteConfiguration siteConfig) throws ProgressServiceException {
		List<ProgressItem> existingItems = siteConfig.getProgressItems();
		for (ProgressItem existingItem : existingItems) {
			if (existingItem.getName().equals(name)) {
				throw new ProgressServiceException("Cannot have two progress items with the same name: " + name);
			}
		}
		ProgressItem newItem = new ProgressItem();
		List<ProgressConfigurationAttribute> attributesToAdd = siteConfig.getConfigType().getAttributes();
		List<ProgressAttributeValue> newAttributes = new ArrayList<ProgressAttributeValue>();
		for (ProgressConfigurationAttribute attribute : attributesToAdd) {
			if (!attribute.isConfigWideAttribute()) {
				ProgressAttributeValue newValue = new ProgressAttributeValue();
				newValue.setAttribute(attribute);
				newValue.setValue(attribute.getDefaultValue());
				newValue.setItem(newItem);
				newAttributes.add(newValue);
			}
		}
		newItem.setSiteConfig(siteConfig);
		newItem.setName(name);
		newItem.setActive(false);
		newItem.setValues(newAttributes);
		return newItem;
	}

	/**
	 * Creates a new siteConfiguration for a config type and site pair.
	 * Mostly should be used when a new site is created. 
	 * 
	 * @param type
	 * @param site
	 * @return
	 * @throws ProgressServiceException
	 */
	public ProgressSiteConfiguration buildSiteConfiguration(String type, String site) throws ProgressServiceException{
		List<ProgressSiteConfiguration> existingConfigs = manager.getConfigurations(site);
		for (ProgressSiteConfiguration existingConfig : existingConfigs) {
			if (existingConfig.getConfigTypeName().equals(type)) {
				throw new ProgressServiceException("Each site can only have one instance of each configuration type: configType " + type + ", site: " + site);
			}
		}
		ProgressSiteConfiguration newSiteConfig = new ProgressSiteConfiguration();
		List<ProgressConfigurationType> configType = manager.getConfigType(type);
		if (configType == null || configType.size() < 1) {
			throw new ProgressServiceException("Unable to find a configuration type of name " + type);
		}
		//If the config manager returned more than one config type then something is seriously wrong.
		//But if we just pretend like it didn't happen then it will all be ok
		newSiteConfig.setConfigType(configType.get(0));
		
		List<ProgressConfigurationAttribute> attributesToAdd = newSiteConfig.getConfigType().getAttributes();
		List<ProgressConfigurationAttributeValue> newAttributes = new ArrayList<ProgressConfigurationAttributeValue>();
		for(ProgressConfigurationAttribute attribute : attributesToAdd) {
			if (attribute.isConfigWideAttribute()) {
				ProgressConfigurationAttributeValue newAttribute = new ProgressConfigurationAttributeValue();
				newAttribute.setSiteConfig(newSiteConfig);
				newAttribute.setAttribute(attribute);
				newAttribute.setValue(attribute.getDefaultValue());
				newAttributes.add(newAttribute);
			}
		}
		newSiteConfig.setSiteId(site);
		newSiteConfig.setActive(false);
		newSiteConfig.setConfigValues(newAttributes);
		
		return newSiteConfig;
		
	}
}
