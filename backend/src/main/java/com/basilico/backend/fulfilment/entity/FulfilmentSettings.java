package com.basilico.backend.fulfilment.entity;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "fulfilment_settings")
public class FulfilmentSettings {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "collection_enabled", nullable = false)
	private boolean collectionEnabled = true;

	@Column(name = "delivery_enabled", nullable = false)
	private boolean deliveryEnabled;

	@Column(name = "minimum_delivery_order_pence")
	private Integer minimumDeliveryOrderPence;

	@Column(name = "delivery_fee_pence")
	private Integer deliveryFeePence;

	@Column(name = "free_delivery_threshold_pence")
	private Integer freeDeliveryThresholdPence;

	@Enumerated(EnumType.STRING)
	@Column(name = "delivery_area_mode", nullable = false, length = 30)
	private DeliveryAreaMode deliveryAreaMode = DeliveryAreaMode.POSTCODE_RULES;

	@Column(name = "restaurant_postcode", length = 20)
	private String restaurantPostcode;

	@Column(name = "restaurant_latitude", precision = 9, scale = 6)
	private BigDecimal restaurantLatitude;

	@Column(name = "restaurant_longitude", precision = 9, scale = 6)
	private BigDecimal restaurantLongitude;

	@Column(name = "delivery_radius_miles", precision = 5, scale = 2)
	private BigDecimal deliveryRadiusMiles;

	@Column(name = "preparation_time_minutes")
	private Integer preparationTimeMinutes;

	@Enumerated(EnumType.STRING)
	@Column(name = "delivery_pricing_mode", nullable = false, length = 30)
	private DeliveryPricingMode deliveryPricingMode = DeliveryPricingMode.FLAT_FEE;

	@Column(name = "base_delivery_radius_miles", precision = 5, scale = 2)
	private BigDecimal baseDeliveryRadiusMiles;

	@Column(name = "base_delivery_fee_pence")
	private Integer baseDeliveryFeePence;

	@Column(name = "extra_mile_fee_pence")
	private Integer extraMileFeePence;

	@Column(name = "created_at", insertable = false, updatable = false)
	private OffsetDateTime createdAt;

	@Column(name = "updated_at", insertable = false, updatable = false)
	private OffsetDateTime updatedAt;

	protected FulfilmentSettings() {
	}

	public Long getId() {
		return id;
	}

	public boolean isCollectionEnabled() {
		return collectionEnabled;
	}

	public void setCollectionEnabled(boolean collectionEnabled) {
		this.collectionEnabled = collectionEnabled;
	}

	public boolean isDeliveryEnabled() {
		return deliveryEnabled;
	}

	public void setDeliveryEnabled(boolean deliveryEnabled) {
		this.deliveryEnabled = deliveryEnabled;
	}

	public Integer getMinimumDeliveryOrderPence() {
		return minimumDeliveryOrderPence;
	}

	public void setMinimumDeliveryOrderPence(Integer minimumDeliveryOrderPence) {
		this.minimumDeliveryOrderPence = minimumDeliveryOrderPence;
	}

	public Integer getDeliveryFeePence() {
		return deliveryFeePence;
	}

	public void setDeliveryFeePence(Integer deliveryFeePence) {
		this.deliveryFeePence = deliveryFeePence;
	}

	public Integer getFreeDeliveryThresholdPence() {
		return freeDeliveryThresholdPence;
	}

	public void setFreeDeliveryThresholdPence(Integer freeDeliveryThresholdPence) {
		this.freeDeliveryThresholdPence = freeDeliveryThresholdPence;
	}

	public DeliveryAreaMode getDeliveryAreaMode() {
		return deliveryAreaMode;
	}

	public void setDeliveryAreaMode(DeliveryAreaMode deliveryAreaMode) {
		this.deliveryAreaMode = deliveryAreaMode;
	}

	public String getRestaurantPostcode() {
		return restaurantPostcode;
	}

	public void setRestaurantPostcode(String restaurantPostcode) {
		this.restaurantPostcode = restaurantPostcode;
	}

	public BigDecimal getRestaurantLatitude() {
		return restaurantLatitude;
	}

	public void setRestaurantLatitude(BigDecimal restaurantLatitude) {
		this.restaurantLatitude = restaurantLatitude;
	}

	public BigDecimal getRestaurantLongitude() {
		return restaurantLongitude;
	}

	public void setRestaurantLongitude(BigDecimal restaurantLongitude) {
		this.restaurantLongitude = restaurantLongitude;
	}

	public BigDecimal getDeliveryRadiusMiles() {
		return deliveryRadiusMiles;
	}

	public void setDeliveryRadiusMiles(BigDecimal deliveryRadiusMiles) {
		this.deliveryRadiusMiles = deliveryRadiusMiles;
	}

	public Integer getPreparationTimeMinutes() {
		return preparationTimeMinutes;
	}

	public void setPreparationTimeMinutes(Integer preparationTimeMinutes) {
		this.preparationTimeMinutes = preparationTimeMinutes;
	}

	public DeliveryPricingMode getDeliveryPricingMode() {
		return deliveryPricingMode;
	}

	public void setDeliveryPricingMode(DeliveryPricingMode deliveryPricingMode) {
		this.deliveryPricingMode = deliveryPricingMode;
	}

	public BigDecimal getBaseDeliveryRadiusMiles() {
		return baseDeliveryRadiusMiles;
	}

	public void setBaseDeliveryRadiusMiles(BigDecimal baseDeliveryRadiusMiles) {
		this.baseDeliveryRadiusMiles = baseDeliveryRadiusMiles;
	}

	public Integer getBaseDeliveryFeePence() {
		return baseDeliveryFeePence;
	}

	public void setBaseDeliveryFeePence(Integer baseDeliveryFeePence) {
		this.baseDeliveryFeePence = baseDeliveryFeePence;
	}

	public Integer getExtraMileFeePence() {
		return extraMileFeePence;
	}

	public void setExtraMileFeePence(Integer extraMileFeePence) {
		this.extraMileFeePence = extraMileFeePence;
	}

	public OffsetDateTime getCreatedAt() {
		return createdAt;
	}

	public OffsetDateTime getUpdatedAt() {
		return updatedAt;
	}
}
