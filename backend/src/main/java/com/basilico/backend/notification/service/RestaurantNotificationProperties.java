package com.basilico.backend.notification.service;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "basilico.notifications.restaurant")
public record RestaurantNotificationProperties(
		String orderAlertEmail,
		String bookingAlertEmail,
		String adminBaseUrl
) {

	public boolean hasOrderAlertEmail() {
		return orderAlertEmail != null && !orderAlertEmail.isBlank();
	}

	public String normalizedOrderAlertEmail() {
		return orderAlertEmail == null ? "" : orderAlertEmail.trim();
	}

	public boolean hasBookingAlertEmail() {
		return bookingAlertEmail != null && !bookingAlertEmail.isBlank();
	}

	public String normalizedBookingAlertEmail() {
		return bookingAlertEmail == null ? "" : bookingAlertEmail.trim();
	}

	public String adminOrderUrl(Long orderId) {
		return adminUrl("/orders/" + orderId);
	}

	public String adminBookingUrl(Long bookingId) {
		return adminUrl("/bookings/" + bookingId);
	}

	private String adminUrl(String path) {
		String baseUrl = adminBaseUrl == null || adminBaseUrl.isBlank()
				? "http://localhost:3002"
				: adminBaseUrl.trim();
		return baseUrl.replaceAll("/+$", "") + path;
	}
}
