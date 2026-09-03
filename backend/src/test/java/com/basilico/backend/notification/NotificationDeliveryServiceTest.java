package com.basilico.backend.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;

import com.basilico.backend.booking.entity.Booking;
import com.basilico.backend.notification.entity.CustomerNotification;
import com.basilico.backend.notification.entity.NotificationStatus;
import com.basilico.backend.notification.entity.NotificationType;
import com.basilico.backend.notification.repository.CustomerNotificationRepository;
import com.basilico.backend.notification.service.EmailNotificationProperties;
import com.basilico.backend.notification.service.NotificationDeliveryService;

class NotificationDeliveryServiceTest {

	private CustomerNotificationRepository notificationRepository;
	private JavaMailSender mailSender;
	private NotificationDeliveryService notificationDeliveryService;

	@BeforeEach
	void setUp() {
		notificationRepository = mock(CustomerNotificationRepository.class);
		mailSender = mock(JavaMailSender.class);
		notificationDeliveryService = new NotificationDeliveryService(
				notificationRepository,
				mailSender,
				new EmailNotificationProperties(
						"orders@basilico.local",
						"Basilico - Simple Italian",
						3,
						20,
						false,
						30000,
						5000
				),
				new NoOpTransactionManager()
		);

		doNothing().when(mailSender).send(any(MimeMessage.class));
		when(mailSender.createMimeMessage())
				.thenAnswer(invocation -> new MimeMessage(Session.getInstance(new Properties())));
	}

	@Test
	void processPendingBatchProcessesOnlyPendingAndFailedNotificationsUnderMaxAttempts() {
		CustomerNotification pending = notification(NotificationStatus.PENDING, 0);
		CustomerNotification failed = notification(NotificationStatus.FAILED, 2);
		CustomerNotification sent = notification(NotificationStatus.SENT, 0);
		CustomerNotification exhausted = notification(NotificationStatus.FAILED, 3);
		stubBatch(Map.of(
				1L, pending,
				2L, failed,
				3L, sent,
				4L, exhausted
		));

		int processedCount = notificationDeliveryService.processPendingBatch();

		assertThat(processedCount).isEqualTo(2);
		verify(mailSender, times(2)).send(any(MimeMessage.class));
		assertThat(pending.getStatus()).isEqualTo(NotificationStatus.SENT);
		assertThat(pending.getAttemptCount()).isEqualTo(1);
		assertThat(failed.getStatus()).isEqualTo(NotificationStatus.SENT);
		assertThat(failed.getAttemptCount()).isEqualTo(3);
		assertThat(sent.getStatus()).isEqualTo(NotificationStatus.SENT);
		assertThat(sent.getAttemptCount()).isZero();
		assertThat(exhausted.getStatus()).isEqualTo(NotificationStatus.FAILED);
		assertThat(exhausted.getAttemptCount()).isEqualTo(3);
	}

	@Test
	void failedRetryIncrementsAttemptCountAndRemainsFailed() {
		CustomerNotification pending = notification(NotificationStatus.PENDING, 0);
		stubBatch(Map.of(1L, pending));
		doThrow(new MailSendException("SMTP unavailable")).when(mailSender).send(any(MimeMessage.class));

		int processedCount = notificationDeliveryService.processPendingBatch();

		assertThat(processedCount).isEqualTo(1);
		assertThat(pending.getStatus()).isEqualTo(NotificationStatus.FAILED);
		assertThat(pending.getAttemptCount()).isEqualTo(1);
		assertThat(pending.getLastError()).contains("SMTP unavailable");
	}

	@Test
	void repeatedBatchDoesNotResendAlreadySentNotification() {
		CustomerNotification pending = notification(NotificationStatus.PENDING, 0);
		stubBatch(Map.of(1L, pending));

		assertThat(notificationDeliveryService.processPendingBatch()).isEqualTo(1);
		verify(mailSender, times(1)).send(any(MimeMessage.class));

		reset(mailSender);

		assertThat(notificationDeliveryService.processPendingBatch()).isZero();
		verifyNoInteractions(mailSender);
		assertThat(pending.getStatus()).isEqualTo(NotificationStatus.SENT);
		assertThat(pending.getAttemptCount()).isEqualTo(1);
	}

	private void stubBatch(Map<Long, CustomerNotification> notifications) {
		when(notificationRepository.findProcessableIds(
				eq(List.of(NotificationStatus.PENDING, NotificationStatus.FAILED)),
				eq(3),
				any(Pageable.class)))
				.thenReturn(List.copyOf(notifications.keySet()));
		notifications.forEach((id, notification) -> when(notificationRepository.findByIdForUpdate(id))
				.thenReturn(Optional.of(notification)));
	}

	private CustomerNotification notification(NotificationStatus status, int attempts) {
		CustomerNotification notification = new CustomerNotification(
				NotificationType.MANUAL_MESSAGE,
				new Booking(
						"BKG-RTY-" + System.nanoTime(),
						LocalDate.now().plusDays(1),
						LocalTime.of(19, 30),
						4,
						"John",
						"Smith",
						"07123456789",
						"john@example.com"
				),
				"john@example.com",
				"John Smith",
				"Retry test",
				"Retry test body",
				null,
				null
		);
		notification.setStatus(status);
		for (int i = 0; i < attempts; i++) {
			notification.incrementAttemptCount();
		}
		return notification;
	}

	private static class NoOpTransactionManager implements PlatformTransactionManager {

		@Override
		public TransactionStatus getTransaction(TransactionDefinition definition) throws TransactionException {
			return new SimpleTransactionStatus();
		}

		@Override
		public void commit(TransactionStatus status) throws TransactionException {
		}

		@Override
		public void rollback(TransactionStatus status) throws TransactionException {
		}
	}
}
