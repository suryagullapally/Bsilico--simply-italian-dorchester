package com.basilico.backend.payment.stripe;

public interface StripeCheckoutGateway {

	StripeCheckoutSessionData createCheckoutSession(StripeCheckoutSessionCreateCommand command);

	StripeCheckoutSessionData retrieveCheckoutSession(String sessionId);

	StripeWebhookEventData constructWebhookEvent(String payload, String signatureHeader);
}
