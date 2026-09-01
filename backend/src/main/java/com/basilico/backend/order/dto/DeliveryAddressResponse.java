package com.basilico.backend.order.dto;

public record DeliveryAddressResponse(
		String line1,
		String line2,
		String city,
		String postcode
) {
}
