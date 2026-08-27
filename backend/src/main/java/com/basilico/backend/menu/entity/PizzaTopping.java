package com.basilico.backend.menu.entity;

import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "pizza_toppings")
public class PizzaTopping {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "customizer_id", nullable = false)
	private MenuCustomizer customizer;

	@Column(nullable = false, length = 180)
	private String name;

	@Column(name = "price_override_pence")
	private Integer priceOverridePence;

	@Column(nullable = false)
	private boolean available = true;

	@Column(name = "display_order", nullable = false)
	private int displayOrder;

	@Column(name = "created_at", insertable = false, updatable = false)
	private OffsetDateTime createdAt;

	@Column(name = "updated_at", insertable = false, updatable = false)
	private OffsetDateTime updatedAt;

	protected PizzaTopping() {
	}

	public Long getId() {
		return id;
	}

	public MenuCustomizer getCustomizer() {
		return customizer;
	}

	public String getName() {
		return name;
	}

	public Integer getPriceOverridePence() {
		return priceOverridePence;
	}

	public boolean isAvailable() {
		return available;
	}

	public int getDisplayOrder() {
		return displayOrder;
	}

	public OffsetDateTime getCreatedAt() {
		return createdAt;
	}

	public OffsetDateTime getUpdatedAt() {
		return updatedAt;
	}
}
