package com.basilico.backend.fulfilment.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import com.basilico.backend.fulfilment.entity.DeliveryAreaMode;
import com.basilico.backend.fulfilment.entity.DeliveryPricingMode;

public record AdminFulfilmentSettingsResponse(
		Long id,
		boolean collectionEnabled,
		boolean deliveryEnabled,
		Integer minimumDeliveryOrderPence,
		Integer deliveryFeePence,
		Integer freeDeliveryThresholdPence,
		DeliveryAreaMode deliveryAreaMode,
		String restaurantPostcode,
		BigDecimal restaurantLatitude,
		BigDecimal restaurantLongitude,
		BigDecimal deliveryRadiusMiles,
		Integer preparationTimeMinutes,
		DeliveryPricingMode deliveryPricingMode,
		BigDecimal baseDeliveryRadiusMiles,
		Integer baseDeliveryFeePence,
		Integer extraMileFeePence,
		boolean drivingTimeConfigured,
		OffsetDateTime createdAt,
		OffsetDateTime updatedAt
) {
}
