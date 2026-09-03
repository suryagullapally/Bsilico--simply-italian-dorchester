package com.basilico.backend.notification;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionTemplate;

import com.basilico.backend.booking.repository.BookingRepository;
import com.basilico.backend.notification.repository.CustomerNotificationRepository;
import com.basilico.backend.notification.service.CustomerNotificationService;
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
				new RestaurantNotificationProperties("", "https://admin.basilicodorchester.co.uk")
		);

		service.queueRestaurantNewOrderAlert(order);

		verifyNoInteractions(notificationRepository, templateService, deliveryService);
	}
}
