package org.sakaiproject.progress.model.data.entity;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.*;

@Entity
@Table(name = "PRG_CONFIG_TYPE")
public class ProgressConfigurationType {

	@Id
	@Column(name = "ID")
	@GeneratedValue(strategy = GenerationType.AUTO)
	private int id;

	@Column(name = "TYPE", unique = true)
	private String type;

	@Column(name = "IMPLEMENTATION_CLASS")
	private String implClassName;

	@OneToMany(cascade = CascadeType.ALL, mappedBy = "configType", orphanRemoval = true)
	private List<ProgressSiteConfiguration> siteConfigs = new ArrayList<>();

	@OneToMany(cascade = CascadeType.ALL, mappedBy = "configType", orphanRemoval = true)
	private List<ProgressConfigurationAttribute> attributes = new ArrayList<>();

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getImplClassName() {
		return implClassName;
	}

	public void setImplClassName(String implClassName) {
		this.implClassName = implClassName;
	}

	public List<ProgressSiteConfiguration> getSiteConfigs() {
		return siteConfigs;
	}

	public void setSiteConfigs(List<ProgressSiteConfiguration> siteConfigs) {
		this.siteConfigs = siteConfigs;
	}

	public List<ProgressConfigurationAttribute> getAttributes() {
		return attributes;
	}

	public void setAttributes(List<ProgressConfigurationAttribute> attributes) {
		this.attributes = attributes;
	}

}
