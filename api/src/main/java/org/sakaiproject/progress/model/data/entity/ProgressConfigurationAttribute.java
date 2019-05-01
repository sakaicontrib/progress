package org.sakaiproject.progress.model.data.entity;

import java.util.List;

import javax.persistence.*;

@Entity
@Table(name = "PRG_CONFIG_ATTRIBUTE")
public class ProgressConfigurationAttribute {

	@Id
	@Column(name = "ID")
	@GeneratedValue(strategy = GenerationType.AUTO)
	private int id;

	@ManyToOne
	@JoinColumn(name = "ATTRIBUTE_TYPE_ID")
	private ProgressAttributeType attributeType;

	@ManyToOne
	@JoinColumn(name = "CONFIGURATION_TYPE")
	private ProgressConfigurationType configType;

	@Column(name = "NAME")
	private String name;

	@Column(name = "CONFIG_WIDE_FLAG")
	private boolean configWideAttribute;
	
	@Column(name = "DEFAULT_VALUE")
	private String defaultValue;

	@OneToMany(cascade = CascadeType.ALL, mappedBy = "attribute", orphanRemoval = true)
	private List<ProgressConfigurationAttributeValue> configAttributeValues;

	@OneToMany(cascade = CascadeType.ALL, mappedBy = "attribute", orphanRemoval = true)
	private List<ProgressAttributeValue> itemAttributeValues;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public ProgressAttributeType getAttributeType() {
		return attributeType;
	}

	public void setAttributeType(ProgressAttributeType attributeType) {
		this.attributeType = attributeType;
	}

	public ProgressConfigurationType getConfigType() {
		return configType;
	}

	public void setConfigType(ProgressConfigurationType configType) {
		this.configType = configType;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public boolean isConfigWideAttribute() {
		return configWideAttribute;
	}

	public void setConfigWideAttribute(boolean configWideAttribute) {
		this.configWideAttribute = configWideAttribute;
	}

	public List<ProgressConfigurationAttributeValue> getConfigAttributeValues() {
		return configAttributeValues;
	}

	public void setConfigAttributeValues(List<ProgressConfigurationAttributeValue> configAttributeValues) {
		this.configAttributeValues = configAttributeValues;
	}

	public List<ProgressAttributeValue> getItemAttributeValues() {
		return itemAttributeValues;
	}

	public void setItemAttributeValues(List<ProgressAttributeValue> itemAttributeValues) {
		this.itemAttributeValues = itemAttributeValues;
	}

	public String getDefaultValue() {
		return defaultValue;
	}

	public void setDefaultValue(String defaultValue) {
		this.defaultValue = defaultValue;
	}
	
	/*
	 * Return the name of the type of attribute (String, int, long, etc)
	 */
	public String getAttributeTypeName() {
		return attributeType.getName();
	}


}
