package com.basilico.backend.order.entity;

public enum OrderStatus {
	PENDING_PAYMENT,
	NEW,
	ACCEPTED,
	PREPARING,
	READY,
	COMPLETED,
	CANCELLED
}
