package org.sakaiproject.progress.model;

import java.util.List;

import org.sakaiproject.progress.model.data.entity.ProgressSiteConfiguration;

/**
 * Stores the Config Form for the Progress application
 */
public class ConfigForm {

    private List<ProgressSiteConfiguration> configs;

	public List<ProgressSiteConfiguration> getConfigs() {
		return configs;
	}

	public void setConfigs(List<ProgressSiteConfiguration> configs) {
		this.configs = configs;
	}

}
