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

import org.sakaiproject.progress.api.ProgressServiceException;

@Entity
@Table(name = "PRG_PROGRESS_ITEM")
public class ProgressItem {

	@Id
	@Column(name = "ID")
	@GeneratedValue(strategy = GenerationType.AUTO)
	private int id;

	@ManyToOne
	@JoinColumn(name = "SITE_CONFIG")
	private ProgressSiteConfiguration siteConfig;

	@Column(name = "NAME")
	private String name;

	@Column(name = "ACTIVE")
	private boolean isActive;

	@OneToMany(cascade = CascadeType.ALL, mappedBy = "item", orphanRemoval = true)
	private List<ProgressAttributeValue> values;
	
	public ProgressItem() {
		//default ctor
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public ProgressSiteConfiguration getSiteConfig() {
		return siteConfig;
	}

	public void setSiteConfig(ProgressSiteConfiguration siteConfig) {
		this.siteConfig = siteConfig;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public boolean isActive() {
		return isActive;
	}

	public void setActive(boolean isActive) {
		this.isActive = isActive;
	}

	public List<ProgressAttributeValue> getValues() {
		return values;
	}

	public void setValues(List<ProgressAttributeValue> values) {
		this.values = values;
	}
	
	/*
	 * Returns the value of an attribute when given the name of that attribute. 
	 * Returns null if that attribute does not exist. 
	 */
	public String getAttributeValueByName(String attributeName) {
		for(ProgressAttributeValue value : values) {
			if (value.getAttributeName().equalsIgnoreCase(attributeName)) {
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
		for (ProgressAttributeValue attributeValue : values) {
			if (attributeValue.getAttributeName().equalsIgnoreCase(attributeName)) {
				attributeValue.setValue(newValue);
				found = true;
			}
		}
		if (!found) {
			throw new ProgressServiceException("Cannot find Progress Attribute on " + name + " with name " + attributeName);
		}
	}

}
