package com.basilico.backend.common.ratelimit;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "basilico.rate-limit")
public record RateLimitProperties(
		boolean enabled,
		int windowSeconds,
		int orderCreateLimit,
		int bookingCreateLimit,
		int checkoutSessionLimit,
		int deliveryQuoteLimit,
		int adminLoginLimit
) {

	public int safeWindowSeconds() {
		return Math.max(windowSeconds, 1);
	}
}
