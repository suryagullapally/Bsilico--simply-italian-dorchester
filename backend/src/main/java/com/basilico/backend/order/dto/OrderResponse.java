package com.basilico.backend.order.dto;

import java.util.List;

import com.basilico.backend.order.entity.FulfilmentType;
import com.basilico.backend.order.entity.OrderStatus;
import com.basilico.backend.order.entity.PaymentStatus;

public record OrderResponse(
		String orderReference,
		OrderStatus status,
		PaymentStatus paymentStatus,
		FulfilmentType fulfilmentType,
		int subtotalPence,
		int deliveryFeePence,
		int totalPence,
		Double deliveryDistanceMiles,
		Integer deliveryPreparationMinutes,
		Integer deliveryTravelMinutes,
		Integer estimatedDeliveryMinutes,
		List<OrderItemResponse> items
) {
}
