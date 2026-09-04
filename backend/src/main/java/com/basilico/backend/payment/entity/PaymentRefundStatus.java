package com.basilico.backend.payment.entity;

public enum PaymentRefundStatus {
	CREATED,
	PENDING,
	SUCCEEDED,
	FAILED,
	CANCELED,
	REQUIRES_ACTION
}
