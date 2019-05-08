package org.sakaiproject.progress.model.data.entity;

import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.sakaiproject.progress.api.ProgressServiceException;

@Entity
@Table(name = "PRG_SITE_CONFIG",
		uniqueConstraints = {@UniqueConstraint(columnNames={"SITE_ID", "CONFIG_TYPE_ID"})}
		)
public class ProgressSiteConfiguration {

	@Id
	@Column(name = "ID")
	@GeneratedValue(strategy = GenerationType.AUTO)
	private int id;

	@Column(name = "SITE_ID")
	private String siteId;
	
	@Column(name = "IS_ACTIVE")
	private boolean isActive;
	
	@Column(name = "WEIGHT")
	private double weight;

	@ManyToOne
	@JoinColumn(name = "CONFIG_TYPE_ID")
	private ProgressConfigurationType configType;

	@OneToMany(cascade = CascadeType.ALL, mappedBy = "siteConfig", orphanRemoval=true)
	private List<ProgressItem> progressItems;

	@OneToMany(cascade = CascadeType.ALL, mappedBy = "siteConfig", orphanRemoval=true)
	private List<ProgressConfigurationAttributeValue> configValues;
	
	public ProgressSiteConfiguration() {
		//default ctor
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getSiteId() {
		return siteId;
	}

	public void setSiteId(String siteId) {
		this.siteId = siteId;
	}

	public ProgressConfigurationType getConfigType() {
		return configType;
	}

	public void setConfigType(ProgressConfigurationType configType) {
		this.configType = configType;
	}

	public List<ProgressItem> getProgressItems() {
		return progressItems;
	}

	public void setProgressItems(List<ProgressItem> progressItems) {
		this.progressItems = progressItems;
	}

	public List<ProgressConfigurationAttributeValue> getConfigValues() {
		return configValues;
	}

	public void setConfigValues(List<ProgressConfigurationAttributeValue> configValues) {
		this.configValues = configValues;
	}

	public boolean isActive() {
		return isActive;
	}

	public void setActive(boolean isActive) {
		this.isActive = isActive;
	}

	public double getWeight() {
		return weight;
	}

	public void setWeight(double weight) {
		this.weight = weight;
	}
	
	public String getConfigTypeName() {
		return configType.getType();
	}
	
	/*
	 * Returns the value of an attribute when given the name of that attribute. 
	 * Returns null if that attribute does not exist. 
	 */
	public String getAttributeValueByName(String attributeName) {
		for(ProgressConfigurationAttributeValue value : configValues) {
			if (value.getValueName().equalsIgnoreCase(attributeName)) {
				return value.getValue();
			}
		}
		return null;
	}
	
	/*
	 * Sets the attribute with the provided name to the provided value.
	 * Throws ProgressServiceException if that attribute cannot be found. 
	 */
	public void setAttributeValueByName(String attributeName, String newValue) {
		boolean found = false;
		for (ProgressConfigurationAttributeValue attributeValue : configValues) {
			if (attributeValue.getValueName().equalsIgnoreCase(attributeName)) {
				attributeValue.setValue(newValue);
				found = true;
			}
		}
		if (!found) {
			throw new ProgressServiceException("Cannot find Progress Attribute on with name " + attributeName);
		}
	}

}
