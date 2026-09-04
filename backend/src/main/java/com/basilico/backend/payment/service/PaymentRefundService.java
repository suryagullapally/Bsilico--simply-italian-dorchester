package com.basilico.backend.payment.service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.basilico.backend.common.error.BadRequestException;
import com.basilico.backend.common.error.ConflictException;
import com.basilico.backend.common.error.ResourceNotFoundException;
import com.basilico.backend.notification.service.CustomerNotificationService;
import com.basilico.backend.order.entity.CustomerOrder;
import com.basilico.backend.order.entity.PaymentStatus;
import com.basilico.backend.order.repository.CustomerOrderRepository;
import com.basilico.backend.payment.dto.RefundOrderRequest;
import com.basilico.backend.payment.entity.PaymentAttempt;
import com.basilico.backend.payment.entity.PaymentAttemptStatus;
import com.basilico.backend.payment.entity.PaymentRefund;
import com.basilico.backend.payment.entity.PaymentRefundReason;
import com.basilico.backend.payment.entity.PaymentRefundStatus;
import com.basilico.backend.payment.repository.PaymentAttemptRepository;
import com.basilico.backend.payment.repository.PaymentRefundRepository;
import com.basilico.backend.payment.stripe.StripeCheckoutGateway;
import com.basilico.backend.payment.stripe.StripeRefundCreateCommand;
import com.basilico.backend.payment.stripe.StripeRefundData;
import com.basilico.backend.payment.stripe.StripeRefundReason;

@Service
public class PaymentRefundService {

	private static final Logger logger = LoggerFactory.getLogger(PaymentRefundService.class);

	private static final List<PaymentRefundStatus> ACTIVE_OR_SUCCEEDED_REFUND_STATUSES = List.of(
			PaymentRefundStatus.CREATED,
			PaymentRefundStatus.PENDING,
			PaymentRefundStatus.REQUIRES_ACTION,
			PaymentRefundStatus.SUCCEEDED
	);

	private final CustomerOrderRepository orderRepository;
	private final PaymentAttemptRepository paymentAttemptRepository;
	private final PaymentRefundRepository paymentRefundRepository;
	private final StripeCheckoutGateway stripeCheckoutGateway;
	private final CustomerNotificationService notificationService;

	public PaymentRefundService(CustomerOrderRepository orderRepository,
			PaymentAttemptRepository paymentAttemptRepository,
			PaymentRefundRepository paymentRefundRepository,
			StripeCheckoutGateway stripeCheckoutGateway,
			CustomerNotificationService notificationService) {
		this.orderRepository = orderRepository;
		this.paymentAttemptRepository = paymentAttemptRepository;
		this.paymentRefundRepository = paymentRefundRepository;
		this.stripeCheckoutGateway = stripeCheckoutGateway;
		this.notificationService = notificationService;
	}

	@Transactional
	public void refundOrder(Long orderId, RefundOrderRequest request) {
		validateRefundRequest(request);

		CustomerOrder order = orderRepository.findByIdForUpdate(orderId)
				.orElseThrow(() -> new ResourceNotFoundException("Order not found"));
		validateOrderCanBeRefunded(order);

		PaymentAttempt attempt = findSuccessfulPaymentAttempt(order);
		PaymentRefund refund = paymentRefundRepository.saveAndFlush(new PaymentRefund(
				order,
				attempt,
				order.getTotalPence(),
				request.reason(),
				blankToNull(request.note())
		));

		StripeRefundData stripeRefund;
		try {
			stripeRefund = stripeCheckoutGateway.createRefund(new StripeRefundCreateCommand(
					attempt.getStripePaymentIntentId(),
					order.getTotalPence(),
					"GBP",
					toStripeReason(request.reason()),
					Map.of(
							"order_reference", order.getOrderReference(),
							"internal_order_id", order.getId().toString(),
							"payment_attempt_id", attempt.getId().toString(),
							"payment_refund_id", refund.getId().toString()
					),
					"basilico-order-" + order.getOrderReference() + "-full-refund"
			));
		}
		catch (RuntimeException exception) {
			refund.setStatus(PaymentRefundStatus.FAILED);
			refund.setFailureReason("Stripe refund request failed. Please try again.");
			return;
		}

		applyStripeRefundData(refund, stripeRefund);
		completeOrderRefundIfSucceeded(refund);
	}

	@Transactional
	public void handleStripeRefundWebhook(StripeRefundData refundData) {
		if (refundData == null || refundData.id() == null || refundData.id().isBlank()) {
			throw new BadRequestException("Stripe webhook does not contain a refund");
		}

		Optional<PaymentRefund> refund = paymentRefundRepository.findByStripeRefundIdForUpdate(refundData.id());
		if (refund.isEmpty() && refundData.metadata() != null) {
			refund = findRefundByMetadata(refundData.metadata());
			if (refund.isEmpty() && hasBasilicoRefundMetadata(refundData.metadata())) {
				throw new ConflictException("Stripe refund record is not ready yet.");
			}
		}
		if (refund.isEmpty() && refundData.paymentIntentId() != null && !refundData.paymentIntentId().isBlank()) {
			refund = paymentRefundRepository.findByStripePaymentIntentId(refundData.paymentIntentId())
					.stream()
					.findFirst();
		}

		if (refund.isEmpty()) {
			logger.warn("Ignoring Stripe refund event for unrecognized refund {}", refundData.id());
			return;
		}

		PaymentRefund paymentRefund = refund.get();
		applyStripeRefundData(paymentRefund, refundData);
		completeOrderRefundIfSucceeded(paymentRefund);
	}

	private void validateRefundRequest(RefundOrderRequest request) {
		if (request.reason() == PaymentRefundReason.OTHER && blankToNull(request.note()) == null) {
			throw new BadRequestException("A note is required when refund reason is Other.");
		}
	}

	private void validateOrderCanBeRefunded(CustomerOrder order) {
		if (order.getPaymentStatus() != PaymentStatus.PAID) {
			throw new ConflictException("Only paid orders can be refunded.");
		}

		if (paymentRefundRepository.existsByOrderIdAndStatusIn(order.getId(), ACTIVE_OR_SUCCEEDED_REFUND_STATUSES)) {
			throw new ConflictException("This order already has a refund in progress or completed.");
		}
	}

	private PaymentAttempt findSuccessfulPaymentAttempt(CustomerOrder order) {
		PaymentAttempt attempt = paymentAttemptRepository
				.findFirstByOrderAndStatusAndStripePaymentIntentIdIsNotNullOrderByCreatedAtDesc(
						order, PaymentAttemptStatus.PAID)
				.filter(candidate -> !candidate.getStripePaymentIntentId().isBlank())
				.orElseThrow(() -> new ConflictException("This order does not have a refundable Stripe payment."));

		if (attempt.getAmountPence() != order.getTotalPence()) {
			throw new ConflictException("Paid amount could not be verified for refund.");
		}

		return attempt;
	}

	private Optional<PaymentRefund> findRefundByMetadata(Map<String, String> metadata) {
		String refundId = metadata.get("payment_refund_id");
		if (refundId == null || refundId.isBlank()) {
			return Optional.empty();
		}

		try {
			return paymentRefundRepository.findByIdForUpdate(Long.valueOf(refundId));
		}
		catch (NumberFormatException exception) {
			return Optional.empty();
		}
	}

	private boolean hasBasilicoRefundMetadata(Map<String, String> metadata) {
		return metadata.containsKey("payment_refund_id")
				|| metadata.containsKey("internal_order_id")
				|| metadata.containsKey("order_reference");
	}

	private void applyStripeRefundData(PaymentRefund refund, StripeRefundData stripeRefund) {
		if (stripeRefund.id() != null && !stripeRefund.id().isBlank()) {
			refund.setStripeRefundId(stripeRefund.id());
		}

		if (stripeRefund.amountPence() != null && stripeRefund.amountPence() != refund.getAmountPence()) {
			refund.setStatus(PaymentRefundStatus.FAILED);
			refund.setFailureReason("Stripe refund amount did not match the full order amount.");
			return;
		}

		if (stripeRefund.currency() != null
				&& !stripeRefund.currency().equalsIgnoreCase(refund.getCurrency())) {
			refund.setStatus(PaymentRefundStatus.FAILED);
			refund.setFailureReason("Stripe refund currency did not match the order currency.");
			return;
		}

		refund.setStatus(toRefundStatus(stripeRefund.status()));
		refund.setFailureReason(blankToNull(stripeRefund.failureReason()));

		if (refund.getStatus() == PaymentRefundStatus.SUCCEEDED && refund.getCompletedAt() == null) {
			refund.setCompletedAt(OffsetDateTime.now());
		}
	}

	private void completeOrderRefundIfSucceeded(PaymentRefund refund) {
		if (refund.getStatus() != PaymentRefundStatus.SUCCEEDED) {
			return;
		}

		CustomerOrder order = refund.getOrder();
		if (refund.getAmountPence() != order.getTotalPence()) {
			logger.warn("Stripe refund {} does not match order {} total; not marking order refunded.",
					refund.getStripeRefundId(), order.getOrderReference());
			return;
		}

		if (order.getPaymentStatus() != PaymentStatus.REFUNDED) {
			order.setPaymentStatus(PaymentStatus.REFUNDED);
		}
		notificationService.queueOrderRefunded(order, refund);
	}

	private PaymentRefundStatus toRefundStatus(String stripeStatus) {
		if (stripeStatus == null || stripeStatus.isBlank()) {
			return PaymentRefundStatus.PENDING;
		}

		return switch (stripeStatus.trim().toLowerCase(Locale.UK)) {
			case "succeeded" -> PaymentRefundStatus.SUCCEEDED;
			case "failed" -> PaymentRefundStatus.FAILED;
			case "canceled", "cancelled" -> PaymentRefundStatus.CANCELED;
			case "requires_action" -> PaymentRefundStatus.REQUIRES_ACTION;
			case "pending" -> PaymentRefundStatus.PENDING;
			default -> PaymentRefundStatus.PENDING;
		};
	}

	private StripeRefundReason toStripeReason(PaymentRefundReason reason) {
		return switch (reason) {
			case CUSTOMER_REQUESTED -> StripeRefundReason.REQUESTED_BY_CUSTOMER;
			case DUPLICATE -> StripeRefundReason.DUPLICATE;
			case FRAUDULENT -> StripeRefundReason.FRAUDULENT;
			case OTHER -> null;
		};
	}

	private String blankToNull(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}

		return value.trim();
	}
}
