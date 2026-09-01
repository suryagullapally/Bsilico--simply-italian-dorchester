package com.basilico.backend.payment.stripe;

public record StripeCheckoutSessionData(
		String id,
		String clientSecret,
		String status,
		String paymentStatus,
		Integer amountTotal,
		String currency,
		String paymentIntentId,
		String orderReference
) {
}
