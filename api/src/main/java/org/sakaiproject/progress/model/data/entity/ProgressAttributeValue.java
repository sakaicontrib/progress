package org.sakaiproject.progress.model.data.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name = "PRG_ATTRIBUTE_VALUE")
public class ProgressAttributeValue {

	@Id
	@Column(name = "ID")
	@GeneratedValue(strategy = GenerationType.AUTO)
	private int id;

	@ManyToOne
	@JoinColumn(name = "ATTRIBUTE_ID")
	private ProgressConfigurationAttribute attribute;

	@ManyToOne
	@JoinColumn(name = "PROGRESS_ITEM_ID")
	private ProgressItem item;

	@Column(name = "VALUE")
	private String value;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public ProgressConfigurationAttribute getAttribute() {
		return attribute;
	}

	public void setAttribute(ProgressConfigurationAttribute attribute) {
		this.attribute = attribute;
	}

	public ProgressItem getItem() {
		return item;
	}

	public void setItem(ProgressItem item) {
		this.item = item;
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
	public String getAttributeName() {
		return attribute.getName();
	}
	
	/*
	 * Return the type of the attribute (i.e. String, int, double..)
	 */
	public String getAttributeType() {
		return attribute.getAttributeTypeName();
	}

}
