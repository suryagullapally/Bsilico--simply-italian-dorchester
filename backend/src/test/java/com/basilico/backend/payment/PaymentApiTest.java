package com.basilico.backend.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.math.BigDecimal;
import java.util.Optional;

import com.jayway.jsonpath.JsonPath;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import com.basilico.backend.common.error.BadRequestException;
import com.basilico.backend.fulfilment.entity.DeliveryAreaMode;
import com.basilico.backend.fulfilment.entity.DeliveryPostcodeRule;
import com.basilico.backend.fulfilment.entity.DeliveryPricingMode;
import com.basilico.backend.fulfilment.entity.FulfilmentSettings;
import com.basilico.backend.fulfilment.repository.DeliveryPostcodeRuleRepository;
import com.basilico.backend.fulfilment.repository.FulfilmentSettingsRepository;
import com.basilico.backend.fulfilment.service.GeoCoordinates;
import com.basilico.backend.fulfilment.service.GeocodedPostcode;
import com.basilico.backend.fulfilment.service.PostcodeGeocoder;
import com.basilico.backend.fulfilment.service.RouteDurationProvider;
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
import com.basilico.backend.payment.repository.PaymentAttemptRepository;
import com.basilico.backend.payment.stripe.StripeCheckoutGateway;
import com.basilico.backend.payment.stripe.StripeCheckoutSessionCreateCommand;
import com.basilico.backend.payment.stripe.StripeCheckoutSessionData;
import com.basilico.backend.payment.stripe.StripeLineItem;
import com.basilico.backend.payment.stripe.StripeWebhookEventData;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PaymentApiTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private MenuItemRepository menuItemRepository;

	@Autowired
	private CustomerOrderRepository orderRepository;

	@Autowired
	private PaymentAttemptRepository paymentAttemptRepository;

	@Autowired
	private CustomerNotificationRepository notificationRepository;

	@Autowired
	private FulfilmentSettingsRepository fulfilmentSettingsRepository;

	@Autowired
	private DeliveryPostcodeRuleRepository postcodeRuleRepository;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@PersistenceContext
	private EntityManager entityManager;

	@MockitoBean
	private StripeCheckoutGateway stripeCheckoutGateway;

	@MockitoBean
	private PostcodeGeocoder postcodeGeocoder;

	@MockitoBean
	private RouteDurationProvider routeDurationProvider;

	@BeforeEach
	void resetMocks() {
		reset(stripeCheckoutGateway, postcodeGeocoder, routeDurationProvider);
		when(routeDurationProvider.estimateDrivingDuration(any(), any())).thenReturn(Optional.empty());
	}

	@Test
	void createCheckoutSessionRejectsUnknownOrder() throws Exception {
		mockMvc.perform(post("/api/payments/checkout-session")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"orderReference\":\"BAS-20991231-MISSING\"}"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.error").value("NOT_FOUND"));

		verifyNoInteractions(stripeCheckoutGateway);
	}

	@Test
	void createCheckoutSessionRejectsAlreadyPaidOrder() throws Exception {
		String orderReference = createSingleItemOrder("pizza-margherita", 1);
		CustomerOrder order = order(orderReference);
		order.setPaymentStatus(PaymentStatus.PAID);
		orderRepository.saveAndFlush(order);

		mockMvc.perform(post("/api/payments/checkout-session")
						.contentType(MediaType.APPLICATION_JSON)
						.content(checkoutPayload(orderReference)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value("This order has already been paid."));

		verifyNoInteractions(stripeCheckoutGateway);
	}

	@Test
	void createCheckoutSessionRejectsCancelledOrder() throws Exception {
		String orderReference = createSingleItemOrder("pizza-margherita", 1);
		CustomerOrder order = order(orderReference);
		order.setStatus(OrderStatus.CANCELLED);
		orderRepository.saveAndFlush(order);

		mockMvc.perform(post("/api/payments/checkout-session")
						.contentType(MediaType.APPLICATION_JSON)
						.content(checkoutPayload(orderReference)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.error").value("CONFLICT"));

		verifyNoInteractions(stripeCheckoutGateway);
	}

	@Test
	void checkoutSessionAmountComesFromDatabaseSnapshots() throws Exception {
		String orderReference = createTwoItemOrder();
		when(stripeCheckoutGateway.createCheckoutSession(any()))
				.thenReturn(openSession("cs_test_amount", orderReference, 1998));

		mockMvc.perform(post("/api/payments/checkout-session")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
				{
				  "orderReference": "%s",
				  "amountPence": 1,
				  "subtotalPence": 1
				}
				""".formatted(orderReference)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.amountPence").value(1998))
				.andExpect(jsonPath("$.currency").value("gbp"))
				.andExpect(jsonPath("$.checkoutSessionClientSecret").value("cs_test_amount_secret"));

		ArgumentCaptor<StripeCheckoutSessionCreateCommand> captor =
				ArgumentCaptor.forClass(StripeCheckoutSessionCreateCommand.class);
		verify(stripeCheckoutGateway).createCheckoutSession(captor.capture());

		StripeCheckoutSessionCreateCommand command = captor.getValue();
		int stripeLineTotal = command.lineItems()
				.stream()
				.mapToInt(item -> item.unitAmountPence() * item.quantity())
				.sum();

		assertThat(stripeLineTotal).isEqualTo(1998);
		assertThat(command.metadata()).containsEntry("order_reference", orderReference);
		assertThat(command.metadata()).containsKey("internal_order_id");
		assertThat(command.metadata()).doesNotContainKeys("customer_email", "customer_phone");
	}

	@Test
	void deliveryCheckoutSessionUsesOrderTotalAndIncludesDeliveryLineItem() throws Exception {
		enableDelivery(250, null, null, "DT1");
		String orderReference = createSingleDeliveryItemOrder("pizza-margherita", 1, "DT1 1TT");
		when(stripeCheckoutGateway.createCheckoutSession(any()))
				.thenReturn(openSession("cs_test_delivery_amount", orderReference, 1149));

		mockMvc.perform(post("/api/payments/checkout-session")
						.contentType(MediaType.APPLICATION_JSON)
						.content(checkoutPayload(orderReference)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.amountPence").value(1149));

		ArgumentCaptor<StripeCheckoutSessionCreateCommand> captor =
				ArgumentCaptor.forClass(StripeCheckoutSessionCreateCommand.class);
		verify(stripeCheckoutGateway).createCheckoutSession(captor.capture());

		StripeCheckoutSessionCreateCommand command = captor.getValue();
		assertThat(command.lineItems()).extracting(StripeLineItem::name)
				.containsExactly("PIZZA MARGHERITA", "Delivery");
		assertThat(command.lineItems()
				.stream()
				.mapToInt(item -> item.unitAmountPence() * item.quantity())
				.sum()).isEqualTo(1149);

		PaymentAttempt attempt = paymentAttemptRepository.findAll().stream()
				.filter(candidate -> candidate.getOrder().getOrderReference().equals(orderReference))
				.findFirst()
				.orElseThrow();
		assertThat(attempt.getAmountPence()).isEqualTo(1149);
	}

	@Test
	void radiusDeliveryCheckoutSessionUsesDynamicOrderTotalAndDeliveryLineItem() throws Exception {
		enableRadiusDelivery();
		mockPostcode("DT4 4AA", 4.4);
		String orderReference = createSingleDeliveryItemOrder("pizza-margherita", 1, "DT4 4AA");
		when(stripeCheckoutGateway.createCheckoutSession(any()))
				.thenReturn(openSession("cs_test_radius_delivery_amount", orderReference, 1299));

		mockMvc.perform(post("/api/payments/checkout-session")
						.contentType(MediaType.APPLICATION_JSON)
						.content(checkoutPayload(orderReference)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.amountPence").value(1299));

		ArgumentCaptor<StripeCheckoutSessionCreateCommand> captor =
				ArgumentCaptor.forClass(StripeCheckoutSessionCreateCommand.class);
		verify(stripeCheckoutGateway).createCheckoutSession(captor.capture());

		StripeCheckoutSessionCreateCommand command = captor.getValue();
		assertThat(command.lineItems()).extracting(StripeLineItem::name)
				.containsExactly("PIZZA MARGHERITA", "Delivery");
		assertThat(command.lineItems()
				.stream()
				.mapToInt(item -> item.unitAmountPence() * item.quantity())
				.sum()).isEqualTo(1299);

		PaymentAttempt attempt = paymentAttemptRepository.findAll().stream()
				.filter(candidate -> candidate.getOrder().getOrderReference().equals(orderReference))
				.findFirst()
				.orElseThrow();
		assertThat(attempt.getAmountPence()).isEqualTo(1299);
	}

	@Test
	void freeDeliveryCheckoutSessionUsesSubtotalAndOmitsZeroDeliveryLineItem() throws Exception {
		enableDelivery(250, null, 899, "DT1");
		String orderReference = createSingleDeliveryItemOrder("pizza-margherita", 1, "DT1 1TT");
		when(stripeCheckoutGateway.createCheckoutSession(any()))
				.thenReturn(openSession("cs_test_free_delivery", orderReference, 899));

		mockMvc.perform(post("/api/payments/checkout-session")
						.contentType(MediaType.APPLICATION_JSON)
						.content(checkoutPayload(orderReference)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.amountPence").value(899));

		ArgumentCaptor<StripeCheckoutSessionCreateCommand> captor =
				ArgumentCaptor.forClass(StripeCheckoutSessionCreateCommand.class);
		verify(stripeCheckoutGateway).createCheckoutSession(captor.capture());

		assertThat(captor.getValue().lineItems()).hasSize(1);
		assertThat(captor.getValue().lineItems().getFirst().name()).isEqualTo("PIZZA MARGHERITA");
	}

	@Test
	void mismatchedPersistedSnapshotTotalRejectsCheckoutSession() throws Exception {
		String orderReference = createSingleItemOrder("pizza-margherita", 1);
		Long orderId = order(orderReference).getId();
		jdbcTemplate.update("update order_items set line_total_pence = line_total_pence + 1 where order_id = ?",
				orderId);
		entityManager.clear();

		mockMvc.perform(post("/api/payments/checkout-session")
						.contentType(MediaType.APPLICATION_JSON)
						.content(checkoutPayload(orderReference)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value("Order total could not be verified."));

		verifyNoInteractions(stripeCheckoutGateway);
	}

	@Test
	void mismatchedPersistedPayableTotalRejectsCheckoutSession() throws Exception {
		String orderReference = createSingleItemOrder("pizza-margherita", 1);
		Long orderId = order(orderReference).getId();
		jdbcTemplate.update("update orders set total_pence = total_pence + 1 where id = ?", orderId);
		entityManager.clear();

		mockMvc.perform(post("/api/payments/checkout-session")
						.contentType(MediaType.APPLICATION_JSON)
						.content(checkoutPayload(orderReference)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value("Order total could not be verified."));

		verifyNoInteractions(stripeCheckoutGateway);
	}

	@Test
	void invalidWebhookSignatureIsRejected() throws Exception {
		when(stripeCheckoutGateway.constructWebhookEvent(anyString(), anyString()))
				.thenThrow(new BadRequestException("Invalid Stripe webhook signature"));

		mockMvc.perform(post("/api/payments/stripe/webhook")
						.contentType(MediaType.APPLICATION_JSON)
						.header("Stripe-Signature", "bad-signature")
						.content("{}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Invalid Stripe webhook signature"));
	}

	@Test
	void completedWebhookMarksPendingOrderPaidAndNew() throws Exception {
		String orderReference = createPaidWebhookReadyOrder("cs_test_completed", 899);
		when(stripeCheckoutGateway.constructWebhookEvent(anyString(), anyString()))
				.thenReturn(completedEvent("evt_completed", "cs_test_completed", orderReference, 899));

		mockMvc.perform(post("/api/payments/stripe/webhook")
						.contentType(MediaType.APPLICATION_JSON)
						.header("Stripe-Signature", "valid-signature")
						.content("{}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("processed"));

		CustomerOrder order = order(orderReference);
		PaymentAttempt attempt = paymentAttemptRepository.findByStripeCheckoutSessionId("cs_test_completed")
				.orElseThrow();

		assertThat(order.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
		assertThat(order.getStatus()).isEqualTo(OrderStatus.NEW);
		assertThat(attempt.getStatus()).isEqualTo(PaymentAttemptStatus.PAID);
		assertThat(attempt.getStripePaymentIntentId()).isEqualTo("pi_test_completed");
		assertThat(attempt.getCompletedAt()).isNotNull();
		assertThat(notificationRepository.findAll())
				.filteredOn(notification -> notification.getNotificationType() == NotificationType.ORDER_RECEIVED)
				.hasSize(1);
	}

	@Test
	void completedWebhookDoesNotRegressAcceptedOrderToNew() throws Exception {
		String orderReference = createPaidWebhookReadyOrder("cs_test_accepted", 899);
		CustomerOrder order = order(orderReference);
		order.setStatus(OrderStatus.ACCEPTED);
		orderRepository.saveAndFlush(order);

		when(stripeCheckoutGateway.constructWebhookEvent(anyString(), anyString()))
				.thenReturn(completedEvent("evt_accepted", "cs_test_accepted", orderReference, 899));

		mockMvc.perform(post("/api/payments/stripe/webhook")
						.contentType(MediaType.APPLICATION_JSON)
						.header("Stripe-Signature", "valid-signature")
						.content("{}"))
				.andExpect(status().isOk());

		assertThat(order(orderReference).getStatus()).isEqualTo(OrderStatus.ACCEPTED);
		assertThat(order(orderReference).getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
	}

	@Test
	void duplicateWebhookEventIsIdempotent() throws Exception {
		String orderReference = createPaidWebhookReadyOrder("cs_test_duplicate", 899);
		when(stripeCheckoutGateway.constructWebhookEvent(anyString(), anyString()))
				.thenReturn(completedEvent("evt_duplicate", "cs_test_duplicate", orderReference, 899));

		mockMvc.perform(post("/api/payments/stripe/webhook")
						.contentType(MediaType.APPLICATION_JSON)
						.header("Stripe-Signature", "valid-signature")
						.content("{}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("processed"));

		mockMvc.perform(post("/api/payments/stripe/webhook")
						.contentType(MediaType.APPLICATION_JSON)
						.header("Stripe-Signature", "valid-signature")
						.content("{}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("duplicate"));

		assertThat(order(orderReference).getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
		assertThat(notificationRepository.findAll())
				.filteredOn(notification -> notification.getNotificationType() == NotificationType.ORDER_RECEIVED)
				.hasSize(1);
	}

	@Test
	void expiredCheckoutSessionLeavesOrderUnpaidAndPendingPayment() throws Exception {
		String orderReference = createPaidWebhookReadyOrder("cs_test_expired", 899);
		when(stripeCheckoutGateway.constructWebhookEvent(anyString(), anyString()))
				.thenReturn(new StripeWebhookEventData(
						"evt_expired",
						"checkout.session.expired",
						new StripeCheckoutSessionData(
								"cs_test_expired",
								null,
								"expired",
								"unpaid",
								899,
								"gbp",
								null,
								orderReference
						)
				));

		mockMvc.perform(post("/api/payments/stripe/webhook")
						.contentType(MediaType.APPLICATION_JSON)
						.header("Stripe-Signature", "valid-signature")
						.content("{}"))
				.andExpect(status().isOk());

		CustomerOrder order = order(orderReference);
		PaymentAttempt attempt = paymentAttemptRepository.findByStripeCheckoutSessionId("cs_test_expired")
				.orElseThrow();

		assertThat(order.getPaymentStatus()).isEqualTo(PaymentStatus.UNPAID);
		assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING_PAYMENT);
		assertThat(attempt.getStatus()).isEqualTo(PaymentAttemptStatus.EXPIRED);
	}

	@Test
	void publicPaymentStatusResponseExposesNoCustomerPii() throws Exception {
		String orderReference = createPaidWebhookReadyOrder("cs_test_status", 899);
		when(stripeCheckoutGateway.retrieveCheckoutSession("cs_test_status"))
				.thenReturn(openSession("cs_test_status", orderReference, 899));

		MvcResult result = mockMvc.perform(get("/api/payments/checkout-session/{sessionId}/status",
						"cs_test_status"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.orderReference").value(orderReference))
				.andExpect(jsonPath("$.paymentStatus").value("UNPAID"))
				.andExpect(jsonPath("$.orderStatus").value("PENDING_PAYMENT"))
				.andExpect(jsonPath("$.checkoutSessionStatus").value("open"))
				.andExpect(jsonPath("$.checkoutSessionPaymentStatus").value("unpaid"))
				.andExpect(jsonPath("$.customer").doesNotExist())
				.andExpect(jsonPath("$.items").doesNotExist())
				.andReturn();

		String responseBody = result.getResponse().getContentAsString();
		assertThat(responseBody).doesNotContain("John", "Smith", "07123456789", "john@example.com");
	}

	@Test
	void publicPaymentStatusDoesNotMarkOrderPaidWithoutWebhook() throws Exception {
		String orderReference = createPaidWebhookReadyOrder("cs_test_complete_status", 899);
		when(stripeCheckoutGateway.retrieveCheckoutSession("cs_test_complete_status"))
				.thenReturn(new StripeCheckoutSessionData(
						"cs_test_complete_status",
						null,
						"complete",
						"paid",
						899,
						"gbp",
						"pi_test_complete_status",
						orderReference
				));

		mockMvc.perform(get("/api/payments/checkout-session/{sessionId}/status",
						"cs_test_complete_status"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.paymentStatus").value("UNPAID"))
				.andExpect(jsonPath("$.orderStatus").value("PENDING_PAYMENT"))
				.andExpect(jsonPath("$.paymentAttemptStatus").value("OPEN"))
				.andExpect(jsonPath("$.checkoutSessionStatus").value("complete"))
				.andExpect(jsonPath("$.checkoutSessionPaymentStatus").value("paid"));

		CustomerOrder order = order(orderReference);
		PaymentAttempt attempt = paymentAttemptRepository.findByStripeCheckoutSessionId("cs_test_complete_status")
				.orElseThrow();

		assertThat(order.getPaymentStatus()).isEqualTo(PaymentStatus.UNPAID);
		assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING_PAYMENT);
		assertThat(attempt.getStatus()).isEqualTo(PaymentAttemptStatus.OPEN);
		assertThat(attempt.getStripePaymentIntentId()).isNull();
	}

	@Test
	void publicPaymentStatusMarksExpiredAttemptButLeavesOrderUnpaid() throws Exception {
		String orderReference = createPaidWebhookReadyOrder("cs_test_expired_status", 899);
		when(stripeCheckoutGateway.retrieveCheckoutSession("cs_test_expired_status"))
				.thenReturn(new StripeCheckoutSessionData(
						"cs_test_expired_status",
						null,
						"expired",
						"unpaid",
						899,
						"gbp",
						null,
						orderReference
				));

		mockMvc.perform(get("/api/payments/checkout-session/{sessionId}/status",
						"cs_test_expired_status"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.paymentStatus").value("UNPAID"))
				.andExpect(jsonPath("$.orderStatus").value("PENDING_PAYMENT"))
				.andExpect(jsonPath("$.paymentAttemptStatus").value("EXPIRED"))
				.andExpect(jsonPath("$.checkoutSessionStatus").value("expired"))
				.andExpect(jsonPath("$.checkoutSessionPaymentStatus").value("unpaid"));

		CustomerOrder order = order(orderReference);
		PaymentAttempt attempt = paymentAttemptRepository.findByStripeCheckoutSessionId("cs_test_expired_status")
				.orElseThrow();

		assertThat(order.getPaymentStatus()).isEqualTo(PaymentStatus.UNPAID);
		assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING_PAYMENT);
		assertThat(attempt.getStatus()).isEqualTo(PaymentAttemptStatus.EXPIRED);
	}

	@Test
	void manualPaymentStatusControlIsDisabledByDefault() throws Exception {
		String orderReference = createSingleItemOrder("pizza-margherita", 1);
		Long orderId = order(orderReference).getId();

		mockMvc.perform(patch("/api/admin/orders/{id}/payment-status", orderId)
						.with(adminUser())
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"paymentStatus\":\"PAID\"}"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value("Manual payment status control is disabled."));
	}

	@Test
	void adminSecurityStillRequiresAuthentication() throws Exception {
		mockMvc.perform(get("/api/admin/orders"))
				.andExpect(status().isUnauthorized());
	}

	private String createPaidWebhookReadyOrder(String sessionId, int amountPence) throws Exception {
		String orderReference = createSingleItemOrder("pizza-margherita", 1);
		PaymentAttempt attempt = paymentAttemptRepository.saveAndFlush(
				new PaymentAttempt(order(orderReference), amountPence)
		);
		attempt.setStripeCheckoutSessionId(sessionId);
		attempt.setStatus(PaymentAttemptStatus.OPEN);
		return orderReference;
	}

	private StripeWebhookEventData completedEvent(String eventId, String sessionId, String orderReference,
			int amountPence) {
		return new StripeWebhookEventData(
				eventId,
				"checkout.session.completed",
				new StripeCheckoutSessionData(
						sessionId,
						null,
						"complete",
						"paid",
						amountPence,
						"gbp",
						"pi_test_completed",
						orderReference
				)
		);
	}

	private StripeCheckoutSessionData openSession(String sessionId, String orderReference, int amountPence) {
		return new StripeCheckoutSessionData(
				sessionId,
				sessionId + "_secret",
				"open",
				"unpaid",
				amountPence,
				"gbp",
				null,
				orderReference
		);
	}

	private String createTwoItemOrder() throws Exception {
		MenuItem margherita = item("pizza-margherita");
		MenuItem lasagna = item("lasagna");

		return createOrder("""
				[
				  { "type": "MENU_ITEM", "menuItemId": %d, "quantity": 1 },
				  { "type": "MENU_ITEM", "menuItemId": %d, "quantity": 1 }
				]
				""".formatted(margherita.getId(), lasagna.getId()));
	}

	private String createSingleItemOrder(String slug, int quantity) throws Exception {
		MenuItem item = item(slug);
		return createOrder("""
				[
				  { "type": "MENU_ITEM", "menuItemId": %d, "quantity": %d }
				]
				""".formatted(item.getId(), quantity));
	}

	private String createSingleDeliveryItemOrder(String slug, int quantity, String postcode) throws Exception {
		MenuItem item = item(slug);
		String response = mockMvc.perform(post("/api/orders")
						.contentType(MediaType.APPLICATION_JSON)
						.content(deliveryOrderPayload("""
								[
								  { "type": "MENU_ITEM", "menuItemId": %d, "quantity": %d }
								]
								""".formatted(item.getId(), quantity), postcode)))
				.andExpect(status().isCreated())
				.andReturn()
				.getResponse()
				.getContentAsString();

		return JsonPath.read(response, "$.orderReference");
	}

	private String createOrder(String itemsJson) throws Exception {
		String response = mockMvc.perform(post("/api/orders")
						.contentType(MediaType.APPLICATION_JSON)
						.content(collectionOrderPayload(itemsJson)))
				.andExpect(status().isCreated())
				.andReturn()
				.getResponse()
				.getContentAsString();

		return JsonPath.read(response, "$.orderReference");
	}

	private String collectionOrderPayload(String itemsJson) {
		return """
				{
				  "fulfilmentType": "COLLECTION",
				  "customer": {
				    "firstName": "John",
				    "lastName": "Smith",
				    "phone": "07123456789",
				    "email": "john@example.com"
				  },
				  "timing": {
				    "type": "ASAP"
				  },
				  "items": %s
				}
				""".formatted(itemsJson);
	}

	private String deliveryOrderPayload(String itemsJson, String postcode) {
		return """
				{
				  "fulfilmentType": "DELIVERY",
				  "customer": {
				    "firstName": "John",
				    "lastName": "Smith",
				    "phone": "07123456789",
				    "email": "john@example.com"
				  },
				  "deliveryAddress": {
				    "line1": "41 Example Street",
				    "city": "Dorchester",
				    "postcode": "%s"
				  },
				  "timing": {
				    "type": "ASAP"
				  },
				  "items": %s
				}
				""".formatted(postcode, itemsJson);
	}

	private void enableDelivery(int deliveryFeePence, Integer minimumDeliveryOrderPence,
			Integer freeDeliveryThresholdPence, String postcodePattern) {
		FulfilmentSettings settings = fulfilmentSettingsRepository.findFirstByOrderByIdAsc().orElseThrow();
		settings.setDeliveryAreaMode(DeliveryAreaMode.POSTCODE_RULES);
		settings.setDeliveryPricingMode(DeliveryPricingMode.FLAT_FEE);
		settings.setDeliveryFeePence(deliveryFeePence);
		settings.setMinimumDeliveryOrderPence(minimumDeliveryOrderPence);
		settings.setFreeDeliveryThresholdPence(freeDeliveryThresholdPence);
		settings.setDeliveryEnabled(true);
		fulfilmentSettingsRepository.saveAndFlush(settings);
		postcodeRuleRepository.saveAndFlush(new DeliveryPostcodeRule(postcodePattern, 1));
	}

	private void enableRadiusDelivery() {
		FulfilmentSettings settings = fulfilmentSettingsRepository.findFirstByOrderByIdAsc().orElseThrow();
		settings.setDeliveryEnabled(true);
		settings.setDeliveryAreaMode(DeliveryAreaMode.RADIUS);
		settings.setRestaurantPostcode("DT1 1TT");
		settings.setRestaurantLatitude(new BigDecimal("50.714050"));
		settings.setRestaurantLongitude(new BigDecimal("-2.438190"));
		settings.setDeliveryRadiusMiles(new BigDecimal("6.00"));
		settings.setPreparationTimeMinutes(20);
		settings.setDeliveryPricingMode(DeliveryPricingMode.RADIUS_BANDS);
		settings.setBaseDeliveryRadiusMiles(new BigDecimal("3.00"));
		settings.setBaseDeliveryFeePence(200);
		settings.setExtraMileFeePence(100);
		settings.setMinimumDeliveryOrderPence(null);
		settings.setFreeDeliveryThresholdPence(null);
		fulfilmentSettingsRepository.saveAndFlush(settings);
	}

	private void mockPostcode(String postcode, double distanceMiles) {
		when(postcodeGeocoder.geocode(postcode))
				.thenReturn(Optional.of(new GeocodedPostcode(postcode, pointEast(distanceMiles))));
	}

	private GeoCoordinates pointEast(double miles) {
		GeoCoordinates basilico = new GeoCoordinates(50.71405, -2.43819);
		double longitudeDelta = miles / (69.172 * Math.cos(Math.toRadians(basilico.latitude())));
		return new GeoCoordinates(basilico.latitude(), basilico.longitude() + longitudeDelta);
	}

	private String checkoutPayload(String orderReference) {
		return """
				{
				  "orderReference": "%s"
				}
				""".formatted(orderReference);
	}

	private MenuItem item(String slug) {
		return menuItemRepository.findBySlug(slug).orElseThrow();
	}

	private CustomerOrder order(String orderReference) {
		return orderRepository.findDetailedByOrderReference(orderReference).orElseThrow();
	}

	private org.springframework.test.web.servlet.request.RequestPostProcessor adminUser() {
		return user("owner@basilico.test").roles("OWNER");
	}
}
