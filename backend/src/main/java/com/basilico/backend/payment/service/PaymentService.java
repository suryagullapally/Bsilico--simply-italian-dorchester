package com.basilico.backend.payment.service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.basilico.backend.common.error.BadRequestException;
import com.basilico.backend.common.error.ConflictException;
import com.basilico.backend.common.error.ResourceNotFoundException;
import com.basilico.backend.common.error.ServiceUnavailableException;
import com.basilico.backend.order.entity.CustomerOrder;
import com.basilico.backend.order.entity.OrderItem;
import com.basilico.backend.order.entity.OrderStatus;
import com.basilico.backend.order.entity.PaymentStatus;
import com.basilico.backend.order.repository.CustomerOrderRepository;
import com.basilico.backend.notification.service.CustomerNotificationService;
import com.basilico.backend.payment.dto.CheckoutSessionResponse;
import com.basilico.backend.payment.dto.CheckoutSessionStatusResponse;
import com.basilico.backend.payment.dto.CreateCheckoutSessionRequest;
import com.basilico.backend.payment.dto.PaymentAttemptResponse;
import com.basilico.backend.payment.dto.WebhookResponse;
import com.basilico.backend.payment.entity.PaymentAttempt;
import com.basilico.backend.payment.entity.PaymentAttemptStatus;
import com.basilico.backend.payment.entity.StripeWebhookEvent;
import com.basilico.backend.payment.repository.PaymentAttemptRepository;
import com.basilico.backend.payment.repository.StripeWebhookEventRepository;
import com.basilico.backend.payment.stripe.StripeCheckoutGateway;
import com.basilico.backend.payment.stripe.StripeCheckoutSessionCreateCommand;
import com.basilico.backend.payment.stripe.StripeCheckoutSessionData;
import com.basilico.backend.payment.stripe.StripeLineItem;
import com.basilico.backend.payment.stripe.StripePaymentProperties;
import com.basilico.backend.payment.stripe.StripeWebhookEventData;

@Service
public class PaymentService {

	private static final Logger logger = LoggerFactory.getLogger(PaymentService.class);

	private static final String CHECKOUT_SESSION_COMPLETED = "checkout.session.completed";
	private static final String CHECKOUT_SESSION_EXPIRED = "checkout.session.expired";
	private static final String CHECKOUT_SESSION_ASYNC_PAYMENT_SUCCEEDED = "checkout.session.async_payment_succeeded";
	private static final String CHECKOUT_SESSION_ASYNC_PAYMENT_FAILED = "checkout.session.async_payment_failed";
	private static final String REFUND_CREATED = "refund.created";
	private static final String REFUND_UPDATED = "refund.updated";
	private static final String REFUND_FAILED = "refund.failed";

	private final CustomerOrderRepository orderRepository;
	private final PaymentAttemptRepository paymentAttemptRepository;
	private final StripeWebhookEventRepository webhookEventRepository;
	private final StripeCheckoutGateway stripeCheckoutGateway;
	private final StripePaymentProperties stripePaymentProperties;
	private final CustomerNotificationService notificationService;
	private final PaymentRefundService paymentRefundService;

	public PaymentService(CustomerOrderRepository orderRepository,
			PaymentAttemptRepository paymentAttemptRepository,
			StripeWebhookEventRepository webhookEventRepository,
			StripeCheckoutGateway stripeCheckoutGateway,
			StripePaymentProperties stripePaymentProperties,
			CustomerNotificationService notificationService,
			PaymentRefundService paymentRefundService) {
		this.orderRepository = orderRepository;
		this.paymentAttemptRepository = paymentAttemptRepository;
		this.webhookEventRepository = webhookEventRepository;
		this.stripeCheckoutGateway = stripeCheckoutGateway;
		this.stripePaymentProperties = stripePaymentProperties;
		this.notificationService = notificationService;
		this.paymentRefundService = paymentRefundService;
	}

	@Transactional
	public CheckoutSessionResponse createCheckoutSession(CreateCheckoutSessionRequest request) {
		String orderReference = normalizeOrderReference(request.orderReference());
		CustomerOrder order = orderRepository.findDetailedByOrderReference(orderReference)
				.orElseThrow(() -> new ResourceNotFoundException("Order not found"));

		validateOrderCanBePaid(order);
		validateOrderSnapshotTotal(order);

		PaymentAttempt reusableAttempt = findReusableOpenAttempt(order);
		if (reusableAttempt != null) {
			StripeCheckoutSessionData sessionData =
					stripeCheckoutGateway.retrieveCheckoutSession(reusableAttempt.getStripeCheckoutSessionId());
			if (isOpen(sessionData.status()) && !isPaid(sessionData.paymentStatus())) {
				return toCheckoutSessionResponse(order, reusableAttempt, sessionData);
			}

			if (isExpired(sessionData.status())) {
				reusableAttempt.setStatus(PaymentAttemptStatus.EXPIRED);
			}
			else if (isPaid(sessionData.paymentStatus())) {
				applyPaidSession(reusableAttempt, sessionData);
				throw new ConflictException("This order has already been paid.");
			}
		}

		PaymentAttempt attempt = paymentAttemptRepository.saveAndFlush(new PaymentAttempt(order, order.getTotalPence()));
		StripeCheckoutSessionData sessionData = stripeCheckoutGateway.createCheckoutSession(
				new StripeCheckoutSessionCreateCommand(
						order.getCustomerEmail(),
						returnUrl(),
						toStripeLineItems(order),
						Map.of(
								"order_reference", order.getOrderReference(),
								"internal_order_id", order.getId().toString()
						),
						"basilico-order-" + order.getOrderReference() + "-payment-" + attempt.getId()
				)
		);

		if (sessionData.clientSecret() == null || sessionData.clientSecret().isBlank()) {
			throw new ConflictException("Stripe checkout session did not return a client secret");
		}

		attempt.setStripeCheckoutSessionId(sessionData.id());
		attempt.setStatus(PaymentAttemptStatus.OPEN);
		return toCheckoutSessionResponse(order, attempt, sessionData);
	}

	@Transactional
	public CheckoutSessionStatusResponse getCheckoutSessionStatus(String sessionId) {
		PaymentAttempt attempt = paymentAttemptRepository.findByStripeCheckoutSessionId(sessionId)
				.orElseThrow(() -> new ResourceNotFoundException("Payment attempt not found"));
		CustomerOrder order = attempt.getOrder();
		StripeCheckoutSessionData sessionData = null;

		if (attempt.getStatus() == PaymentAttemptStatus.OPEN) {
			sessionData = stripeCheckoutGateway.retrieveCheckoutSession(sessionId);

			if (isExpired(sessionData.status())) {
				attempt.setStatus(PaymentAttemptStatus.EXPIRED);
			}
		}

		return new CheckoutSessionStatusResponse(
				order.getOrderReference(),
				order.getStatus(),
				order.getPaymentStatus(),
				attempt.getStatus(),
				attempt.getStripeCheckoutSessionId(),
				sessionData == null ? null : sessionData.status(),
				sessionData == null ? null : sessionData.paymentStatus()
		);
	}

	@Transactional
	public WebhookResponse handleStripeWebhook(String payload, String signatureHeader) {
		StripeWebhookEventData event = stripeCheckoutGateway.constructWebhookEvent(payload, signatureHeader);

		if (webhookEventRepository.existsByStripeEventId(event.id())) {
			return new WebhookResponse("duplicate");
		}

		switch (event.type()) {
			case CHECKOUT_SESSION_COMPLETED, CHECKOUT_SESSION_ASYNC_PAYMENT_SUCCEEDED -> handlePaidSession(event.session());
			case CHECKOUT_SESSION_EXPIRED -> handleExpiredSession(event.session());
			case CHECKOUT_SESSION_ASYNC_PAYMENT_FAILED -> handleFailedSession(event.session());
			case REFUND_CREATED, REFUND_UPDATED, REFUND_FAILED -> paymentRefundService.handleStripeRefundWebhook(event.refund());
			default -> {
				// Unknown Stripe events are recorded for idempotency but do not affect order state.
			}
		}

		webhookEventRepository.save(new StripeWebhookEvent(event.id(), event.type()));
		return new WebhookResponse("processed");
	}

	private PaymentAttempt findReusableOpenAttempt(CustomerOrder order) {
		return paymentAttemptRepository.findFirstByOrderAndStatusOrderByCreatedAtDesc(order, PaymentAttemptStatus.OPEN)
				.filter(attempt -> attempt.getStripeCheckoutSessionId() != null
						&& !attempt.getStripeCheckoutSessionId().isBlank())
				.orElse(null);
	}

	private void validateOrderCanBePaid(CustomerOrder order) {
		if (order.getPaymentStatus() == PaymentStatus.PAID) {
			throw new ConflictException("This order has already been paid.");
		}

		if (order.getStatus() == OrderStatus.CANCELLED || order.getStatus() == OrderStatus.COMPLETED) {
			throw new ConflictException("This order is no longer eligible for online payment.");
		}
	}

	private void validateOrderSnapshotTotal(CustomerOrder order) {
		int calculatedSubtotal = order.getItems()
				.stream()
				.mapToInt(item -> {
					int calculatedLineTotal = Math.toIntExact((long) item.getUnitPricePence() * item.getQuantity());
					if (calculatedLineTotal != item.getLineTotalPence()) {
						logger.error("Order {} has inconsistent item snapshot totals", order.getOrderReference());
						throw new ConflictException("Order total could not be verified.");
					}
					return item.getLineTotalPence();
				})
				.sum();

		if (calculatedSubtotal != order.getSubtotalPence()) {
			logger.error("Order {} has inconsistent subtotal snapshot", order.getOrderReference());
			throw new ConflictException("Order total could not be verified.");
		}

		int calculatedTotal = Math.toIntExact((long) order.getSubtotalPence() + order.getDeliveryFeePence());
		if (calculatedTotal != order.getTotalPence()) {
			logger.error("Order {} has inconsistent payable total snapshot", order.getOrderReference());
			throw new ConflictException("Order total could not be verified.");
		}
	}

	private List<StripeLineItem> toStripeLineItems(CustomerOrder order) {
		List<StripeLineItem> lineItems = new ArrayList<>(order.getItems()
				.stream()
				.map(item -> new StripeLineItem(
						item.getProductNameSnapshot(),
						descriptionForStripe(item),
						item.getUnitPricePence(),
						item.getQuantity()
				))
				.toList());

		if (order.getDeliveryFeePence() > 0) {
			lineItems.add(new StripeLineItem("Delivery", null, order.getDeliveryFeePence(), 1));
		}

		return lineItems;
	}

	private String descriptionForStripe(OrderItem item) {
		if (item.getToppings().isEmpty()) {
			return null;
		}

		String toppings = item.getToppings()
				.stream()
				.map(topping -> topping.getToppingNameSnapshot())
				.reduce((left, right) -> left + ", " + right)
				.orElse("");

		return "Toppings: " + toppings;
	}

	private CheckoutSessionResponse toCheckoutSessionResponse(CustomerOrder order, PaymentAttempt attempt,
			StripeCheckoutSessionData sessionData) {
		return new CheckoutSessionResponse(
				order.getOrderReference(),
				sessionData.clientSecret(),
				attempt.getStripeCheckoutSessionId(),
				attempt.getAmountPence(),
				"gbp"
		);
	}

	private void handlePaidSession(StripeCheckoutSessionData sessionData) {
		requireSessionData(sessionData);
		PaymentAttempt attempt = paymentAttemptRepository.findByStripeCheckoutSessionId(sessionData.id())
				.orElseThrow(() -> new ResourceNotFoundException("Payment attempt not found"));
		applyPaidSession(attempt, sessionData);
	}

	private void applyPaidSession(PaymentAttempt attempt, StripeCheckoutSessionData sessionData) {
		CustomerOrder order = attempt.getOrder();
		validateSessionMatchesOrder(attempt, sessionData);

		if (!isPaid(sessionData.paymentStatus())) {
			return;
		}

		PaymentStatus previousPaymentStatus = order.getPaymentStatus();
		attempt.setStatus(PaymentAttemptStatus.PAID);
		attempt.setStripePaymentIntentId(sessionData.paymentIntentId());
		attempt.setCompletedAt(OffsetDateTime.now());
		order.setPaymentStatus(PaymentStatus.PAID);

		if (order.getStatus() == OrderStatus.PENDING_PAYMENT) {
			order.setStatus(OrderStatus.NEW);
		}

		if (previousPaymentStatus != PaymentStatus.PAID && order.getStatus() == OrderStatus.NEW) {
			notificationService.queueOrderReceived(order);
			notificationService.queueRestaurantNewOrderAlert(order);
		}
	}

	private void handleExpiredSession(StripeCheckoutSessionData sessionData) {
		requireSessionData(sessionData);
		paymentAttemptRepository.findByStripeCheckoutSessionId(sessionData.id())
				.ifPresent(attempt -> attempt.setStatus(PaymentAttemptStatus.EXPIRED));
	}

	private void handleFailedSession(StripeCheckoutSessionData sessionData) {
		requireSessionData(sessionData);
		paymentAttemptRepository.findByStripeCheckoutSessionId(sessionData.id())
				.ifPresent(attempt -> {
					attempt.setStatus(PaymentAttemptStatus.FAILED);
					attempt.getOrder().setPaymentStatus(PaymentStatus.FAILED);
				});
	}

	private void validateSessionMatchesOrder(PaymentAttempt attempt, StripeCheckoutSessionData sessionData) {
		CustomerOrder order = attempt.getOrder();

		if (sessionData.orderReference() != null
				&& !order.getOrderReference().equals(normalizeOrderReference(sessionData.orderReference()))) {
			throw new ConflictException("Stripe checkout session does not match this order.");
		}

		if (sessionData.amountTotal() == null || sessionData.amountTotal() != order.getTotalPence()
				|| sessionData.amountTotal() != attempt.getAmountPence()) {
			logger.error("Stripe session {} amount did not match order {}", sessionData.id(), order.getOrderReference());
			throw new ConflictException("Stripe payment amount does not match the order.");
		}

		if (sessionData.currency() == null || !"gbp".equals(sessionData.currency().toLowerCase(Locale.UK))) {
			throw new ConflictException("Stripe payment currency does not match the order.");
		}
	}

	private void requireSessionData(StripeCheckoutSessionData sessionData) {
		if (sessionData == null || sessionData.id() == null || sessionData.id().isBlank()) {
			throw new BadRequestException("Stripe webhook does not contain a checkout session");
		}
	}

	private String returnUrl() {
		if (!stripePaymentProperties.hasCustomerWebBaseUrl()) {
			throw new ServiceUnavailableException("Payment return URL is not configured.");
		}

		return stripePaymentProperties.normalizedCustomerWebBaseUrl()
				+ "/checkout/payment/return?session_id={CHECKOUT_SESSION_ID}";
	}

	private boolean isOpen(String status) {
		return "open".equalsIgnoreCase(status);
	}

	private boolean isExpired(String status) {
		return "expired".equalsIgnoreCase(status);
	}

	private boolean isPaid(String paymentStatus) {
		return "paid".equalsIgnoreCase(paymentStatus);
	}

	private String normalizeOrderReference(String value) {
		if (value == null || value.isBlank()) {
			throw new BadRequestException("Order reference is required");
		}

		return value.trim().toUpperCase(Locale.UK);
	}

	public PaymentAttemptResponse toPaymentAttemptResponse(PaymentAttempt attempt) {
		return new PaymentAttemptResponse(
				attempt.getId(),
				attempt.getProvider(),
				attempt.getStatus(),
				attempt.getAmountPence(),
				attempt.getCurrency(),
				attempt.getStripeCheckoutSessionId(),
				attempt.getStripePaymentIntentId(),
				attempt.getCreatedAt(),
				attempt.getUpdatedAt(),
				attempt.getCompletedAt()
		);
	}
}
