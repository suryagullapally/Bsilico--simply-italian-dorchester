package com.basilico.backend.payment.stripe;

import com.basilico.backend.common.error.BadRequestException;
import com.basilico.backend.common.error.ServiceUnavailableException;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.Refund;
import com.stripe.model.StripeObject;
import com.stripe.model.checkout.Session;
import com.stripe.net.RequestOptions;
import com.stripe.net.Webhook;
import com.stripe.param.RefundCreateParams;
import com.stripe.param.checkout.SessionCreateParams;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class StripeJavaCheckoutGateway implements StripeCheckoutGateway {

	private static final Logger LOGGER = LoggerFactory.getLogger(StripeJavaCheckoutGateway.class);

	private static final String CHECKOUT_SESSION_COMPLETED = "checkout.session.completed";
	private static final String CHECKOUT_SESSION_EXPIRED = "checkout.session.expired";
	private static final String CHECKOUT_SESSION_ASYNC_PAYMENT_SUCCEEDED = "checkout.session.async_payment_succeeded";
	private static final String CHECKOUT_SESSION_ASYNC_PAYMENT_FAILED = "checkout.session.async_payment_failed";
	private static final String REFUND_CREATED = "refund.created";
	private static final String REFUND_UPDATED = "refund.updated";
	private static final String REFUND_FAILED = "refund.failed";

	private final StripePaymentProperties properties;

	public StripeJavaCheckoutGateway(StripePaymentProperties properties) {
		this.properties = properties;
	}

	@Override
	public StripeCheckoutSessionData createCheckoutSession(StripeCheckoutSessionCreateCommand command) {
		requireSecretKey();

		SessionCreateParams.Builder builder = SessionCreateParams.builder()
				.setMode(SessionCreateParams.Mode.PAYMENT)
				.setUiMode(SessionCreateParams.UiMode.ELEMENTS)
				.setCurrency("gbp")
				.setReturnUrl(command.returnUrl())
				.setClientReferenceId(command.metadata().get("order_reference"));

		if (command.customerEmail() != null && !command.customerEmail().isBlank()) {
			builder.setCustomerEmail(command.customerEmail());
		}

		command.metadata().forEach(builder::putMetadata);
		command.lineItems().stream()
				.map(this::toStripeLineItem)
				.forEach(builder::addLineItem);

		try {
			Session session = Session.create(
					builder.build(),
					requestOptionsBuilder()
							.setIdempotencyKey(command.idempotencyKey())
							.build()
			);
			return toSessionData(session);
		}
		catch (StripeException exception) {
			logStripeException("create checkout session", exception);
			throw paymentUnavailable();
		}
	}

	@Override
	public StripeCheckoutSessionData retrieveCheckoutSession(String sessionId) {
		requireSecretKey();

		try {
			return toSessionData(Session.retrieve(sessionId, requestOptionsBuilder().build()));
		}
		catch (StripeException exception) {
			logStripeException("retrieve checkout session", exception);
			throw paymentUnavailable();
		}
	}

	@Override
	public StripeRefundData createRefund(StripeRefundCreateCommand command) {
		requireSecretKey();

		RefundCreateParams.Builder builder = RefundCreateParams.builder()
				.setPaymentIntent(command.paymentIntentId())
				.setAmount((long) command.amountPence())
				.setCurrency(command.currency().toLowerCase(java.util.Locale.UK));

		if (command.reason() != null) {
			builder.setReason(toStripeReason(command.reason()));
		}

		command.metadata().forEach(builder::putMetadata);

		try {
			Refund refund = Refund.create(
					builder.build(),
					requestOptionsBuilder()
							.setIdempotencyKey(command.idempotencyKey())
							.build()
			);
			return toRefundData(refund);
		}
		catch (StripeException exception) {
			logStripeException("create refund", exception);
			throw paymentUnavailable();
		}
	}

	private void logStripeException(String operation, StripeException exception) {
		String stripeErrorType = exception.getStripeError() == null ? null : exception.getStripeError().getType();
		String stripeErrorCode = exception.getStripeError() == null ? null : exception.getStripeError().getCode();
		String stripeErrorParam = exception.getStripeError() == null ? null : exception.getStripeError().getParam();

		LOGGER.warn(
				"Stripe {} failed: exception={}, status={}, type={}, code={}, param={}, message={}",
				operation,
				exception.getClass().getSimpleName(),
				exception.getStatusCode(),
				stripeErrorType,
				stripeErrorCode,
				stripeErrorParam,
				exception.getMessage()
		);
	}

	@Override
	public StripeWebhookEventData constructWebhookEvent(String payload, String signatureHeader) {
		if (signatureHeader == null || signatureHeader.isBlank()) {
			throw new BadRequestException("Stripe webhook signature is missing");
		}

		if (!properties.hasWebhookSecret()) {
			throw new ServiceUnavailableException("Stripe webhook handling is not configured.");
		}

		try {
			Event event = Webhook.constructEvent(payload, signatureHeader, properties.webhookSecret());
			return new StripeWebhookEventData(event.getId(), event.getType(),
					readCheckoutSession(event), readRefund(event));
		}
		catch (SignatureVerificationException exception) {
			throw new BadRequestException("Invalid Stripe webhook signature");
		}
	}

	private SessionCreateParams.LineItem toStripeLineItem(StripeLineItem item) {
		SessionCreateParams.LineItem.PriceData.ProductData.Builder productData =
				SessionCreateParams.LineItem.PriceData.ProductData.builder()
						.setName(item.name());

		if (item.description() != null && !item.description().isBlank()) {
			productData.setDescription(item.description());
		}

		return SessionCreateParams.LineItem.builder()
				.setQuantity((long) item.quantity())
				.setPriceData(SessionCreateParams.LineItem.PriceData.builder()
						.setCurrency("gbp")
						.setUnitAmount((long) item.unitAmountPence())
						.setProductData(productData.build())
						.build())
				.build();
	}

	private StripeCheckoutSessionData readCheckoutSession(Event event) {
		if (!isCheckoutSessionEvent(event.getType())) {
			return null;
		}

		StripeObject stripeObject = event.getDataObjectDeserializer()
				.getObject()
				.orElseThrow(() -> new BadRequestException("Stripe webhook payload could not be read"));

		if (!(stripeObject instanceof Session session)) {
			throw new BadRequestException("Stripe webhook payload is not a checkout session");
		}

		return toSessionData(session);
	}

	private boolean isCheckoutSessionEvent(String eventType) {
		return CHECKOUT_SESSION_COMPLETED.equals(eventType)
				|| CHECKOUT_SESSION_EXPIRED.equals(eventType)
				|| CHECKOUT_SESSION_ASYNC_PAYMENT_SUCCEEDED.equals(eventType)
				|| CHECKOUT_SESSION_ASYNC_PAYMENT_FAILED.equals(eventType);
	}

	private RefundCreateParams.Reason toStripeReason(StripeRefundReason reason) {
		return switch (reason) {
			case REQUESTED_BY_CUSTOMER -> RefundCreateParams.Reason.REQUESTED_BY_CUSTOMER;
			case DUPLICATE -> RefundCreateParams.Reason.DUPLICATE;
			case FRAUDULENT -> RefundCreateParams.Reason.FRAUDULENT;
		};
	}

	private StripeRefundData readRefund(Event event) {
		if (!isRefundEvent(event.getType())) {
			return null;
		}

		StripeObject stripeObject = event.getDataObjectDeserializer()
				.getObject()
				.orElseThrow(() -> new BadRequestException("Stripe webhook payload could not be read"));

		if (!(stripeObject instanceof Refund refund)) {
			throw new BadRequestException("Stripe webhook payload is not a refund");
		}

		return toRefundData(refund);
	}

	private boolean isRefundEvent(String eventType) {
		return REFUND_CREATED.equals(eventType)
				|| REFUND_UPDATED.equals(eventType)
				|| REFUND_FAILED.equals(eventType);
	}

	private StripeCheckoutSessionData toSessionData(Session session) {
		String orderReference = session.getMetadata() == null
				? null
				: session.getMetadata().get("order_reference");

		return new StripeCheckoutSessionData(
				session.getId(),
				session.getClientSecret(),
				session.getStatus(),
				session.getPaymentStatus(),
				session.getAmountTotal() == null ? null : Math.toIntExact(session.getAmountTotal()),
				session.getCurrency(),
				session.getPaymentIntent(),
				orderReference
		);
	}

	private StripeRefundData toRefundData(Refund refund) {
		return new StripeRefundData(
				refund.getId(),
				refund.getStatus(),
				refund.getAmount() == null ? null : Math.toIntExact(refund.getAmount()),
				refund.getCurrency(),
				refund.getPaymentIntent(),
				refund.getReason(),
				refund.getFailureReason(),
				refund.getMetadata() == null ? java.util.Map.of() : refund.getMetadata()
		);
	}

	private RequestOptions.RequestOptionsBuilder requestOptionsBuilder() {
		return RequestOptions.builder().setApiKey(properties.secretKey());
	}

	private void requireSecretKey() {
		if (!properties.hasSecretKey()) {
			throw new ServiceUnavailableException("Online payment is not configured yet.");
		}
	}

	private ServiceUnavailableException paymentUnavailable() {
		return new ServiceUnavailableException("Payment service is unavailable. Please try again.");
	}
}
