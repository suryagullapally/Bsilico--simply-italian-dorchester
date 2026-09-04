package com.basilico.backend.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.jayway.jsonpath.JsonPath;

import com.basilico.backend.menu.entity.MenuItem;
import com.basilico.backend.menu.repository.MenuItemRepository;
import com.basilico.backend.notification.entity.NotificationType;
import com.basilico.backend.notification.repository.CustomerNotificationRepository;
import com.basilico.backend.order.entity.CustomerOrder;
import com.basilico.backend.order.entity.OrderStatus;
import com.basilico.backend.order.entity.PaymentStatus;
import com.basilico.backend.order.repository.CustomerOrderRepository;
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
import com.basilico.backend.payment.stripe.StripeWebhookEventData;

@SpringBootTest(properties = "basilico.notifications.email.worker-enabled=false")
@AutoConfigureMockMvc
@Transactional
class PaymentRefundApiTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private MenuItemRepository menuItemRepository;

	@Autowired
	private CustomerOrderRepository orderRepository;

	@Autowired
	private PaymentAttemptRepository paymentAttemptRepository;

	@Autowired
	private PaymentRefundRepository paymentRefundRepository;

	@Autowired
	private CustomerNotificationRepository notificationRepository;

	@MockitoBean
	private StripeCheckoutGateway stripeCheckoutGateway;

	@BeforeEach
	void resetGateway() {
		org.mockito.Mockito.reset(stripeCheckoutGateway);
	}

	@Test
	void paidStripeOrderIsRefundEligible() throws Exception {
		CustomerOrder order = createPaidStripeOrder();

		mockMvc.perform(get("/api/admin/orders/{id}", order.getId())
						.with(adminUser()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.refundEligible").value(true))
				.andExpect(jsonPath("$.refundStatus").doesNotExist());
	}

	@Test
	void unpaidOrderCannotBeRefunded() throws Exception {
		CustomerOrder order = createOrder();

		mockMvc.perform(post("/api/admin/orders/{id}/refund", order.getId())
						.with(adminUser())
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(refundPayload("CUSTOMER_REQUESTED", "Customer called to cancel")))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value("Only paid orders can be refunded."));

		verifyNoInteractions(stripeCheckoutGateway);
	}

	@Test
	void missingPaymentIntentCannotBeRefunded() throws Exception {
		CustomerOrder order = createPaidStripeOrder();
		PaymentAttempt attempt = paymentAttemptRepository.findAll().stream()
				.filter(candidate -> candidate.getOrder().getId().equals(order.getId()))
				.findFirst()
				.orElseThrow();
		attempt.setStripePaymentIntentId(null);
		paymentAttemptRepository.saveAndFlush(attempt);

		mockMvc.perform(post("/api/admin/orders/{id}/refund", order.getId())
						.with(adminUser())
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(refundPayload("CUSTOMER_REQUESTED", "Customer called to cancel")))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value("This order does not have a refundable Stripe payment."));

		verifyNoInteractions(stripeCheckoutGateway);
	}

	@Test
	void refundEndpointRequiresCsrfAndAuthentication() throws Exception {
		CustomerOrder order = createPaidStripeOrder();

		mockMvc.perform(post("/api/admin/orders/{id}/refund", order.getId())
						.contentType(MediaType.APPLICATION_JSON)
						.content(refundPayload("CUSTOMER_REQUESTED", "Customer called to cancel")))
				.andExpect(status().isForbidden());

		mockMvc.perform(post("/api/admin/orders/{id}/refund", order.getId())
						.with(adminUser())
						.contentType(MediaType.APPLICATION_JSON)
						.content(refundPayload("CUSTOMER_REQUESTED", "Customer called to cancel")))
				.andExpect(status().isForbidden());

		verifyNoInteractions(stripeCheckoutGateway);
	}

	@Test
	void successfulFullRefundUsesServerOrderTotalAndQueuesCustomerEmail() throws Exception {
		CustomerOrder order = createPaidStripeOrder();
		when(stripeCheckoutGateway.createRefund(any()))
				.thenReturn(stripeRefund("re_succeeded", "succeeded", order.getTotalPence(),
						"pi_test_refundable", Map.of("payment_refund_id", "1")));

		mockMvc.perform(post("/api/admin/orders/{id}/refund", order.getId())
						.with(adminUser())
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "reason": "CUSTOMER_REQUESTED",
								  "note": "Customer called to cancel",
								  "amountPence": 1,
								  "paymentIntentId": "pi_from_browser"
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.paymentStatus").value("REFUNDED"))
				.andExpect(jsonPath("$.refundEligible").value(false))
				.andExpect(jsonPath("$.refundStatus").value("SUCCEEDED"))
				.andExpect(jsonPath("$.refundAmountPence").value(order.getTotalPence()));

		ArgumentCaptor<StripeRefundCreateCommand> captor =
				ArgumentCaptor.forClass(StripeRefundCreateCommand.class);
		verify(stripeCheckoutGateway).createRefund(captor.capture());

		StripeRefundCreateCommand command = captor.getValue();
		assertThat(command.paymentIntentId()).isEqualTo("pi_test_refundable");
		assertThat(command.amountPence()).isEqualTo(order.getTotalPence());
		assertThat(command.idempotencyKey()).isEqualTo(
				"basilico-order-" + order.getOrderReference() + "-full-refund");
		assertThat(command.metadata()).containsEntry("order_reference", order.getOrderReference());
		assertThat(paymentRefundRepository.findByOrderIdOrderByCreatedAtDesc(order.getId()))
				.hasSize(1)
				.first()
				.satisfies(refund -> {
					assertThat(refund.getStripeRefundId()).isEqualTo("re_succeeded");
					assertThat(refund.getStatus()).isEqualTo(PaymentRefundStatus.SUCCEEDED);
				});
		assertThat(notificationRepository.findAll())
				.filteredOn(notification -> notification.getNotificationType() == NotificationType.ORDER_REFUNDED)
				.hasSize(1);
	}

	@Test
	void pendingRefundPreventsDuplicateRefundSubmission() throws Exception {
		CustomerOrder order = createPaidStripeOrder();
		when(stripeCheckoutGateway.createRefund(any()))
				.thenReturn(stripeRefund("re_pending", "pending", order.getTotalPence(),
						"pi_test_refundable", Map.of()));

		mockMvc.perform(post("/api/admin/orders/{id}/refund", order.getId())
						.with(adminUser())
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(refundPayload("CUSTOMER_REQUESTED", "Customer called to cancel")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.paymentStatus").value("PAID"))
				.andExpect(jsonPath("$.refundStatus").value("PENDING"));

		mockMvc.perform(post("/api/admin/orders/{id}/refund", order.getId())
						.with(adminUser())
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(refundPayload("CUSTOMER_REQUESTED", "Second click")))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value("This order already has a refund in progress or completed."));

		verify(stripeCheckoutGateway, times(1)).createRefund(any());
	}

	@Test
	void failedRefundLeavesOrderPaidAndRecordsFailure() throws Exception {
		CustomerOrder order = createPaidStripeOrder();
		when(stripeCheckoutGateway.createRefund(any()))
				.thenReturn(stripeRefund("re_failed", "failed", order.getTotalPence(),
						"pi_test_refundable", Map.of()));

		mockMvc.perform(post("/api/admin/orders/{id}/refund", order.getId())
						.with(adminUser())
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(refundPayload("DUPLICATE", "Duplicate order")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.paymentStatus").value("PAID"))
				.andExpect(jsonPath("$.refundStatus").value("FAILED"));

		assertThat(orderRepository.findById(order.getId()).orElseThrow().getPaymentStatus())
				.isEqualTo(PaymentStatus.PAID);
		assertThat(notificationRepository.findAll())
				.filteredOn(notification -> notification.getNotificationType() == NotificationType.ORDER_REFUNDED)
				.isEmpty();
	}

	@Test
	void otherRefundReasonRequiresNote() throws Exception {
		CustomerOrder order = createPaidStripeOrder();

		mockMvc.perform(post("/api/admin/orders/{id}/refund", order.getId())
						.with(adminUser())
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(refundPayload("OTHER", "")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("A note is required when refund reason is Other."));

		verifyNoInteractions(stripeCheckoutGateway);
	}

	@Test
	void refundUpdatedWebhookToSucceededUpdatesOrderAndIsIdempotent() throws Exception {
		CustomerOrder order = createPaidStripeOrder();
		PaymentRefund refund = createPendingRefund(order, "re_webhook_succeeded");
		when(stripeCheckoutGateway.constructWebhookEvent(anyString(), anyString()))
				.thenReturn(new StripeWebhookEventData(
						"evt_refund_succeeded",
						"refund.updated",
						null,
						stripeRefund("re_webhook_succeeded", "succeeded", order.getTotalPence(),
								"pi_test_refundable", Map.of("payment_refund_id", refund.getId().toString()))
				));

		mockMvc.perform(post("/api/payments/stripe/webhook")
						.header("Stripe-Signature", "valid-signature")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("processed"));

		mockMvc.perform(post("/api/payments/stripe/webhook")
						.header("Stripe-Signature", "valid-signature")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("duplicate"));

		assertThat(orderRepository.findById(order.getId()).orElseThrow().getPaymentStatus())
				.isEqualTo(PaymentStatus.REFUNDED);
		assertThat(paymentRefundRepository.findById(refund.getId()).orElseThrow().getStatus())
				.isEqualTo(PaymentRefundStatus.SUCCEEDED);
		assertThat(notificationRepository.findAll())
				.filteredOn(notification -> notification.getNotificationType() == NotificationType.ORDER_REFUNDED)
				.hasSize(1);
	}

	@Test
	void refundFailedWebhookRecordsFailureAndLeavesOrderPaid() throws Exception {
		CustomerOrder order = createPaidStripeOrder();
		PaymentRefund refund = createPendingRefund(order, "re_webhook_failed");
		when(stripeCheckoutGateway.constructWebhookEvent(anyString(), anyString()))
				.thenReturn(new StripeWebhookEventData(
						"evt_refund_failed",
						"refund.failed",
						null,
						stripeRefund("re_webhook_failed", "failed", order.getTotalPence(),
								"pi_test_refundable", Map.of("payment_refund_id", refund.getId().toString()))
				));

		mockMvc.perform(post("/api/payments/stripe/webhook")
						.header("Stripe-Signature", "valid-signature")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("processed"));

		assertThat(orderRepository.findById(order.getId()).orElseThrow().getPaymentStatus())
				.isEqualTo(PaymentStatus.PAID);
		assertThat(paymentRefundRepository.findById(refund.getId()).orElseThrow().getStatus())
				.isEqualTo(PaymentRefundStatus.FAILED);
	}

	private PaymentRefund createPendingRefund(CustomerOrder order, String stripeRefundId) {
		PaymentAttempt attempt = paymentAttemptRepository
				.findFirstByOrderAndStatusAndStripePaymentIntentIdIsNotNullOrderByCreatedAtDesc(
						order, PaymentAttemptStatus.PAID)
				.orElseThrow();
		PaymentRefund refund = paymentRefundRepository.saveAndFlush(new PaymentRefund(
				order,
				attempt,
				order.getTotalPence(),
				PaymentRefundReason.CUSTOMER_REQUESTED,
				"Customer called to cancel"
		));
		refund.setStripeRefundId(stripeRefundId);
		refund.setStatus(PaymentRefundStatus.PENDING);
		return paymentRefundRepository.saveAndFlush(refund);
	}

	private StripeRefundData stripeRefund(String id, String status, int amountPence,
			String paymentIntentId, Map<String, String> metadata) {
		return new StripeRefundData(
				id,
				status,
				amountPence,
				"gbp",
				paymentIntentId,
				"requested_by_customer",
				status.equals("failed") ? "expired_or_canceled_card" : null,
				metadata
		);
	}

	private CustomerOrder createPaidStripeOrder() throws Exception {
		CustomerOrder order = createOrder();
		order.setPaymentStatus(PaymentStatus.PAID);
		order.setStatus(OrderStatus.NEW);
		orderRepository.saveAndFlush(order);
		PaymentAttempt attempt = paymentAttemptRepository.saveAndFlush(new PaymentAttempt(order, order.getTotalPence()));
		attempt.setStatus(PaymentAttemptStatus.PAID);
		attempt.setStripeCheckoutSessionId("cs_test_" + order.getId());
		attempt.setStripePaymentIntentId("pi_test_refundable");
		paymentAttemptRepository.saveAndFlush(attempt);
		return order;
	}

	private CustomerOrder createOrder() throws Exception {
		MenuItem margherita = menuItemRepository.findBySlug("pizza-margherita").orElseThrow();
		String response = mockMvc.perform(post("/api/orders")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "fulfilmentType": "COLLECTION",
								  "customer": {
								    "firstName": "John",
								    "lastName": "Smith",
								    "phone": "07123456789",
								    "email": "john@example.com"
								  },
								  "timing": {
								    "type": "SCHEDULED",
								    "requestedDate": "%s",
								    "requestedTime": "18:30"
								  },
								  "items": [
								    { "type": "MENU_ITEM", "menuItemId": %d, "quantity": 1 }
								  ]
								}
								""".formatted(nextOpenDate(), margherita.getId())))
				.andExpect(status().isCreated())
				.andReturn()
				.getResponse()
				.getContentAsString();
		String orderReference = JsonPath.read(response, "$.orderReference");
		return orderRepository.findDetailedByOrderReference(orderReference).orElseThrow();
	}

	private String refundPayload(String reason, String note) {
		return """
				{
				  "reason": "%s",
				  "note": "%s"
				}
				""".formatted(reason, note);
	}

	private LocalDate nextOpenDate() {
		LocalDate date = LocalDate.now().plusDays(1);
		while (date.getDayOfWeek() == DayOfWeek.TUESDAY) {
			date = date.plusDays(1);
		}
		return date;
	}

	private org.springframework.test.web.servlet.request.RequestPostProcessor adminUser() {
		return user("owner@basilico.test").roles("OWNER");
	}
}
