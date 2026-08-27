package com.basilico.backend.menu.dto;

import java.util.List;

public record MenuResponse(List<MenuCategoryResponse> categories, List<MenuCustomizerResponse> customizers) {
}
