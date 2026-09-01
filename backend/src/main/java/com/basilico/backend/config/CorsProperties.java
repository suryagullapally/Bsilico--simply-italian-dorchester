package com.basilico.backend.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "basilico.cors")
public record CorsProperties(
		List<String> allowedOrigins
) {

	public List<String> sanitizedAllowedOrigins() {
		if (allowedOrigins == null) {
			return List.of();
		}

		return allowedOrigins.stream()
				.map(String::trim)
				.filter(origin -> !origin.isBlank())
				.distinct()
				.toList();
	}
}
