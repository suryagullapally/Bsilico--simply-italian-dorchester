package com.basilico.backend.fulfilment.dto;

public record DeliveryEligibilityResponse(
		boolean eligible,
		String normalizedPostcode,
		String message
) {
}
