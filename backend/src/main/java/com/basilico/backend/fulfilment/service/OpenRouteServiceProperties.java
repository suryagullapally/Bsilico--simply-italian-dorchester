package com.basilico.backend.fulfilment.service;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "basilico.delivery.openrouteservice")
public record OpenRouteServiceProperties(
		String apiKey,
		String baseUrl,
		int timeoutMs,
		long cacheTtlSeconds,
		int cacheMaxEntries
) {

	public boolean hasApiKey() {
		return apiKey != null && !apiKey.isBlank();
	}

	public String safeApiKey() {
		return apiKey == null ? "" : apiKey.trim();
	}

	public String safeBaseUrl() {
		if (baseUrl == null || baseUrl.isBlank()) {
			return "https://api.heigit.org/openrouteservice";
		}

		return baseUrl.replaceAll("/+$", "");
	}

	public Duration safeTimeout() {
		return Duration.ofMillis(Math.max(timeoutMs, 500));
	}

	public long safeCacheTtlSeconds() {
		return Math.max(cacheTtlSeconds, 60);
	}

	public int safeCacheMaxEntries() {
		return Math.max(cacheMaxEntries, 1);
	}
}
