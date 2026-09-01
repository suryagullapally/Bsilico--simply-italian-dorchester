package com.basilico.backend.order.dto;

import java.util.List;

import com.basilico.backend.order.entity.OrderItemType;

public record OrderItemResponse(
		OrderItemType itemType,
		String productName,
		String productSlug,
		int unitPricePence,
		int quantity,
		int lineTotalPence,
		List<OrderItemToppingResponse> toppings
) {
}
