package com.basilico.backend.payment.stripe;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "basilico.payments.stripe")
public record StripePaymentProperties(
		String secretKey,
		String webhookSecret,
		String customerWebBaseUrl
) {

	public boolean hasSecretKey() {
		return secretKey != null && !secretKey.isBlank();
	}

	public boolean hasWebhookSecret() {
		return webhookSecret != null && !webhookSecret.isBlank();
	}

	public boolean hasCustomerWebBaseUrl() {
		return customerWebBaseUrl != null && !customerWebBaseUrl.isBlank();
	}

	public String normalizedCustomerWebBaseUrl() {
		return customerWebBaseUrl.replaceAll("/+$", "");
	}
}
