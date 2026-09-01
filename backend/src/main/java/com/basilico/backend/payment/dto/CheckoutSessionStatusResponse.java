package com.basilico.backend.payment.dto;

import com.basilico.backend.order.entity.OrderStatus;
import com.basilico.backend.order.entity.PaymentStatus;
import com.basilico.backend.payment.entity.PaymentAttemptStatus;

public record CheckoutSessionStatusResponse(
		String orderReference,
		OrderStatus orderStatus,
		PaymentStatus paymentStatus,
		PaymentAttemptStatus paymentAttemptStatus,
		String stripeCheckoutSessionId,
		String checkoutSessionStatus,
		String checkoutSessionPaymentStatus
) {
}
