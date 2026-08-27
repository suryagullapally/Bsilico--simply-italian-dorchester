package com.basilico.backend.menu.dto;

import java.util.List;

public record MenuCustomizerResponse(
		Long id,
		String slug,
		String name,
		int basePricePence,
		int extraToppingPricePence,
		boolean active,
		int displayOrder,
		String categorySlug,
		String categoryName,
		List<PizzaToppingResponse> toppings
) {
}
