package com.basilico.backend.fulfilment.dto;

public record DeliveryQuoteResponse(
		boolean eligible,
		String source,
		String normalizedPostcode,
		Double distanceMiles,
		Integer deliveryFeePence,
		Integer preparationMinutes,
		Integer travelMinutes,
		Integer estimatedDeliveryMinutes,
		String message
) {
}
