package com.basilico.backend.menu.dto;

import java.util.List;

import com.basilico.backend.menu.entity.DietaryTag;
import com.basilico.backend.menu.entity.ProductType;

public record MenuItemResponse(
		Long id,
		String slug,
		String name,
		String description,
		int pricePence,
		String imagePath,
		ProductType productType,
		boolean available,
		boolean active,
		boolean featured,
		boolean customizable,
		int displayOrder,
		String categorySlug,
		String categoryName,
		List<DietaryTag> dietaryTags
) {
}
