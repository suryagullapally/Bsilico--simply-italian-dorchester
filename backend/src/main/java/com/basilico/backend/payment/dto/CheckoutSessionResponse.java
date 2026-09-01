package com.basilico.backend.payment.dto;

public record CheckoutSessionResponse(
		String orderReference,
		String checkoutSessionClientSecret,
		String stripeCheckoutSessionId,
		int amountPence,
		String currency
) {
}
