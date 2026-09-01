package com.basilico.backend.order.dto;

import jakarta.validation.constraints.NotNull;

import com.basilico.backend.order.entity.PaymentStatus;

public record PaymentStatusUpdateRequest(
		@NotNull PaymentStatus paymentStatus
) {
}
