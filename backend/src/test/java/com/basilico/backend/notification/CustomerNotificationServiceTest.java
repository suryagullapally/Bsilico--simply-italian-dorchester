package com.basilico.backend.notification;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionTemplate;

import com.basilico.backend.booking.entity.Booking;
import com.basilico.backend.booking.repository.BookingRepository;
import com.basilico.backend.notification.entity.CustomerNotification;
import com.basilico.backend.notification.entity.NotificationType;
import com.basilico.backend.notification.repository.CustomerNotificationRepository;
import com.basilico.backend.notification.service.CustomerNotificationService;
import com.basilico.backend.notification.service.EmailContent;
import com.basilico.backend.notification.service.NotificationDeliveryService;
import com.basilico.backend.notification.service.NotificationTemplateService;
import com.basilico.backend.notification.service.RestaurantNotificationProperties;
import com.basilico.backend.order.entity.CustomerOrder;
import com.basilico.backend.order.entity.OrderStatus;
import com.basilico.backend.order.entity.PaymentStatus;
import com.basilico.backend.order.repository.CustomerOrderRepository;

class CustomerNotificationServiceTest {

	@Test
	void missingRestaurantAlertEmailDoesNotCreateNotificationOrFailPaidOrderFlow() {
		CustomerNotificationRepository notificationRepository = mock(CustomerNotificationRepository.class);
		NotificationTemplateService templateService = mock(NotificationTemplateService.class);
		NotificationDeliveryService deliveryService = mock(NotificationDeliveryService.class);
		CustomerOrder order = mock(CustomerOrder.class);
		when(order.getStatus()).thenReturn(OrderStatus.NEW);
		when(order.getPaymentStatus()).thenReturn(PaymentStatus.PAID);
		when(order.getOrderReference()).thenReturn("BAS-20260831-ABC123");

		CustomerNotificationService service = new CustomerNotificationService(
				notificationRepository,
				mock(CustomerOrderRepository.class),
				mock(BookingRepository.class),
				templateService,
				deliveryService,
				mock(TransactionTemplate.class),
				new RestaurantNotificationProperties("", "", "https://admin.basilicodorchester.co.uk")
		);

		service.queueRestaurantNewOrderAlert(order);

		verifyNoInteractions(notificationRepository, templateService, deliveryService);
	}

	@Test
	void missingRestaurantBookingAlertEmailDoesNotCreateNotificationOrFailBookingFlow() {
		CustomerNotificationRepository notificationRepository = mock(CustomerNotificationRepository.class);
		NotificationTemplateService templateService = mock(NotificationTemplateService.class);
		NotificationDeliveryService deliveryService = mock(NotificationDeliveryService.class);
		Booking booking = mock(Booking.class);
		when(booking.getBookingReference()).thenReturn("BKG-20260904-ABC12");

		CustomerNotificationService service = new CustomerNotificationService(
				notificationRepository,
				mock(CustomerOrderRepository.class),
				mock(BookingRepository.class),
				templateService,
				deliveryService,
				mock(TransactionTemplate.class),
				new RestaurantNotificationProperties("", "", "https://admin.basilicodorchester.co.uk")
		);

		service.queueRestaurantNewBookingAlert(booking);

		verifyNoInteractions(notificationRepository, templateService, deliveryService);
	}

	@Test
	void restaurantBookingAlertUsesConfiguredBookingRecipient() {
		CustomerNotificationRepository notificationRepository = mock(CustomerNotificationRepository.class);
		NotificationTemplateService templateService = mock(NotificationTemplateService.class);
		NotificationDeliveryService deliveryService = mock(NotificationDeliveryService.class);
		Booking booking = mock(Booking.class);
		when(booking.getId()).thenReturn(42L);
		when(templateService.restaurantNewBooking(booking,
				"https://admin.basilicodorchester.co.uk/bookings/42"))
				.thenReturn(new EmailContent("NEW TABLE BOOKING", "Body", "<p>Body</p>"));
		when(notificationRepository.findByDeduplicationKey("RESTAURANT_NEW_BOOKING:42"))
				.thenReturn(java.util.Optional.empty());
		when(notificationRepository.saveAndFlush(org.mockito.ArgumentMatchers.any(CustomerNotification.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		CustomerNotificationService service = new CustomerNotificationService(
				notificationRepository,
				mock(CustomerOrderRepository.class),
				mock(BookingRepository.class),
				templateService,
				deliveryService,
				mock(TransactionTemplate.class),
				new RestaurantNotificationProperties("", "basilico2921@gmail.com",
						"https://admin.basilicodorchester.co.uk")
		);

		service.queueRestaurantNewBookingAlert(booking);

		org.mockito.ArgumentCaptor<CustomerNotification> captor =
				org.mockito.ArgumentCaptor.forClass(CustomerNotification.class);
		verify(notificationRepository).saveAndFlush(captor.capture());
		CustomerNotification notification = captor.getValue();
		org.assertj.core.api.Assertions.assertThat(notification.getNotificationType())
				.isEqualTo(NotificationType.RESTAURANT_NEW_BOOKING);
		org.assertj.core.api.Assertions.assertThat(notification.getRecipientEmail())
				.isEqualTo("basilico2921@gmail.com");
		org.assertj.core.api.Assertions.assertThat(notification.getRecipientName())
				.isEqualTo("Basilico team");
		org.assertj.core.api.Assertions.assertThat(notification.getDeduplicationKey())
				.isEqualTo("RESTAURANT_NEW_BOOKING:42");
	}
}
