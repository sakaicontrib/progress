package org.sakaiproject.progress.tool;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.sakaiproject.progress.api.ProgressServiceException;
import org.sakaiproject.progress.model.data.entity.ProgressAttributeValue;
import org.sakaiproject.progress.model.data.entity.ProgressConfigurationAttributeValue;
import org.sakaiproject.progress.model.data.entity.ProgressItem;
import org.sakaiproject.progress.model.data.entity.ProgressSiteConfiguration;

public class RequestParamParser {

	protected static List<ProgressSiteConfiguration> updateProgressSiteConfigs(Map<String, String[]> requestParams, List<ProgressSiteConfiguration> siteConfigs) {
		//first gotta do some setup to make this more efficient
		Map<Integer, ProgressSiteConfiguration> siteConfigIdMap = new HashMap<Integer, ProgressSiteConfiguration>();
		for (ProgressSiteConfiguration siteConfig : siteConfigs) {
			siteConfigIdMap.put(siteConfig.getId(), siteConfig);
		}
		
		Map<Integer, Map<String, String>> paramsToSiteConfigMap = new HashMap<Integer, Map<String, String>>();
		
		for (String parameterName : requestParams.keySet()) {
			if (parameterName.equals("submit")) {
				//gotta skip this one
				continue;
			}
			int valueSeparator = parameterName.indexOf(".");
			String siteConfigIdStr = parameterName.substring(0, valueSeparator);
			String restOfParameter = parameterName.substring(valueSeparator + 1);

			int siteConfigId = Integer.parseInt(siteConfigIdStr.substring(siteConfigIdStr.indexOf("/") + 1));
			
			if (paramsToSiteConfigMap.get(siteConfigId) == null) {
				paramsToSiteConfigMap.put(siteConfigId, new HashMap<String, String>());
			}
			paramsToSiteConfigMap.get(siteConfigId).put(restOfParameter, requestParams.get(parameterName)[0]);
			
		}
		for (Integer siteConfigId : paramsToSiteConfigMap.keySet()) {
			updateProgressSiteConfig(siteConfigIdMap.get(siteConfigId), paramsToSiteConfigMap.get(siteConfigId));
		}
		
		return siteConfigs;
	}
	
	/**
	 * Takes as input a site configuration and the map of paramNames and paramValues to update for this config.
	 * @param siteConfig The Site Configuration to update
	 * @param params The map of Parameter Names and Parameter Values to update. 
	 */
	private static void updateProgressSiteConfig(ProgressSiteConfiguration siteConfig, Map<String, String> params) {
		//first gotta do some setup to make this more efficient
		Map<Integer, ProgressItem> progressItemIdMap = new HashMap<Integer, ProgressItem>();
		for (ProgressItem progressItem : siteConfig.getProgressItems()) {
			progressItemIdMap.put(progressItem.getId(), progressItem);
		}
		
		Map<Integer, ProgressConfigurationAttributeValue> configWideAttributeIdMap = new HashMap<Integer, ProgressConfigurationAttributeValue>();
		for (ProgressConfigurationAttributeValue progressItem : siteConfig.getConfigValues()) {
			configWideAttributeIdMap.put(progressItem.getId(), progressItem);
		}
		
		Map<Integer, Map<String, String>> paramsToProgressItemMap = new HashMap<Integer, Map<String, String>>();
		
		boolean setActive = false;

		for (String parameterName : params.keySet()) {
			int indexOfSlash = parameterName.indexOf("/");
			String typeOfItem;
			if (indexOfSlash == -1) {
				typeOfItem = parameterName;
			}
			else {
				typeOfItem = parameterName.substring(0, indexOfSlash);
			}
			if (typeOfItem.equals("active")) {
				updateSiteConfigIsActive(siteConfig, params.get(parameterName));
				setActive = true;
			}
			else if (typeOfItem.equals("weight")) {
				updateSiteConfigWeight(siteConfig, params.get(parameterName));
			}
			else if (typeOfItem.equals("configWideValue")) {
				int configWideAttributeId = Integer.parseInt(parameterName.substring(indexOfSlash + 1));
				updateConfigWideValue(configWideAttributeIdMap.get(configWideAttributeId), params.get(parameterName));
				configWideAttributeIdMap.remove(configWideAttributeId);
			}
			else if (typeOfItem.equals("progressItem")) {
				int indexOfSeparator = parameterName.indexOf(".");
				String restOfParameter = parameterName.substring(indexOfSeparator + 1);
				int progressItemId = Integer.parseInt(parameterName.substring(indexOfSlash + 1, indexOfSeparator));
				if (paramsToProgressItemMap.get(progressItemId) == null) {
					paramsToProgressItemMap.put(progressItemId, new HashMap<String, String>());
				}
				paramsToProgressItemMap.get(progressItemId).put(restOfParameter, params.get(parameterName));				
			}
			else {
				throw new ProgressServiceException("Invalid object type in parameter header");
			}
		}
		
		for (Integer progressItemId : paramsToProgressItemMap.keySet()) {
			updateProgressItem(progressItemIdMap.get(progressItemId), paramsToProgressItemMap.get(progressItemId));
		}
		
		//anything that wasn't present in the POST body needs to be reset back to it's default value
		for(ProgressConfigurationAttributeValue notPresentConfigAttribute : configWideAttributeIdMap.values()) {
			if(notPresentConfigAttribute.getAttributeType().equalsIgnoreCase("boolean")){
				updateConfigWideValue(notPresentConfigAttribute, "false");
			}
			else{
				updateConfigWideValue(notPresentConfigAttribute, notPresentConfigAttribute.getAttribute().getDefaultValue());
			}
		}
		
		//if we didn't set active earlier, that means that it wasn't present in the POST body
		//which means the user un-checked the box, so it needs to be false
		if (!setActive) {
			updateSiteConfigIsActive(siteConfig, "false");
		}
	}
	
	private static void updateSiteConfigIsActive(ProgressSiteConfiguration siteConfig, String newActive) {
		if (newActive.equals("true") || newActive.equals("checked") || newActive.equals("on")) {
			siteConfig.setActive(true);
		}
		else if (newActive.equals("false")) {
			siteConfig.setActive(false);
		}
		else {
			//somehow an invalid value was passed in, only possible if someone sends a fake POST request
			return;
		}
	}
	
	private static void updateSiteConfigWeight(ProgressSiteConfiguration siteConfig, String newWeight) {
		//need to do validations here to make sure that the user actually input a number
		//and that all of the new numbers actually do add up to 100
		siteConfig.setWeight(Double.parseDouble(newWeight));
	}
	
	private static void updateConfigWideValue(ProgressConfigurationAttributeValue configWideValue, String newValue) {
		//Should we be doing validations here? Isn't that the job of the implementation?
		//But if so, we aren't giving the implementation a chance to do it.

		if(newValue.equalsIgnoreCase("on")){
			newValue = "true";
		}
		else if(newValue.equals("")){
			newValue = configWideValue.getAttribute().getDefaultValue();
		}

		configWideValue.setValue(newValue);
	}
	
	/**
	 * Takes as input a progress item and the map of paramNames and paramValues to update for this item.
	 * @param siteConfig The Progress Item to update
	 * @param params The map of Parameter Names and Parameter Values to update. 
	 */
	private static void updateProgressItem(ProgressItem progressItem, Map<String, String> params) {
		Map<Integer, ProgressAttributeValue> progressAttributeValueMap = new HashMap<Integer, ProgressAttributeValue>();
		for (ProgressAttributeValue attribute : progressItem.getValues()) {
			progressAttributeValueMap.put(attribute.getId(), attribute);
		}
		
		boolean setActive = false;
		
		for (String paramName : params.keySet()) {
			int indexOfSlash = paramName.indexOf("/");
			String typeOfAttribute;
			if (indexOfSlash == -1) {
				typeOfAttribute = paramName;
			}
			else {
				typeOfAttribute = paramName.substring(0, indexOfSlash);
			}
			if (typeOfAttribute.equals("active")) {
				updateProgressItemIsActive(progressItem, params.get(paramName));
				setActive = true;
			}
			else if (typeOfAttribute.equals("progressItemAttribute")) {
				int attributeId = Integer.parseInt(paramName.substring(paramName.indexOf("/") + 1));
				updateProgressItemAttribute(progressAttributeValueMap.get(attributeId), params.get(paramName));
				progressAttributeValueMap.remove(attributeId);
			}
			else {
				throw new ProgressServiceException("Invalid object type in parameter header");
			}
		}
		
		//anything that wasn't present in the POST body needs to be reset back to it's default value
		for(ProgressAttributeValue notPresentAttribute : progressAttributeValueMap.values()) {
			if(notPresentAttribute.getAttributeType().equalsIgnoreCase("boolean")){
				updateProgressItemAttribute(notPresentAttribute, "false");
			}
			else{
				updateProgressItemAttribute(notPresentAttribute, notPresentAttribute.getAttribute().getDefaultValue());
			}
		}
		
		//if we didn't set active earlier, that means that it wasn't present in the POST body
		//which means the user un-checked the box, so it needs to be false
		if (!setActive) {
			updateProgressItemIsActive(progressItem, "false");
		}
	}
	
	private static void updateProgressItemIsActive(ProgressItem progressItem, String newActive) {
		if (newActive.equals("true") || newActive.equals("checked") || newActive.equals("on")) {
			progressItem.setActive(true);
		}
		else if (newActive.equals("false")) {
			progressItem.setActive(false);
		}
		else {
			//somehow an invalid value was passed in, only possible if someone sends a fake POST request
			return;
		}
	}
	
	private static void updateProgressItemAttribute(ProgressAttributeValue attribute, String newValue) {
		//Should we be doing validations here? Isn't that the job of the implementation?
		//But if so, we aren't giving the implementation a chance to do it.
		if(newValue.equalsIgnoreCase("on")){
			newValue = "true";
		}
		else if(newValue.equals("")){
			newValue = attribute.getAttribute().getDefaultValue();
		}

		attribute.setValue(newValue);
	}
	
	
}
