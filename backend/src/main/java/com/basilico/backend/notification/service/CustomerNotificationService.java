package com.basilico.backend.notification.service;

import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.basilico.backend.booking.entity.Booking;
import com.basilico.backend.booking.entity.BookingStatus;
import com.basilico.backend.booking.repository.BookingRepository;
import com.basilico.backend.common.dto.PageResponse;
import com.basilico.backend.common.error.BadRequestException;
import com.basilico.backend.common.error.ConflictException;
import com.basilico.backend.common.error.ResourceNotFoundException;
import com.basilico.backend.notification.dto.NotificationResponse;
import com.basilico.backend.notification.dto.SendManualEmailRequest;
import com.basilico.backend.notification.entity.CustomerNotification;
import com.basilico.backend.notification.entity.NotificationStatus;
import com.basilico.backend.notification.entity.NotificationType;
import com.basilico.backend.notification.repository.CustomerNotificationRepository;
import com.basilico.backend.order.entity.CustomerOrder;
import com.basilico.backend.order.entity.OrderStatus;
import com.basilico.backend.order.entity.PaymentStatus;
import com.basilico.backend.order.repository.CustomerOrderRepository;
import com.basilico.backend.payment.entity.PaymentRefund;

@Service
public class CustomerNotificationService {

	private static final Logger logger = LoggerFactory.getLogger(CustomerNotificationService.class);
	private static final int MAX_ADMIN_PAGE_SIZE = 100;
	private static final String RESTAURANT_RECIPIENT_NAME = "Basilico team";

	private final CustomerNotificationRepository notificationRepository;
	private final CustomerOrderRepository orderRepository;
	private final BookingRepository bookingRepository;
	private final NotificationTemplateService templateService;
	private final NotificationDeliveryService deliveryService;
	private final TransactionTemplate transactionTemplate;
	private final RestaurantNotificationProperties restaurantNotificationProperties;

	public CustomerNotificationService(CustomerNotificationRepository notificationRepository,
			CustomerOrderRepository orderRepository,
			BookingRepository bookingRepository,
			NotificationTemplateService templateService,
			NotificationDeliveryService deliveryService,
			TransactionTemplate transactionTemplate,
			RestaurantNotificationProperties restaurantNotificationProperties) {
		this.notificationRepository = notificationRepository;
		this.orderRepository = orderRepository;
		this.bookingRepository = bookingRepository;
		this.templateService = templateService;
		this.deliveryService = deliveryService;
		this.transactionTemplate = transactionTemplate;
		this.restaurantNotificationProperties = restaurantNotificationProperties;
	}

	public void queueOrderReceived(CustomerOrder order) {
		if (order.getStatus() != OrderStatus.NEW || order.getPaymentStatus() != PaymentStatus.PAID) {
			return;
		}
		queueAutomaticOrderNotification(NotificationType.ORDER_RECEIVED, order,
				templateService.orderReceived(order));
	}

	public void queueRestaurantNewOrderAlert(CustomerOrder order) {
		if (order.getStatus() != OrderStatus.NEW) {
			return;
		}

		if (!restaurantNotificationProperties.hasOrderAlertEmail()) {
			logger.warn("Restaurant new-order alert email is not configured; order {} was still paid and accepted.",
					order.getOrderReference());
			return;
		}

		EmailContent content = templateService.restaurantNewOrder(
				order,
				restaurantNotificationProperties.adminOrderUrl(order.getId())
		);
		queueAutomaticOrderNotification(
				NotificationType.RESTAURANT_NEW_ORDER,
				order,
				restaurantNotificationProperties.normalizedOrderAlertEmail(),
				RESTAURANT_RECIPIENT_NAME,
				content
		);
	}

	public void queueOrderRefunded(CustomerOrder order, PaymentRefund refund) {
		if (refund == null || refund.getId() == null) {
			return;
		}

		queueAutomaticOrderNotification(
				NotificationType.ORDER_REFUNDED,
				order,
				order.getCustomerEmail(),
				customerName(order.getCustomerFirstName(), order.getCustomerLastName()),
				templateService.orderRefunded(order, refund),
				NotificationType.ORDER_REFUNDED.name() + ":" + refund.getId()
		);
	}

	public void queueOrderStatusNotification(CustomerOrder order, OrderStatus previousStatus) {
		if (previousStatus == order.getStatus()) {
			return;
		}

		switch (order.getStatus()) {
			case ACCEPTED -> queueAutomaticOrderNotification(NotificationType.ORDER_ACCEPTED, order,
					templateService.orderAccepted(order));
			case READY -> queueAutomaticOrderNotification(NotificationType.ORDER_READY, order,
					templateService.orderReady(order));
			case CANCELLED -> queueAutomaticOrderNotification(NotificationType.ORDER_CANCELLED, order,
					templateService.orderCancelled(order));
			default -> {
			}
		}
	}

	public void queueBookingRequestReceived(Booking booking) {
		queueAutomaticBookingNotification(NotificationType.BOOKING_REQUEST_RECEIVED, booking,
				templateService.bookingRequestReceived(booking));
	}

	public void queueRestaurantNewBookingAlert(Booking booking) {
		if (!restaurantNotificationProperties.hasBookingAlertEmail()) {
			logger.warn("Restaurant new-booking alert email is not configured; booking {} was still saved.",
					booking.getBookingReference());
			return;
		}

		EmailContent content = templateService.restaurantNewBooking(
				booking,
				restaurantNotificationProperties.adminBookingUrl(booking.getId())
		);
		queueAutomaticBookingNotification(
				NotificationType.RESTAURANT_NEW_BOOKING,
				booking,
				restaurantNotificationProperties.normalizedBookingAlertEmail(),
				RESTAURANT_RECIPIENT_NAME,
				content
		);
	}

	public void queueBookingStatusNotification(Booking booking, BookingStatus previousStatus) {
		if (previousStatus == booking.getStatus()) {
			return;
		}

		switch (booking.getStatus()) {
			case CONFIRMED -> queueAutomaticBookingNotification(NotificationType.BOOKING_CONFIRMED, booking,
					templateService.bookingConfirmed(booking));
			case DECLINED -> queueAutomaticBookingNotification(NotificationType.BOOKING_DECLINED, booking,
					templateService.bookingDeclined(booking));
			case CANCELLED -> queueAutomaticBookingNotification(NotificationType.BOOKING_CANCELLED, booking,
					templateService.bookingCancelled(booking));
			default -> {
			}
		}
	}

	@Transactional(readOnly = true)
	public PageResponse<NotificationResponse> getAdminNotifications(NotificationStatus status,
			NotificationType type,
			Long orderId,
			Long bookingId,
			int page,
			int size) {
		int safePage = Math.max(page, 0);
		int safeSize = Math.min(Math.max(size, 1), MAX_ADMIN_PAGE_SIZE);
		PageRequest pageRequest = PageRequest.of(safePage, safeSize,
				Sort.by(Sort.Direction.DESC, "createdAt", "id"));
		return PageResponse.from(notificationRepository.findForAdmin(status, type, orderId, bookingId, pageRequest)
				.map(this::toResponse));
	}

	@Transactional(readOnly = true)
	public NotificationResponse getAdminNotification(Long id) {
		return notificationRepository.findById(id)
				.map(this::toResponse)
				.orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
	}

	public NotificationResponse sendManualEmail(SendManualEmailRequest request) {
		Long notificationId = transactionTemplate.execute(status -> createManualNotification(request));
		deliveryService.processNotification(Objects.requireNonNull(notificationId));
		return loadAdminNotificationResponse(notificationId);
	}

	public NotificationResponse retryNotification(Long id) {
		CustomerNotification notification = notificationRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
		if (notification.getStatus() != NotificationStatus.FAILED) {
			throw new ConflictException("Only failed notifications can be retried.");
		}

		deliveryService.retryNotification(id);
		return loadAdminNotificationResponse(id);
	}

	private void queueAutomaticOrderNotification(NotificationType type, CustomerOrder order, EmailContent content) {
		queueAutomaticOrderNotification(
				type,
				order,
				order.getCustomerEmail(),
				customerName(order.getCustomerFirstName(), order.getCustomerLastName()),
				content
		);
	}

	private void queueAutomaticOrderNotification(NotificationType type, CustomerOrder order,
			String recipientEmail, String recipientName, EmailContent content) {
		String deduplicationKey = type.name() + ":" + order.getId();
		queueAutomaticOrderNotification(type, order, recipientEmail, recipientName, content, deduplicationKey);
	}

	private void queueAutomaticOrderNotification(NotificationType type, CustomerOrder order,
			String recipientEmail, String recipientName, EmailContent content, String deduplicationKey) {
		if (notificationRepository.findByDeduplicationKey(deduplicationKey).isPresent()) {
			return;
		}

		CustomerNotification notification = notificationRepository.saveAndFlush(new CustomerNotification(
				type,
				order,
				recipientEmail,
				recipientName,
				content.subject(),
				content.text(),
				content.html(),
				deduplicationKey
		));
		deliverAfterCommit(notification.getId());
	}

	private void queueAutomaticBookingNotification(NotificationType type, Booking booking, EmailContent content) {
		queueAutomaticBookingNotification(
				type,
				booking,
				booking.getEmail(),
				customerName(booking.getFirstName(), booking.getLastName()),
				content
		);
	}

	private void queueAutomaticBookingNotification(NotificationType type, Booking booking,
			String recipientEmail, String recipientName, EmailContent content) {
		String deduplicationKey = type.name() + ":" + booking.getId();
		if (notificationRepository.findByDeduplicationKey(deduplicationKey).isPresent()) {
			return;
		}

		CustomerNotification notification = notificationRepository.saveAndFlush(new CustomerNotification(
				type,
				booking,
				recipientEmail,
				recipientName,
				content.subject(),
				content.text(),
				content.html(),
				deduplicationKey
		));
		deliverAfterCommit(notification.getId());
	}

	private Long createManualNotification(SendManualEmailRequest request) {
		boolean hasOrder = request.orderId() != null;
		boolean hasBooking = request.bookingId() != null;
		if (hasOrder == hasBooking) {
			throw new BadRequestException("Manual email must reference exactly one order or booking.");
		}

		EmailContent content = templateService.manualMessage(request.subject(), request.message());
		if (hasOrder) {
			CustomerOrder order = orderRepository.findById(request.orderId())
					.orElseThrow(() -> new ResourceNotFoundException("Order not found"));
			CustomerNotification notification = notificationRepository.saveAndFlush(new CustomerNotification(
					NotificationType.MANUAL_MESSAGE,
					order,
					order.getCustomerEmail(),
					customerName(order.getCustomerFirstName(), order.getCustomerLastName()),
					content.subject(),
					content.text(),
					content.html(),
					null
			));
			return notification.getId();
		}

		Booking booking = bookingRepository.findById(request.bookingId())
				.orElseThrow(() -> new ResourceNotFoundException("Booking not found"));
		CustomerNotification notification = notificationRepository.saveAndFlush(new CustomerNotification(
				NotificationType.MANUAL_MESSAGE,
				booking,
				booking.getEmail(),
				customerName(booking.getFirstName(), booking.getLastName()),
				content.subject(),
				content.text(),
				content.html(),
				null
		));
		return notification.getId();
	}

	private NotificationResponse loadAdminNotificationResponse(Long id) {
		return transactionTemplate.execute(status -> notificationRepository.findById(id)
				.map(this::toResponse)
				.orElseThrow(() -> new ResourceNotFoundException("Notification not found")));
	}

	private void deliverAfterCommit(Long notificationId) {
		if (!TransactionSynchronizationManager.isSynchronizationActive()) {
			deliveryService.processNotification(notificationId);
			return;
		}

		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				deliveryService.processNotification(notificationId);
			}
		});
	}

	private NotificationResponse toResponse(CustomerNotification notification) {
		CustomerOrder order = notification.getOrder();
		Booking booking = notification.getBooking();
		return new NotificationResponse(
				notification.getId(),
				notification.getChannel(),
				notification.getNotificationType(),
				order == null ? null : order.getId(),
				order == null ? null : order.getOrderReference(),
				booking == null ? null : booking.getId(),
				booking == null ? null : booking.getBookingReference(),
				notification.getRecipientEmail(),
				notification.getRecipientName(),
				notification.getSubject(),
				notification.getBodyText(),
				notification.getStatus(),
				notification.getAttemptCount(),
				notification.getLastError(),
				notification.getCreatedAt(),
				notification.getUpdatedAt(),
				notification.getSentAt()
		);
	}

	private String customerName(String firstName, String lastName) {
		return (firstName + " " + lastName).trim();
	}
}
