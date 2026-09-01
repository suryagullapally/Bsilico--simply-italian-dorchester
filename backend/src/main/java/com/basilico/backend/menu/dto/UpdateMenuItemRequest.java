package com.basilico.backend.menu.dto;

import java.util.Set;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import com.basilico.backend.menu.entity.DietaryTag;
import com.basilico.backend.menu.entity.ProductType;

public record UpdateMenuItemRequest(
		@NotNull Long categoryId,
		@NotBlank @Size(max = 220) String name,
		@Size(max = 2000) String description,
		@NotNull @PositiveOrZero Integer pricePence,
		@Size(max = 500) String imagePath,
		@NotNull ProductType productType,
		@NotNull Boolean available,
		@NotNull Boolean active,
		@NotNull Boolean featured,
		@NotNull Boolean customizable,
		@NotNull Integer displayOrder,
		@NotNull Set<DietaryTag> dietaryTags
) {
}
