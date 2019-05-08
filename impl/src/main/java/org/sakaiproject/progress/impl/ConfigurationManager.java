package org.sakaiproject.progress.impl;

import org.sakaiproject.progress.api.IConfiguration;
import org.sakaiproject.progress.api.IProgress;
import org.sakaiproject.progress.api.ProgressServiceException;
import org.sakaiproject.progress.api.IProgress.Attribute;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;
import org.sakaiproject.progress.impl.persistence.ConfigurationTypeRepositoryImpl;
import org.sakaiproject.progress.model.data.entity.ProgressAttributeType;
import org.sakaiproject.progress.model.data.entity.ProgressAttributeValue;
import org.sakaiproject.progress.model.data.entity.ProgressConfigurationAttribute;
import org.sakaiproject.progress.model.data.entity.ProgressConfigurationAttributeValue;
import org.sakaiproject.progress.model.data.entity.ProgressConfigurationType;
import org.sakaiproject.progress.model.data.entity.ProgressItem;
import org.sakaiproject.progress.model.data.entity.ProgressSiteConfiguration;
import org.sakaiproject.progress.model.data.repository.AttributeTypeRepository;
import org.sakaiproject.service.gradebook.shared.*;
import org.sakaiproject.progress.model.data.repository.ConfigurationTypeRepository;
import org.sakaiproject.progress.model.data.repository.SiteConfigurationRepository;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PostConstruct;

/**
 * All the database interactions should be done through this class. 
 */
@Slf4j
public class ConfigurationManager implements IConfiguration {

    @Autowired public SiteConfigurationRepository siteConfigRepo;
    @Autowired public ConfigurationTypeRepository configTypeRepo;
    @Autowired public AttributeTypeRepository attributeTypeRepo;
    @Autowired public List<IProgress> progresses;

	/**
	 * Gets the progress configuration type from the database
	 * @param siteID
	 * @return List with the value. It should have only one item
	 */
	@Override
    public List<ProgressSiteConfiguration> getConfigurations(String siteID) {
        return siteConfigRepo.findConfigsBySite(siteID);
    }

	public boolean setConfiguration(ProgressSiteConfiguration config) {
		if (siteConfigRepo.existsConfiguration(config.getId())){
			siteConfigRepo.updateConfiguration(config);
		}
		else {
			siteConfigRepo.newConfiguration(config);
		}
		return true;
	}
	
	public List<ProgressConfigurationType> getAllConfigurations() {
		return configTypeRepo.getAllConfigs();
	}
	
	public List<ProgressConfigurationType> getConfigType(String type){
		return configTypeRepo.findConfigurationByName(type);
	}
	
	public List<ProgressSiteConfiguration> getConfiguration(String siteID, String type){
		return siteConfigRepo.findConfigBySiteAndType(siteID, type);
	}

	@PostConstruct
	public void initializeImplementations() {
		try {

			if (progresses == null) {
				throw new ProgressServiceException("No implementations found. No config data will be added to the database.");
			}

			for (IProgress implementation : progresses) {
				if(implementation.getName() == null) {
					throw new ProgressServiceException(implementation.getClass().getName() + ".getName() returned null. Is it defined?");
				}
				if(implementation.getAttributes() == null) {
					throw new ProgressServiceException(implementation.getClass().getName() + ".getAttributes() returned null. Is it defined?");
				}
				List<ProgressConfigurationType> existingTypeList = configTypeRepo.findConfigurationByName(implementation.getName());
				if (existingTypeList != null && existingTypeList.size() > 0) {
					validateDatabaseConfiguration(implementation, existingTypeList.get(0));
				}
				else {
					initNewImplementation(implementation);
				}
			}
		}
		catch (ProgressServiceException ex) {
			log.error("Unable to initialize implementations.", ex);
		}
	}

	/**
	 * Validates that the given implementation and the given databaseSetup match, and if they don't, updates the databaseSetup.
	 * @param implementation The implementation
	 * @param databaseSetup The way the implementation is currently represented in the database
	 */
	private void validateDatabaseConfiguration(IProgress implementation, ProgressConfigurationType databaseSetup) {
		boolean needToUpdateDatabase = false;
		
		if(!implementation.getName().equals(databaseSetup.getType())) {
			databaseSetup.setType(implementation.getName());
			needToUpdateDatabase = true;
		}
		if(!databaseSetup.getImplClassName().equals(implementation.getClass().getName())) {
			databaseSetup.setImplClassName(implementation.getClass().getName());
			needToUpdateDatabase = true;
		}
		
		//Loop through all the attributes from the implementation and make sure they are present in the database
		//If they are, validate them, if they aren't, create them
		if (implementation.getAttributes() != null) {
			for(Attribute attribute : implementation.getAttributes()) {
				boolean attributeInDatabase = false;
				for (ProgressConfigurationAttribute databaseAttribute : databaseSetup.getAttributes()) {
					if (attribute.getName().equals(databaseAttribute.getName())) {
						attributeInDatabase = true;
						if(attribute.isConfigWideAttribute() != databaseAttribute.isConfigWideAttribute()) {
							databaseAttribute.setConfigWideAttribute(databaseAttribute.isConfigWideAttribute());
							needToUpdateDatabase = true;
						}
						if(!attribute.getDefaultValue().equals(databaseAttribute.getDefaultValue())) {
							databaseAttribute.setDefaultValue(attribute.getDefaultValue());
							needToUpdateDatabase = true;
						}
						if(!attribute.getAttributeType().equals(databaseAttribute.getAttributeType().getName())) {
							List<ProgressAttributeType> existingAttributeList = attributeTypeRepo.findTypeByName(attribute.getAttributeType());
							if (existingAttributeList != null && existingAttributeList.size() > 0) {
								databaseAttribute.setAttributeType(existingAttributeList.get(0));
							}
							else {
								ProgressAttributeType newAttributeType = new ProgressAttributeType();
								newAttributeType.setName(attribute.getAttributeType());
								databaseAttribute.setAttributeType(newAttributeType);
								attributeTypeRepo.newAttributeType(newAttributeType);
							}
							needToUpdateDatabase = true;
						}
					}
				}
				if (!attributeInDatabase) {
					ProgressConfigurationAttribute newAttribute = new ProgressConfigurationAttribute();
					newAttribute.setConfigType(databaseSetup);
					newAttribute.setName(attribute.getName());
					newAttribute.setConfigWideAttribute(attribute.isConfigWideAttribute());
					newAttribute.setDefaultValue(attribute.getDefaultValue());
					List<ProgressAttributeType> existingAttributeList = attributeTypeRepo.findTypeByName(attribute.getAttributeType());
					if (existingAttributeList != null && existingAttributeList.size() > 0) {
						newAttribute.setAttributeType(existingAttributeList.get(0));
					}
					else {
						ProgressAttributeType newAttributeType = new ProgressAttributeType();
						newAttributeType.setName(attribute.getAttributeType());
						newAttribute.setAttributeType(newAttributeType);
						attributeTypeRepo.newAttributeType(newAttributeType);
					}
					needToUpdateDatabase = true;
					databaseSetup.getAttributes().add(newAttribute);
				}
			}
	
			//Now we need to loop through all existing databaseAttributes
			//and find the matching attribute from the implementation just to make sure it exists
			//If a database attribute exists but is not present in the implementation, just blow it away
			List<ProgressConfigurationAttribute> attributesToRemove = new ArrayList<>();
			for(ProgressConfigurationAttribute databaseAttribute: databaseSetup.getAttributes()){
				boolean attributeShouldExist = false;
				for (Attribute attribute : implementation.getAttributes()) {
					if (attribute.getName().equals(databaseAttribute.getName())) {
						attributeShouldExist = true;
					}
				}
				if (!attributeShouldExist) {
					attributesToRemove.add(databaseAttribute);
					needToUpdateDatabase = true;
				}
			}

			for(ProgressConfigurationAttribute databaseAttribute : attributesToRemove){
				databaseSetup.getAttributes().remove(databaseAttribute);
			}
			if (needToUpdateDatabase) {
				//if we need to update the database then we need to update the site configs associated with that configuration type
				configTypeRepo.updateConfigurationType(databaseSetup);
				updateAssociatedConfigs(databaseSetup);
				
			}
		}	
	}
	
	/**
	 * Given a ProgressConfigurationType, will check all the SiteConfigurations that use that type
	 * and add any attributes that are in the configuration type but not in the site configuration.
	 * 
	 * Note: We don't need to remove any, because hibernate orphan removal will take care of that for us. 
	 * @param databaseSetup
	 */
	private void updateAssociatedConfigs(ProgressConfigurationType databaseSetup){
		List<ProgressSiteConfiguration> associatedConfigs = databaseSetup.getSiteConfigs();
		for (ProgressSiteConfiguration config : associatedConfigs) {
			for (ProgressConfigurationAttribute attribute : databaseSetup.getAttributes()) {
				if (attribute.isConfigWideAttribute()) {
					boolean presentInConfig = false;
					for (ProgressConfigurationAttributeValue configWideAttributeValue : config.getConfigValues()) {
						if (configWideAttributeValue.getValueName().equals(attribute.getName())) {
							presentInConfig = true;
							break;
						}
					}	
					if (!presentInConfig) {
						ProgressConfigurationAttributeValue newAttributeValue = new ProgressConfigurationAttributeValue();
						newAttributeValue.setSiteConfig(config);
						newAttributeValue.setAttribute(attribute);
						newAttributeValue.setValue(attribute.getDefaultValue());
						
						config.getConfigValues().add(newAttributeValue);
					}
				}
				else {
					for(ProgressItem item : config.getProgressItems()) {
						boolean presentOnItem = false;
						for (ProgressAttributeValue itemAttributeValue : item.getValues()) {
							if (itemAttributeValue.getAttributeName().equals(attribute.getName())) {
								presentOnItem = true;
								break;
							}
						}
						if (!presentOnItem) {
							ProgressAttributeValue newAttributeValue = new ProgressAttributeValue();
							newAttributeValue.setItem(item);
							newAttributeValue.setAttribute(attribute);
							newAttributeValue.setValue(attribute.getDefaultValue());
							
							item.getValues().add(newAttributeValue);
						}
					}
				}
			}
			setConfiguration(config);
		}
	}
	
	/**
	 * Creates all the necessary data in the database for this implementation. 
	 * @param implementation The implementation to create.
	 */
	private void initNewImplementation(IProgress implementation) {
		ProgressConfigurationType newType = new ProgressConfigurationType();
		newType.setType(implementation.getName());
		newType.setImplClassName(implementation.getClass().getName());
		
		List<ProgressConfigurationAttribute> newAttributes = new ArrayList<ProgressConfigurationAttribute>();
		List<Attribute> implAttributes = implementation.getAttributes();
		for(Attribute attribute : implAttributes) {
			ProgressConfigurationAttribute newAttribute = new ProgressConfigurationAttribute();
			List<ProgressAttributeType> attributeTypeList = attributeTypeRepo.findTypeByName(attribute.getAttributeType());
			
			if(attributeTypeList != null && attributeTypeList.size() > 0) {
				//There should only ever be one type of each name, so this list should never be longer than 1.
				newAttribute.setAttributeType(attributeTypeList.get(0));
			}
			else {
				ProgressAttributeType newAttributeType = new ProgressAttributeType();
				newAttributeType.setName(attribute.getAttributeType());
				newAttribute.setAttributeType(newAttributeType);
				attributeTypeRepo.newAttributeType(newAttributeType);
			}
			
			newAttribute.setName(attribute.getName());
			newAttribute.setConfigWideAttribute(attribute.isConfigWideAttribute());
			newAttribute.setDefaultValue(attribute.getDefaultValue());
			newAttribute.setConfigType(newType);
			newAttributes.add(newAttribute);
		}
		
		newType.setAttributes(newAttributes);
		configTypeRepo.newConfigurationType(newType);
	}
}
