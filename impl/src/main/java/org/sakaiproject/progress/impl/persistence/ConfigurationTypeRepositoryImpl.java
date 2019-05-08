package org.sakaiproject.progress.impl.persistence;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.criterion.Restrictions;
import org.sakaiproject.progress.model.data.entity.ProgressConfigurationAttribute;
import org.sakaiproject.progress.model.data.entity.ProgressConfigurationType;
import org.sakaiproject.progress.model.data.repository.ConfigurationTypeRepository;
import org.sakaiproject.serialization.BasicSerializableRepository;
import org.springframework.transaction.annotation.Transactional;

@SuppressWarnings("unchecked")
public class ConfigurationTypeRepositoryImpl extends BasicSerializableRepository<ProgressConfigurationType, Integer> implements ConfigurationTypeRepository {

	@Override
	public ProgressConfigurationType findConfigurationType(int id) {
		return findOne(id);
	}

	@Override
	@Transactional
	public boolean newConfigurationType(ProgressConfigurationType configType) {
		if (!existsConfigurationType(configType.getId())) {
			sessionFactory.getCurrentSession().persist(configType);
			return true;
		}
		return false;
	}

	@Override
	@Transactional
	public boolean deleteConfigurationType(int id) {
		ProgressConfigurationType configType = findOne(id);
        if (configType != null) {
            delete(configType);
            return true;
        }
        return false;
	}

	@Override
	@Transactional
	public boolean updateConfigurationType(ProgressConfigurationType configType) {
		if (existsConfigurationType(configType.getId())) {
			ProgressConfigurationType temp = findOne(configType.getId());

			for(ProgressConfigurationAttribute attribute: configType.getAttributes()){
				boolean inDatabase = false;

				for(ProgressConfigurationAttribute dbAttribute: temp.getAttributes()){
					if(attribute.getId() == dbAttribute.getId()){
						inDatabase = true;
						break;
					}
				}

				if(!inDatabase){
					temp.getAttributes().add(attribute);
				}
			}

			List<ProgressConfigurationAttribute> toRemove = new ArrayList<>();
			for(ProgressConfigurationAttribute dbAttribute: temp.getAttributes()){

				boolean inConfigType = false;

				for(ProgressConfigurationAttribute attribute: configType.getAttributes()){
					if(attribute.getId() == dbAttribute.getId()){
						inConfigType = true;
						break;
					}
				}

				if(!inConfigType){
					toRemove.add(dbAttribute);
				}
			}

			for(ProgressConfigurationAttribute notInConfigType: toRemove){
				temp.getAttributes().remove(notInConfigType);
			}

			sessionFactory.getCurrentSession().update(temp);
			return true;
		}
		return false;
	}

	@Override
	public boolean existsConfigurationType(int id) {
		if (exists(id)) {
            return true;
        }
        return false;
	}

	@Override
	public List<ProgressConfigurationType> findConfigurationByName(String name) {
		return startCriteriaQuery()
				.add(Restrictions.eq("type", name))
				.list();
	}

	@Override
	public List<ProgressConfigurationType> findConfigurationByClass(String className) {
		return startCriteriaQuery()
				.add(Restrictions.eq("implClassName", className))
				.list();
	}

	@Override
	public List<ProgressConfigurationType> getAllConfigs() {
		return startCriteriaQuery().list();
	}

}
