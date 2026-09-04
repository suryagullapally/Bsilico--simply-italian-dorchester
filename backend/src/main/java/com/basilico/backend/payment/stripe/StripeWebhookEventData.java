package com.basilico.backend.payment.stripe;

public record StripeWebhookEventData(
		String id,
		String type,
		StripeCheckoutSessionData session,
		StripeRefundData refund
) {
	public StripeWebhookEventData(String id, String type, StripeCheckoutSessionData session) {
		this(id, type, session, null);
	}
}
