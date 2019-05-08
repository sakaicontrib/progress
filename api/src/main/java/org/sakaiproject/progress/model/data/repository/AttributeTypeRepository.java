package org.sakaiproject.progress.model.data.repository;

import java.util.List;

import org.sakaiproject.hibernate.CrudRepository;
import org.sakaiproject.progress.model.data.entity.ProgressAttributeType;

public interface AttributeTypeRepository extends CrudRepository<ProgressAttributeType, Integer> {
	
	ProgressAttributeType findAttributeType(int id);
	
	boolean newAttributeType(ProgressAttributeType attributeType);
	
	boolean deleteAttributeType(int id);
	
	boolean updateAttributeType(ProgressAttributeType attributeType);
	
	boolean existsAttributeType(int id);
	
	List<ProgressAttributeType> getAllTypes();
	
	List<ProgressAttributeType> findTypeByName(String name);

}
