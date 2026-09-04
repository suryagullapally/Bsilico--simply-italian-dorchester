package com.basilico.backend.payment.dto;

import java.time.OffsetDateTime;

import com.basilico.backend.payment.entity.PaymentRefundReason;
import com.basilico.backend.payment.entity.PaymentRefundStatus;
import com.basilico.backend.payment.entity.PaymentProvider;

public record PaymentRefundResponse(
		Long id,
		PaymentProvider provider,
		PaymentRefundStatus status,
		int amountPence,
		String currency,
		PaymentRefundReason reason,
		String note,
		String failureReason,
		OffsetDateTime createdAt,
		OffsetDateTime updatedAt,
		OffsetDateTime completedAt
) {
}
