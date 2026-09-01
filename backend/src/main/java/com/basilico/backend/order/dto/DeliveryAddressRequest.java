package com.basilico.backend.order.dto;

import jakarta.validation.constraints.Size;

public record DeliveryAddressRequest(
		@Size(max = 220) String line1,
		@Size(max = 220) String line2,
		@Size(max = 120) String city,
		@Size(max = 20) String postcode
) {
}
