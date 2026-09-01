package com.basilico.backend.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Properties;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.basilico.backend.booking.entity.Booking;
import com.basilico.backend.booking.repository.BookingRepository;
import com.basilico.backend.menu.entity.MenuItem;
import com.basilico.backend.menu.repository.MenuItemRepository;
import com.basilico.backend.notification.entity.CustomerNotification;
import com.basilico.backend.notification.entity.NotificationStatus;
import com.basilico.backend.notification.entity.NotificationType;
import com.basilico.backend.notification.repository.CustomerNotificationRepository;
import com.basilico.backend.order.entity.CustomerOrder;
import com.basilico.backend.order.repository.CustomerOrderRepository;

@SpringBootTest(properties = {
		"basilico.notifications.email.worker-enabled=false",
		"management.health.mail.enabled=false"
})
@AutoConfigureMockMvc
@Transactional
class NotificationApiTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private BookingRepository bookingRepository;

	@Autowired
	private CustomerOrderRepository orderRepository;

	@Autowired
	private MenuItemRepository menuItemRepository;

	@Autowired
	private CustomerNotificationRepository notificationRepository;

	@MockitoBean
	private JavaMailSender mailSender;

	@BeforeEach
	void setUpMailSender() {
		reset(mailSender);
		doNothing().when(mailSender).send(any(MimeMessage.class));
		org.mockito.Mockito.when(mailSender.createMimeMessage())
				.thenAnswer(invocation -> new MimeMessage(Session.getInstance(new Properties())));
	}

	@Test
	void bookingCreationQueuesRequestReceivedNotification() throws Exception {
		Booking booking = createBooking();

		assertThat(notificationsOfTypeForBooking(NotificationType.BOOKING_REQUEST_RECEIVED, booking.getId()))
				.hasSize(1)
				.first()
				.extracting(CustomerNotification::getStatus)
				.isEqualTo(NotificationStatus.PENDING);
	}

	@Test
	void bookingStatusNotificationsAreDeduplicated() throws Exception {
		Booking booking = createBooking();

		mockMvc.perform(patch("/api/admin/bookings/{id}/status", booking.getId())
						.with(adminUser())
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"status\":\"CONFIRMED\"}"))
				.andExpect(status().isOk());

		mockMvc.perform(patch("/api/admin/bookings/{id}/status", booking.getId())
						.with(adminUser())
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"status\":\"CONFIRMED\"}"))
				.andExpect(status().isOk());

		assertThat(notificationsOfTypeForBooking(NotificationType.BOOKING_CONFIRMED, booking.getId())).hasSize(1);
	}

	@Test
	void bookingDeclineQueuesDeclinedNotification() throws Exception {
		Booking booking = createBooking();

		mockMvc.perform(patch("/api/admin/bookings/{id}/status", booking.getId())
						.with(adminUser())
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"status\":\"DECLINED\"}"))
				.andExpect(status().isOk());

		assertThat(notificationsOfTypeForBooking(NotificationType.BOOKING_DECLINED, booking.getId())).hasSize(1);
	}

	@Test
	void orderStatusChangesQueueOperationalEmails() throws Exception {
		CustomerOrder order = createOrder();

		updateOrderStatus(order, "ACCEPTED");
		updateOrderStatus(order, "PREPARING");
		updateOrderStatus(order, "READY");
		updateOrderStatus(order, "CANCELLED");

		assertThat(notificationsOfTypeForOrder(NotificationType.ORDER_ACCEPTED, order.getId())).hasSize(1);
		assertThat(notificationsOfTypeForOrder(NotificationType.ORDER_READY, order.getId())).hasSize(1);
		assertThat(notificationsOfTypeForOrder(NotificationType.ORDER_CANCELLED, order.getId())).hasSize(1);
		assertThat(notificationsOfTypeForOrder(NotificationType.ORDER_RECEIVED, order.getId())).isEmpty();
	}

	@Test
	void adminMessagesRequireAuthenticationAndCsrf() throws Exception {
		Booking booking = createBooking();

		mockMvc.perform(get("/api/admin/messages"))
				.andExpect(status().isUnauthorized());

		mockMvc.perform(post("/api/admin/messages/email")
						.with(adminUser())
						.contentType(MediaType.APPLICATION_JSON)
						.content(manualBookingEmailPayload(booking.getId())))
				.andExpect(status().isForbidden());
	}

	@Test
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	void manualBookingEmailSendsAndRecordsSentNotification() throws Exception {
		Booking booking = createBooking();
		setUpMailSender();

		mockMvc.perform(post("/api/admin/messages/email")
						.with(adminUser())
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(manualBookingEmailPayload(booking.getId())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.notificationType").value("MANUAL_MESSAGE"))
				.andExpect(jsonPath("$.status").value("SENT"))
				.andExpect(jsonPath("$.bookingId").value(booking.getId()))
				.andExpect(jsonPath("$.recipientEmail").value("john@example.com"));

		verify(mailSender).send(any(MimeMessage.class));
		assertThat(notificationsOfTypeForBooking(NotificationType.MANUAL_MESSAGE, booking.getId()))
				.hasSize(1)
				.first()
				.extracting(CustomerNotification::getAttemptCount)
				.isEqualTo(1);
	}

	@Test
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	void manualOrderEmailFailureIsRecordedWithoutChangingOrder() throws Exception {
		CustomerOrder order = createOrder();
		setUpMailSender();
		doThrow(new MailSendException("SMTP down")).when(mailSender).send(any(MimeMessage.class));

		mockMvc.perform(post("/api/admin/messages/email")
						.with(adminUser())
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "orderId": %d,
								  "subject": "Test message",
								  "message": "This is a Basilico local email test."
								}
								""".formatted(order.getId())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("FAILED"))
				.andExpect(jsonPath("$.lastError").isString());

		assertThat(orderRepository.findById(order.getId()).orElseThrow().getOrderReference())
				.isEqualTo(order.getOrderReference());
	}

	@Test
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	void failedNotificationCanBeRetried() throws Exception {
		Booking booking = createBooking();
		setUpMailSender();
		doThrow(new MailSendException("SMTP down")).when(mailSender).send(any(MimeMessage.class));

		String response = mockMvc.perform(post("/api/admin/messages/email")
						.with(adminUser())
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(manualBookingEmailPayload(booking.getId())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("FAILED"))
				.andReturn()
				.getResponse()
				.getContentAsString();

		Number notificationId = com.jayway.jsonpath.JsonPath.read(response, "$.id");

		reset(mailSender);
		doNothing().when(mailSender).send(any(MimeMessage.class));
		org.mockito.Mockito.when(mailSender.createMimeMessage())
				.thenAnswer(invocation -> new MimeMessage(Session.getInstance(new Properties())));

		mockMvc.perform(post("/api/admin/messages/{id}/retry", notificationId.longValue())
						.with(adminUser())
						.with(csrf()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("SENT"))
				.andExpect(jsonPath("$.attemptCount").value(2));
	}

	private void updateOrderStatus(CustomerOrder order, String status) throws Exception {
		mockMvc.perform(patch("/api/admin/orders/{id}/status", order.getId())
						.with(adminUser())
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"status\":\"%s\"}".formatted(status)))
				.andExpect(status().isOk());
	}

	private Booking createBooking() throws Exception {
		String response = mockMvc.perform(post("/api/bookings")
						.contentType(MediaType.APPLICATION_JSON)
						.content(bookingPayload()))
				.andExpect(status().isCreated())
				.andReturn()
				.getResponse()
				.getContentAsString();
		String bookingReference = com.jayway.jsonpath.JsonPath.read(response, "$.bookingReference");
		return bookingRepository.findAll()
				.stream()
				.filter(candidate -> candidate.getBookingReference().equals(bookingReference))
				.findFirst()
				.orElseThrow();
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
								    "type": "ASAP"
								  },
								  "notes": "No onions please",
								  "items": [
								    { "type": "MENU_ITEM", "menuItemId": %d, "quantity": 1 }
								  ]
								}
								""".formatted(margherita.getId())))
				.andExpect(status().isCreated())
				.andReturn()
				.getResponse()
				.getContentAsString();
		String orderReference = com.jayway.jsonpath.JsonPath.read(response, "$.orderReference");
		return orderRepository.findDetailedByOrderReference(orderReference).orElseThrow();
	}

	private String bookingPayload() {
		return """
				{
				  "date": "%s",
				  "time": "19:30",
				  "partySize": 4,
				  "firstName": "John",
				  "lastName": "Smith",
				  "phone": "07123456789",
				  "email": "john@example.com",
				  "specialRequests": "Birthday dinner"
				}
				""".formatted(nextOpenDate());
	}

	private String manualBookingEmailPayload(Long bookingId) {
		return """
				{
				  "bookingId": %d,
				  "subject": "Test message",
				  "message": "This is a Basilico local email test."
				}
				""".formatted(bookingId);
	}

	private java.util.List<CustomerNotification> notificationsOfType(NotificationType type) {
		return notificationRepository.findAll()
				.stream()
				.filter(notification -> notification.getNotificationType() == type)
				.toList();
	}

	private java.util.List<CustomerNotification> notificationsOfTypeForBooking(NotificationType type, Long bookingId) {
		return notificationRepository.findAll()
				.stream()
				.filter(notification -> notification.getNotificationType() == type)
				.filter(notification -> notification.getBooking() != null)
				.filter(notification -> notification.getBooking().getId().equals(bookingId))
				.toList();
	}

	private java.util.List<CustomerNotification> notificationsOfTypeForOrder(NotificationType type, Long orderId) {
		return notificationRepository.findAll()
				.stream()
				.filter(notification -> notification.getNotificationType() == type)
				.filter(notification -> notification.getOrder() != null)
				.filter(notification -> notification.getOrder().getId().equals(orderId))
				.toList();
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
