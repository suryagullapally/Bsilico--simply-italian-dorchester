package com.basilico.backend.order.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.basilico.backend.order.entity.OrderItemType;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CreateOrderItemRequest(
		@NotNull OrderItemType type,
		Long menuItemId,
		Long customizerId,
		@Size(max = 40) List<Long> toppingIds,
		@NotNull @Min(1) @Max(99) Integer quantity
) {
}
