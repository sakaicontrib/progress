package org.sakaiproject.progress.model.data.entity;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name = "PRG_CONFIG_ATTRIBUTE_VALUE")
public class ProgressConfigurationAttributeValue {

	@Id
	@Column(name = "ID")
	@GeneratedValue(strategy = GenerationType.AUTO)
	private int id;

	@ManyToOne
	@JoinColumn(name = "SITE_CONFIG_ID")
	private ProgressSiteConfiguration siteConfig;

	@ManyToOne
	@JoinColumn(name = "ATTRIBUTE_ID")
	private ProgressConfigurationAttribute attribute;

	@Column(name = "VALUE")
	private String value;

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

	public ProgressConfigurationAttribute getAttribute() {
		return attribute;
	}

	public void setAttribute(ProgressConfigurationAttribute attribute) {
		this.attribute = attribute;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}
	
	/*
	 * Return the name of the attribute that this value is for. 
	 */
	public String getValueName() {
		return attribute.getName();
	}
	
	/*
	 * Return the type of the attribute (i.e. String, int, double..)
	 */
	public String getAttributeType() {
		return attribute.getAttributeTypeName();
	}

}
