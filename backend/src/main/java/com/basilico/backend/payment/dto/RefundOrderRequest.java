package com.basilico.backend.payment.dto;

import com.basilico.backend.payment.entity.PaymentRefundReason;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RefundOrderRequest(
		@NotNull PaymentRefundReason reason,
		@Size(max = 2000) String note
) {
}
