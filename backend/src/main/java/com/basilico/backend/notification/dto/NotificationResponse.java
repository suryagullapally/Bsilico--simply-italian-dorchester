package com.basilico.backend.notification.dto;

import java.time.OffsetDateTime;

import com.basilico.backend.notification.entity.NotificationChannel;
import com.basilico.backend.notification.entity.NotificationStatus;
import com.basilico.backend.notification.entity.NotificationType;

public record NotificationResponse(
		Long id,
		NotificationChannel channel,
		NotificationType notificationType,
		Long orderId,
		String orderReference,
		Long bookingId,
		String bookingReference,
		String recipientEmail,
		String recipientName,
		String subject,
		String bodyText,
		NotificationStatus status,
		int attemptCount,
		String lastError,
		OffsetDateTime createdAt,
		OffsetDateTime updatedAt,
		OffsetDateTime sentAt
) {
}
