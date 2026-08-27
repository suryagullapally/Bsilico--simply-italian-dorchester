package com.basilico.backend.menu.dto;

import jakarta.validation.constraints.NotNull;

public record FeaturedUpdateRequest(@NotNull Boolean featured) {
}
