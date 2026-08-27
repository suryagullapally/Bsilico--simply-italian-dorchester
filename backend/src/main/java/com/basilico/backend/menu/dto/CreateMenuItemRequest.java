package com.basilico.backend.menu.dto;

import java.util.Set;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import com.basilico.backend.menu.entity.DietaryTag;
import com.basilico.backend.menu.entity.ProductType;

public record CreateMenuItemRequest(
		@NotNull Long categoryId,
		@NotBlank String slug,
		@NotBlank String name,
		String description,
		@NotNull @PositiveOrZero Integer pricePence,
		String imagePath,
		@NotNull ProductType productType,
		@NotNull Boolean available,
		@NotNull Boolean active,
		@NotNull Boolean featured,
		@NotNull Boolean customizable,
		@NotNull Integer displayOrder,
		@NotNull Set<DietaryTag> dietaryTags
) {
}
