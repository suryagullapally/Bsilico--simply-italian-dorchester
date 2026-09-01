package com.basilico.backend.fulfilment.service;

import java.math.BigDecimal;

public record FulfilmentPricing(
		int deliveryFeePence,
		int totalPence,
		String normalizedPostcode,
		BigDecimal deliveryDistanceMiles,
		Integer preparationMinutes,
		Integer travelMinutes,
		Integer estimatedDeliveryMinutes
) {

	public static FulfilmentPricing collection(int subtotalPence) {
		return new FulfilmentPricing(0, subtotalPence, null, null, null, null, null);
	}
}
