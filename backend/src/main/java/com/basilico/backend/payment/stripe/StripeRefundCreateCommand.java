package com.basilico.backend.payment.stripe;

import java.util.Map;

public record StripeRefundCreateCommand(
		String paymentIntentId,
		int amountPence,
		String currency,
		StripeRefundReason reason,
		Map<String, String> metadata,
		String idempotencyKey
) {
}
