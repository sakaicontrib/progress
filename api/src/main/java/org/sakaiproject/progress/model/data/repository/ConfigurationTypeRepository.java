package org.sakaiproject.progress.model.data.repository;

import java.util.List;

import org.sakaiproject.hibernate.CrudRepository;
import org.sakaiproject.progress.model.data.entity.ProgressConfigurationType;

public interface ConfigurationTypeRepository extends CrudRepository<ProgressConfigurationType, Integer> {

	ProgressConfigurationType findConfigurationType(int id);
	
	boolean newConfigurationType(ProgressConfigurationType configType);
	
	boolean deleteConfigurationType(int id);
	
	boolean updateConfigurationType(ProgressConfigurationType configType);
	
	boolean existsConfigurationType(int id);
	
	List<ProgressConfigurationType> findConfigurationByName(String name);
	
	List<ProgressConfigurationType> findConfigurationByClass(String className);
	
	List<ProgressConfigurationType> getAllConfigs();
	
	
}
