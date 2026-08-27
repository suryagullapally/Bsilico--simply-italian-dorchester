package com.basilico.backend.menu.entity;

import java.time.OffsetDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "menu_items")
public class MenuItem {

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

	@Column(columnDefinition = "text")
	private String description;

	@Column(name = "price_pence", nullable = false)
	private int pricePence;

	@Column(name = "image_path", length = 500)
	private String imagePath;

	@Enumerated(EnumType.STRING)
	@Column(name = "product_type", nullable = false, length = 40)
	private ProductType productType = ProductType.STANDARD;

	@Column(nullable = false)
	private boolean available = true;

	@Column(nullable = false)
	private boolean active = true;

	@Column(nullable = false)
	private boolean featured;

	@Column(nullable = false)
	private boolean customizable;

	@Column(name = "display_order", nullable = false)
	private int displayOrder;

	@ElementCollection(fetch = FetchType.LAZY)
	@CollectionTable(name = "menu_item_dietary_tags", joinColumns = @JoinColumn(name = "menu_item_id"))
	@Enumerated(EnumType.STRING)
	@Column(name = "tag", nullable = false)
	private Set<DietaryTag> dietaryTags = new LinkedHashSet<>();

	@Column(name = "created_at", insertable = false, updatable = false)
	private OffsetDateTime createdAt;

	@Column(name = "updated_at", insertable = false, updatable = false)
	private OffsetDateTime updatedAt;

	protected MenuItem() {
	}

	public MenuItem(MenuCategory category, String slug, String name, int pricePence, ProductType productType,
			int displayOrder) {
		this.category = category;
		this.slug = slug;
		this.name = name;
		this.pricePence = pricePence;
		this.productType = productType;
		this.displayOrder = displayOrder;
	}

	public Long getId() {
		return id;
	}

	public MenuCategory getCategory() {
		return category;
	}

	public void setCategory(MenuCategory category) {
		this.category = category;
	}

	public String getSlug() {
		return slug;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public int getPricePence() {
		return pricePence;
	}

	public void setPricePence(int pricePence) {
		this.pricePence = pricePence;
	}

	public String getImagePath() {
		return imagePath;
	}

	public void setImagePath(String imagePath) {
		this.imagePath = imagePath;
	}

	public ProductType getProductType() {
		return productType;
	}

	public void setProductType(ProductType productType) {
		this.productType = productType;
	}

	public boolean isAvailable() {
		return available;
	}

	public void setAvailable(boolean available) {
		this.available = available;
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	public boolean isFeatured() {
		return featured;
	}

	public void setFeatured(boolean featured) {
		this.featured = featured;
	}

	public boolean isCustomizable() {
		return customizable;
	}

	public void setCustomizable(boolean customizable) {
		this.customizable = customizable;
	}

	public int getDisplayOrder() {
		return displayOrder;
	}

	public void setDisplayOrder(int displayOrder) {
		this.displayOrder = displayOrder;
	}

	public Set<DietaryTag> getDietaryTags() {
		return dietaryTags;
	}

	public void setDietaryTags(Set<DietaryTag> dietaryTags) {
		this.dietaryTags.clear();
		this.dietaryTags.addAll(dietaryTags);
	}

	public OffsetDateTime getCreatedAt() {
		return createdAt;
	}

	public OffsetDateTime getUpdatedAt() {
		return updatedAt;
	}
}
