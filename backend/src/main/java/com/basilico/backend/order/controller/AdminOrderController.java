package com.basilico.backend.order.controller;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.basilico.backend.common.dto.PageResponse;
import com.basilico.backend.order.dto.AdminOrderResponse;
import com.basilico.backend.order.dto.AdminOrderSummaryResponse;
import com.basilico.backend.order.dto.OrderStatusUpdateRequest;
import com.basilico.backend.order.dto.PaymentStatusUpdateRequest;
import com.basilico.backend.order.entity.FulfilmentType;
import com.basilico.backend.order.entity.OrderStatus;
import com.basilico.backend.order.entity.PaymentStatus;
import com.basilico.backend.order.service.OrderService;
import com.basilico.backend.payment.dto.RefundOrderRequest;
import com.basilico.backend.payment.service.PaymentRefundService;

@RestController
@RequestMapping("/api/admin/orders")
public class AdminOrderController {

	private final OrderService orderService;
	private final PaymentRefundService paymentRefundService;

	public AdminOrderController(OrderService orderService, PaymentRefundService paymentRefundService) {
		this.orderService = orderService;
		this.paymentRefundService = paymentRefundService;
	}

	@GetMapping
	public PageResponse<AdminOrderSummaryResponse> getOrders(
			@RequestParam(required = false) OrderStatus status,
			@RequestParam(required = false) PaymentStatus paymentStatus,
			@RequestParam(required = false) FulfilmentType fulfilmentType,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return orderService.getAdminOrders(status, paymentStatus, fulfilmentType, page, size);
	}

	@GetMapping("/{id}")
	public AdminOrderResponse getOrder(@PathVariable Long id) {
		return orderService.getAdminOrder(id);
	}

	@PatchMapping("/{id}/status")
	public AdminOrderResponse updateStatus(@PathVariable Long id,
			@Valid @RequestBody OrderStatusUpdateRequest request) {
		return orderService.updateStatus(id, request.status());
	}

	@PatchMapping("/{id}/payment-status")
	public AdminOrderResponse updatePaymentStatus(@PathVariable Long id,
			@Valid @RequestBody PaymentStatusUpdateRequest request) {
		return orderService.updatePaymentStatus(id, request.paymentStatus());
	}

	@PostMapping("/{id}/refund")
	public AdminOrderResponse refundPayment(@PathVariable Long id,
			@Valid @RequestBody RefundOrderRequest request) {
		paymentRefundService.refundOrder(id, request);
		return orderService.getAdminOrder(id);
	}
}
