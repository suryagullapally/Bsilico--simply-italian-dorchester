package com.basilico.backend.fulfilment.entity;

import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "delivery_postcode_rules")
public class DeliveryPostcodeRule {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "postcode_pattern", nullable = false, unique = true, length = 20)
	private String postcodePattern;

	@Column(nullable = false)
	private boolean active = true;

	@Column(name = "display_order", nullable = false)
	private int displayOrder;

	@Column(name = "created_at", insertable = false, updatable = false)
	private OffsetDateTime createdAt;

	@Column(name = "updated_at", insertable = false, updatable = false)
	private OffsetDateTime updatedAt;

	protected DeliveryPostcodeRule() {
	}

	public DeliveryPostcodeRule(String postcodePattern, int displayOrder) {
		this.postcodePattern = postcodePattern;
		this.displayOrder = displayOrder;
	}

	public Long getId() {
		return id;
	}

	public String getPostcodePattern() {
		return postcodePattern;
	}

	public void setPostcodePattern(String postcodePattern) {
		this.postcodePattern = postcodePattern;
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	public int getDisplayOrder() {
		return displayOrder;
	}

	public void setDisplayOrder(int displayOrder) {
		this.displayOrder = displayOrder;
	}

	public OffsetDateTime getCreatedAt() {
		return createdAt;
	}

	public OffsetDateTime getUpdatedAt() {
		return updatedAt;
	}
}
