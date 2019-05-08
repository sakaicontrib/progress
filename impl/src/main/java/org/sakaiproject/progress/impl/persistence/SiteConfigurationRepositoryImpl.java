package org.sakaiproject.progress.impl.persistence;

import java.util.List;

import org.hibernate.criterion.Restrictions;
import org.sakaiproject.progress.model.data.entity.ProgressSiteConfiguration;
import org.sakaiproject.progress.model.data.repository.SiteConfigurationRepository;
import org.sakaiproject.serialization.BasicSerializableRepository;
import org.springframework.transaction.annotation.Transactional;

@SuppressWarnings("unchecked")
public class SiteConfigurationRepositoryImpl extends BasicSerializableRepository<ProgressSiteConfiguration, Integer> implements SiteConfigurationRepository {

	@Override
	public ProgressSiteConfiguration findConfiguration(int id) {
		return findOne(id);
	}

	@Override
	@Transactional
	public boolean newConfiguration(ProgressSiteConfiguration config) {
		if (!existsConfiguration(config.getId())) {
			sessionFactory.getCurrentSession().persist(config);
			return true;
		}
		return false;
	}

	@Override
	@Transactional
	public boolean deleteConfiguration(int id) {
		ProgressSiteConfiguration config = findOne(id);
        if (config != null) {
            delete(config);
            return true;
        }
        return false;
	}

	@Override
	@Transactional
	public boolean updateConfiguration(ProgressSiteConfiguration config) {
		if (existsConfiguration(config.getId())) {
			sessionFactory.getCurrentSession().merge(config);
			return true;
		}
		return false;
	}

	@Override
	public boolean existsConfiguration(int id) {
		if (exists(id)) {
            return true;
        }
        return false;
	}

	@Override
	public List<ProgressSiteConfiguration> findConfigsBySite(String siteId) {
		return startCriteriaQuery()
				.add(Restrictions.eq("siteId", siteId))
				.list();
	}

	//TODO: make sure to test that this actually works
	@Override
	public List<ProgressSiteConfiguration> findConfigsByType(String typeName) {
		return startCriteriaQuery()
				.createAlias("configType", "c")
				.add(Restrictions.eq("c.type", typeName))
				.list();
	}

	//TODO: make sure to test that this actually works
	@Override
	public List<ProgressSiteConfiguration> findConfigBySiteAndType(String siteId, String typeName) {
		return startCriteriaQuery()
				.add(Restrictions.eq("siteId", siteId))
				.createAlias("configType", "c")
				.add(Restrictions.eq("c.type", typeName))
				.list();
	}

}
