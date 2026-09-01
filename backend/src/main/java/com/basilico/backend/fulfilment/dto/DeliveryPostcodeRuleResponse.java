package com.basilico.backend.fulfilment.dto;

import java.time.OffsetDateTime;

public record DeliveryPostcodeRuleResponse(
		Long id,
		String postcodePattern,
		boolean active,
		int displayOrder,
		OffsetDateTime createdAt,
		OffsetDateTime updatedAt
) {
}
