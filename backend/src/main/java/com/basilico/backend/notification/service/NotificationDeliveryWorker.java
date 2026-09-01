package com.basilico.backend.notification.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "basilico.notifications.email", name = "worker-enabled",
		havingValue = "true", matchIfMissing = true)
public class NotificationDeliveryWorker {

	private final NotificationDeliveryService notificationDeliveryService;

	public NotificationDeliveryWorker(NotificationDeliveryService notificationDeliveryService) {
		this.notificationDeliveryService = notificationDeliveryService;
	}

	@Scheduled(
			initialDelayString = "${basilico.notifications.email.initial-delay-ms:5000}",
			fixedDelayString = "${basilico.notifications.email.retry-delay-ms:30000}"
	)
	public void processPendingNotifications() {
		notificationDeliveryService.processPendingBatch();
	}
}
