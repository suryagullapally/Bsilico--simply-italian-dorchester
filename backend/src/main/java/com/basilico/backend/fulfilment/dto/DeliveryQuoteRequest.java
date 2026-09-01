package com.basilico.backend.fulfilment.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

public record DeliveryQuoteRequest(
		@Size(max = 20) String postcode,
		@DecimalMin("-90.0") @DecimalMax("90.0") Double latitude,
		@DecimalMin("-180.0") @DecimalMax("180.0") Double longitude
) {
}
