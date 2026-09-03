package com.basilico.backend.notification.service;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.List;

import jakarta.mail.internet.MimeMessage;

import org.springframework.data.domain.PageRequest;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import com.basilico.backend.notification.entity.CustomerNotification;
import com.basilico.backend.notification.entity.NotificationStatus;
import com.basilico.backend.notification.repository.CustomerNotificationRepository;

@Service
public class NotificationDeliveryService {

	private static final int ERROR_MESSAGE_LIMIT = 1000;

	private final CustomerNotificationRepository notificationRepository;
	private final JavaMailSender mailSender;
	private final EmailNotificationProperties properties;
	private final TransactionTemplate transactionTemplate;

	public NotificationDeliveryService(CustomerNotificationRepository notificationRepository,
			JavaMailSender mailSender,
			EmailNotificationProperties properties,
			PlatformTransactionManager transactionManager) {
		this.notificationRepository = notificationRepository;
		this.mailSender = mailSender;
		this.properties = properties;
		this.transactionTemplate = new TransactionTemplate(transactionManager);
		this.transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
	}

	public boolean processNotification(Long notificationId) {
		return processNotification(notificationId, false);
	}

	public boolean retryNotification(Long notificationId) {
		return processNotification(notificationId, true);
	}

	public int processPendingBatch() {
		List<Long> notificationIds = notificationRepository.findProcessableIds(
				List.of(NotificationStatus.PENDING, NotificationStatus.FAILED),
				properties.maxAttempts(),
				PageRequest.of(0, properties.batchSize())
		);
		int processedCount = 0;
		for (Long notificationId : notificationIds) {
			if (processNotification(notificationId)) {
				processedCount++;
			}
		}
		return processedCount;
	}

	private boolean processNotification(Long notificationId, boolean forceRetry) {
		Boolean processed = transactionTemplate.execute(status -> {
			CustomerNotification notification = notificationRepository.findByIdForUpdate(notificationId)
					.orElse(null);
			if (notification == null || !canSend(notification, forceRetry)) {
				return false;
			}

			notification.setStatus(NotificationStatus.SENDING);
			notification.incrementAttemptCount();
			notification.setLastError(null);
			notificationRepository.flush();

			try {
				sendEmail(notification);
				notification.setStatus(NotificationStatus.SENT);
				notification.setSentAt(OffsetDateTime.now());
				notification.setLastError(null);
			}
			catch (RuntimeException exception) {
				notification.setStatus(NotificationStatus.FAILED);
				notification.setLastError(cleanErrorMessage(exception));
			}
			return true;
		});
		return Boolean.TRUE.equals(processed);
	}

	private boolean canSend(CustomerNotification notification, boolean forceRetry) {
		if (notification.getStatus() == NotificationStatus.SENT
				|| notification.getStatus() == NotificationStatus.SENDING) {
			return false;
		}

		if (forceRetry) {
			return notification.getStatus() == NotificationStatus.FAILED;
		}

		return notification.getAttemptCount() < properties.maxAttempts();
	}

	private void sendEmail(CustomerNotification notification) {
		if (properties.fromEmail() == null || properties.fromEmail().isBlank()) {
			throw new IllegalStateException("Email sender is not configured.");
		}

		try {
			MimeMessage message = mailSender.createMimeMessage();
			MimeMessageHelper helper = new MimeMessageHelper(
					message,
					true,
					StandardCharsets.UTF_8.name()
			);
			helper.setFrom(properties.fromEmail(), properties.fromName());
			helper.setTo(notification.getRecipientEmail());
			helper.setSubject(notification.getSubject());
			helper.setText(notification.getBodyText(), notification.getBodyHtml() != null
					? notification.getBodyHtml()
					: notification.getBodyText());
			mailSender.send(message);
		}
		catch (MailException exception) {
			throw exception;
		}
		catch (Exception exception) {
			throw new IllegalStateException("Email could not be prepared.", exception);
		}
	}

	private String cleanErrorMessage(RuntimeException exception) {
		String message = exception.getMessage();
		if (message == null || message.isBlank()) {
			message = exception.getClass().getSimpleName();
		}
		message = message.replaceAll("\\s+", " ").trim();
		if (message.length() > ERROR_MESSAGE_LIMIT) {
			return message.substring(0, ERROR_MESSAGE_LIMIT);
		}
		return message;
	}
}
