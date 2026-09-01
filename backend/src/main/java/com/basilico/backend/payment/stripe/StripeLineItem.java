package com.basilico.backend.payment.stripe;

public record StripeLineItem(
		String name,
		String description,
		int unitAmountPence,
		int quantity
) {
}
