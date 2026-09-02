package com.basilico.backend.config;

import java.time.Duration;
import java.util.Locale;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "basilico.admin.session.cookie")
public record AdminSessionCookieProperties(
		Duration maxAge,
		boolean secure,
		String sameSite
) {

	public AdminSessionCookieProperties {
		if (maxAge == null) {
			maxAge = Duration.ofHours(8);
		}
		if (maxAge.isZero() || maxAge.isNegative()) {
			throw new IllegalArgumentException("Admin session cookie max age must be positive");
		}
		if (sameSite == null || sameSite.isBlank()) {
			sameSite = "Lax";
		}
		sameSite = normalizeSameSite(sameSite);
	}

	public int maxAgeSeconds() {
		return Math.toIntExact(maxAge.toSeconds());
	}

	private static String normalizeSameSite(String sameSite) {
		return switch (sameSite.trim().toLowerCase(Locale.ROOT)) {
			case "lax" -> "Lax";
			case "strict" -> "Strict";
			case "none" -> "None";
			default -> throw new IllegalArgumentException("Admin session cookie SameSite must be Lax, Strict, or None");
		};
	}
}
