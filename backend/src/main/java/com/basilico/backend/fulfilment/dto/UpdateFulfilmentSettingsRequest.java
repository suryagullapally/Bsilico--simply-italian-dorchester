package com.basilico.backend.fulfilment.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import com.basilico.backend.fulfilment.entity.DeliveryAreaMode;
import com.basilico.backend.fulfilment.entity.DeliveryPricingMode;

public record UpdateFulfilmentSettingsRequest(
		@NotNull Boolean collectionEnabled,
		@NotNull Boolean deliveryEnabled,
		@PositiveOrZero Integer minimumDeliveryOrderPence,
		@PositiveOrZero Integer deliveryFeePence,
		@PositiveOrZero Integer freeDeliveryThresholdPence,
		@NotNull DeliveryAreaMode deliveryAreaMode,
		@Size(max = 20) String restaurantPostcode,
		@DecimalMin("-90.0") @DecimalMax("90.0") BigDecimal restaurantLatitude,
		@DecimalMin("-180.0") @DecimalMax("180.0") BigDecimal restaurantLongitude,
		@DecimalMin(value = "0.0", inclusive = false) BigDecimal deliveryRadiusMiles,
		@PositiveOrZero Integer preparationTimeMinutes,
		@NotNull DeliveryPricingMode deliveryPricingMode,
		@DecimalMin("0.0") BigDecimal baseDeliveryRadiusMiles,
		@PositiveOrZero Integer baseDeliveryFeePence,
		@PositiveOrZero Integer extraMileFeePence
) {
}
