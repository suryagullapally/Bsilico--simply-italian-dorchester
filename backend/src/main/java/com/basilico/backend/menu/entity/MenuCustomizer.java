package com.basilico.backend.menu.entity;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

@Entity
@Table(name = "menu_customizers")
public class MenuCustomizer {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "category_id", nullable = false)
	private MenuCategory category;

	@Column(nullable = false, unique = true, length = 160)
	private String slug;

	@Column(nullable = false, length = 220)
	private String name;

	@Column(name = "base_price_pence", nullable = false)
	private int basePricePence;

	@Column(name = "extra_topping_price_pence", nullable = false)
	private int extraToppingPricePence;

	@Column(nullable = false)
	private boolean active = true;

	@Column(name = "display_order", nullable = false)
	private int displayOrder;

	@OneToMany(mappedBy = "customizer", fetch = FetchType.LAZY)
	@OrderBy("displayOrder ASC")
	private List<PizzaTopping> toppings = new ArrayList<>();

	@Column(name = "created_at", insertable = false, updatable = false)
	private OffsetDateTime createdAt;

	@Column(name = "updated_at", insertable = false, updatable = false)
	private OffsetDateTime updatedAt;

	protected MenuCustomizer() {
	}

	public Long getId() {
		return id;
	}

	public MenuCategory getCategory() {
		return category;
	}

	public String getSlug() {
		return slug;
	}

	public String getName() {
		return name;
	}

	public int getBasePricePence() {
		return basePricePence;
	}

	public int getExtraToppingPricePence() {
		return extraToppingPricePence;
	}

	public boolean isActive() {
		return active;
	}

	public int getDisplayOrder() {
		return displayOrder;
	}

	public List<PizzaTopping> getToppings() {
		return toppings;
	}

	public OffsetDateTime getCreatedAt() {
		return createdAt;
	}

	public OffsetDateTime getUpdatedAt() {
		return updatedAt;
	}
}
