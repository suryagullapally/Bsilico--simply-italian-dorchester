package com.basilico.backend.payment.stripe;

public record StripeWebhookEventData(
		String id,
		String type,
		StripeCheckoutSessionData session
) {
}
