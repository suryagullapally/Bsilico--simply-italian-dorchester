package com.basilico.backend.payment.stripe;

import java.util.List;
import java.util.Map;

public record StripeCheckoutSessionCreateCommand(
		String customerEmail,
		String returnUrl,
		List<StripeLineItem> lineItems,
		Map<String, String> metadata,
		String idempotencyKey
) {
}
