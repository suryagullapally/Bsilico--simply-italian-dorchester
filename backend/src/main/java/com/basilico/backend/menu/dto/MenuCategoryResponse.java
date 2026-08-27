package com.basilico.backend.menu.dto;

import java.util.List;

public record MenuCategoryResponse(
		Long id,
		String slug,
		String name,
		int displayOrder,
		List<MenuItemResponse> items
) {
}
