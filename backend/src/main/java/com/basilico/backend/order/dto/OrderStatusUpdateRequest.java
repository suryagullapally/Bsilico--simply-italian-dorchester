package com.basilico.backend.order.dto;

import jakarta.validation.constraints.NotNull;

import com.basilico.backend.order.entity.OrderStatus;

public record OrderStatusUpdateRequest(
		@NotNull OrderStatus status
) {
}
