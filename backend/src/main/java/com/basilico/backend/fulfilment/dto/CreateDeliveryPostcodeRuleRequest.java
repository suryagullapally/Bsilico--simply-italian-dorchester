package com.basilico.backend.fulfilment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateDeliveryPostcodeRuleRequest(
		@NotBlank @Size(max = 20) String postcodePattern,
		Integer displayOrder
) {
}
