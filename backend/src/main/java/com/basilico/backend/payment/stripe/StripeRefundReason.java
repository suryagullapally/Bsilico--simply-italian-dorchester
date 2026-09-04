package com.basilico.backend.payment.stripe;

public enum StripeRefundReason {
	REQUESTED_BY_CUSTOMER,
	DUPLICATE,
	FRAUDULENT
}
