package com.basilico.backend.notification.service;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "basilico.notifications.restaurant")
public record RestaurantNotificationProperties(
		String orderAlertEmail,
		String adminBaseUrl
) {

	public boolean hasOrderAlertEmail() {
		return orderAlertEmail != null && !orderAlertEmail.isBlank();
	}

	public String normalizedOrderAlertEmail() {
		return orderAlertEmail == null ? "" : orderAlertEmail.trim();
	}

	public String adminOrderUrl(Long orderId) {
		String baseUrl = adminBaseUrl == null || adminBaseUrl.isBlank()
				? "http://localhost:3002"
				: adminBaseUrl.trim();
		return baseUrl.replaceAll("/+$", "") + "/orders/" + orderId;
	}
}
