package com.basilico.backend.payment.service;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "basilico.payments.manual-payment-status")
public record ManualPaymentStatusProperties(
		boolean enabled
) {
}
