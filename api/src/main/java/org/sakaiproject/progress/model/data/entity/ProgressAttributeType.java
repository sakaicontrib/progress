package org.sakaiproject.progress.model.data.entity;

import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;

@Entity
@Table(name = "PRG_ATTRIBUTE_TYPE")
public class ProgressAttributeType {
	
	@Id
	@Column(name = "ID")
	@GeneratedValue(strategy = GenerationType.AUTO)
	private int id;
	
	@Column(name="NAME")
	private String name;
	
	/*
	 * for future use we can add the class name to the table. this will allow future
	 * implementations to use custom classes However it will require finding some
	 * way to represent the custom class as a string in the DB.
	 * 
	 * @Column(name="CLASS")
	 * private String className;
	 */

	@OneToMany(cascade = CascadeType.ALL, mappedBy = "attributeType")
	private List<ProgressConfigurationAttribute> attributesOfType;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<ProgressConfigurationAttribute> getAttributesOfType() {
		return attributesOfType;
	}

	public void setAttributesOfType(List<ProgressConfigurationAttribute> attributesOfType) {
		this.attributesOfType = attributesOfType;
	}

}
