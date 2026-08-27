package com.basilico.backend.menu.dto;

public record PizzaToppingResponse(
		Long id,
		String name,
		Integer priceOverridePence,
		boolean available,
		int displayOrder
) {
}
