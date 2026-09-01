package com.basilico.backend.order.dto;

import java.time.OffsetDateTime;
import java.util.List;

import com.basilico.backend.order.entity.FulfilmentType;
import com.basilico.backend.order.entity.OrderStatus;
import com.basilico.backend.order.entity.PaymentStatus;
import com.basilico.backend.payment.dto.PaymentAttemptResponse;

public record AdminOrderResponse(
		Long id,
		String orderReference,
		OrderStatus status,
		PaymentStatus paymentStatus,
		FulfilmentType fulfilmentType,
		CustomerResponse customer,
		DeliveryAddressResponse deliveryAddress,
		OrderTimingResponse timing,
		String notes,
		int subtotalPence,
		int deliveryFeePence,
		int totalPence,
		Double deliveryDistanceMiles,
		Integer deliveryPreparationMinutes,
		Integer deliveryTravelMinutes,
		Integer estimatedDeliveryMinutes,
		List<OrderItemResponse> items,
		List<PaymentAttemptResponse> paymentAttempts,
		OffsetDateTime createdAt,
		OffsetDateTime updatedAt
) {
}
