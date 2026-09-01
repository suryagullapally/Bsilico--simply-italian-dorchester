package com.basilico.backend.notification.controller;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.basilico.backend.common.dto.PageResponse;
import com.basilico.backend.notification.dto.NotificationResponse;
import com.basilico.backend.notification.dto.SendManualEmailRequest;
import com.basilico.backend.notification.entity.NotificationStatus;
import com.basilico.backend.notification.entity.NotificationType;
import com.basilico.backend.notification.service.CustomerNotificationService;

@RestController
@RequestMapping("/api/admin/messages")
public class AdminNotificationController {

	private final CustomerNotificationService notificationService;

	public AdminNotificationController(CustomerNotificationService notificationService) {
		this.notificationService = notificationService;
	}

	@GetMapping
	public PageResponse<NotificationResponse> getNotifications(
			@RequestParam(required = false) NotificationStatus status,
			@RequestParam(required = false) NotificationType type,
			@RequestParam(required = false) Long orderId,
			@RequestParam(required = false) Long bookingId,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return notificationService.getAdminNotifications(status, type, orderId, bookingId, page, size);
	}

	@GetMapping("/{id}")
	public NotificationResponse getNotification(@PathVariable Long id) {
		return notificationService.getAdminNotification(id);
	}

	@PostMapping("/email")
	public NotificationResponse sendManualEmail(@Valid @RequestBody SendManualEmailRequest request) {
		return notificationService.sendManualEmail(request);
	}

	@PostMapping("/{id}/retry")
	public NotificationResponse retryNotification(@PathVariable Long id) {
		return notificationService.retryNotification(id);
	}
}
