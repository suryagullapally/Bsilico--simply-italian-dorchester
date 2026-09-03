package com.basilico.backend.notification.dto;

public record ProcessPendingNotificationsResponse(
		String status,
		int processedCount
) {
}
