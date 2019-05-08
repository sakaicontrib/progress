package org.sakaiproject.progress.model.data.repository;

import java.util.List;

import org.sakaiproject.hibernate.CrudRepository;
import org.sakaiproject.progress.model.data.entity.ProgressSiteConfiguration;

public interface SiteConfigurationRepository extends CrudRepository<ProgressSiteConfiguration, Integer> {
	
	ProgressSiteConfiguration findConfiguration(int id);
	
	boolean newConfiguration(ProgressSiteConfiguration config);
	
	boolean deleteConfiguration(int id);
	
	boolean updateConfiguration(ProgressSiteConfiguration config);
	
	boolean existsConfiguration(int id);
	
	List<ProgressSiteConfiguration> findConfigsBySite(String siteId);
	
	List<ProgressSiteConfiguration> findConfigsByType(String typeName);
	
	List<ProgressSiteConfiguration> findConfigBySiteAndType(String siteId, String typeName);

}
