package com.basilico.backend.order.dto;

import java.time.OffsetDateTime;

import com.basilico.backend.order.entity.FulfilmentType;
import com.basilico.backend.order.entity.OrderStatus;
import com.basilico.backend.order.entity.PaymentStatus;

public record AdminOrderSummaryResponse(
		Long id,
		String orderReference,
		OrderStatus status,
		PaymentStatus paymentStatus,
		FulfilmentType fulfilmentType,
		String customerName,
		int subtotalPence,
		int deliveryFeePence,
		int totalPence,
		Double deliveryDistanceMiles,
		Integer estimatedDeliveryMinutes,
		OffsetDateTime createdAt,
		OffsetDateTime updatedAt
) {
}
