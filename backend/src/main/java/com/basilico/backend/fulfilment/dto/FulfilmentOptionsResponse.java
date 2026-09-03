package com.basilico.backend.fulfilment.dto;

import java.math.BigDecimal;

import com.basilico.backend.fulfilment.entity.DeliveryAreaMode;
import com.basilico.backend.fulfilment.entity.DeliveryPricingMode;

public record FulfilmentOptionsResponse(
		boolean collectionEnabled,
		boolean deliveryEnabled,
		Integer minimumDeliveryOrderPence,
		Integer deliveryFeePence,
		Integer freeDeliveryThresholdPence,
		DeliveryAreaMode deliveryAreaMode,
		String restaurantPostcode,
		BigDecimal deliveryRadiusMiles,
		Integer preparationTimeMinutes,
		DeliveryPricingMode deliveryPricingMode,
		BigDecimal baseDeliveryRadiusMiles,
		Integer baseDeliveryFeePence,
		Integer extraMileFeePence,
		OrderAvailabilityResponse orderAvailability
) {
}
