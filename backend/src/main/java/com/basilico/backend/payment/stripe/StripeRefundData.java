package com.basilico.backend.payment.stripe;

import java.util.Map;

public record StripeRefundData(
		String id,
		String status,
		Integer amountPence,
		String currency,
		String paymentIntentId,
		String reason,
		String failureReason,
		Map<String, String> metadata
) {
}
