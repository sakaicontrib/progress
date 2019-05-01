package org.sakaiproject.progress.impl.persistence;

import java.util.List;

import org.hibernate.criterion.Restrictions;
import org.sakaiproject.progress.model.data.entity.ProgressAttributeType;
import org.sakaiproject.progress.model.data.repository.AttributeTypeRepository;
import org.sakaiproject.serialization.BasicSerializableRepository;
import org.springframework.transaction.annotation.Transactional;

@SuppressWarnings("unchecked")
public class AttributeTypeRepositoryImpl extends BasicSerializableRepository<ProgressAttributeType, Integer> implements AttributeTypeRepository {

	@Override
	public ProgressAttributeType findAttributeType(int id) {
		return findOne(id);
	}

	@Override
	@Transactional
	public boolean newAttributeType(ProgressAttributeType attributeType) {
		if (!existsAttributeType(attributeType.getId())) {
			sessionFactory.getCurrentSession().persist(attributeType);
			return true;
		}
		return false;
	}

	@Override
	@Transactional
	public boolean deleteAttributeType(int id) {
        ProgressAttributeType attributeType = findOne(id);
        if (attributeType != null) {
            delete(attributeType);
            return true;
        }
        return false;
	}

	@Override
	@Transactional
	public boolean updateAttributeType(ProgressAttributeType attributeType) {
		if (existsAttributeType(attributeType.getId())) {
			sessionFactory.getCurrentSession().merge(attributeType);
			return true;
		}
		return false;
	}
	
	@Override
	public boolean existsAttributeType(int id) {
		if (exists(id)) {
            return true;
        }
        return false;
	}

	@Override
	public List<ProgressAttributeType> getAllTypes() {
		return startCriteriaQuery().list();
	}

	@Override
	public List<ProgressAttributeType> findTypeByName(String name) {
		return startCriteriaQuery()
				.add(Restrictions.eq("name", name))
				.list();
	}


}
