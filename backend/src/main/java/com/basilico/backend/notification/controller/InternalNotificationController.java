package com.basilico.backend.notification.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.basilico.backend.notification.dto.ProcessPendingNotificationsResponse;
import com.basilico.backend.notification.service.NotificationDeliveryService;

@RestController
@RequestMapping("/api/internal/notifications")
public class InternalNotificationController {

	private static final Logger logger = LoggerFactory.getLogger(InternalNotificationController.class);

	private final NotificationDeliveryService notificationDeliveryService;

	public InternalNotificationController(NotificationDeliveryService notificationDeliveryService) {
		this.notificationDeliveryService = notificationDeliveryService;
	}

	@PostMapping("/process-pending")
	public ProcessPendingNotificationsResponse processPendingNotifications() {
		int processedCount = notificationDeliveryService.processPendingBatch();
		logger.info("Notification retry trigger processed {} notifications.", processedCount);
		return new ProcessPendingNotificationsResponse("processed", processedCount);
	}
}
