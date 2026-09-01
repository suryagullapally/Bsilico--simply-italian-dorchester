package com.basilico.backend.payment.dto;

import java.time.OffsetDateTime;

import com.basilico.backend.payment.entity.PaymentAttemptStatus;
import com.basilico.backend.payment.entity.PaymentProvider;

public record PaymentAttemptResponse(
		Long id,
		PaymentProvider provider,
		PaymentAttemptStatus status,
		int amountPence,
		String currency,
		String stripeCheckoutSessionId,
		String stripePaymentIntentId,
		OffsetDateTime createdAt,
		OffsetDateTime updatedAt,
		OffsetDateTime completedAt
) {
}
