package com.basilico.backend.notification.service;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "basilico.notifications.email")
public record EmailNotificationProperties(
		String fromEmail,
		String fromName,
		int maxAttempts,
		int batchSize,
		boolean workerEnabled,
		long retryDelayMs,
		long initialDelayMs
) {
	public EmailNotificationProperties {
		if (maxAttempts < 1) {
			maxAttempts = 3;
		}
		if (batchSize < 1) {
			batchSize = 10;
		}
	}
}
