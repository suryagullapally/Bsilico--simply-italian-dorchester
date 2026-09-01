package com.basilico.backend.payment.stripe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class StripePaymentConfigurationLogger implements ApplicationRunner {

	private static final Logger LOGGER = LoggerFactory.getLogger(StripePaymentConfigurationLogger.class);

	private final StripePaymentProperties properties;

	public StripePaymentConfigurationLogger(StripePaymentProperties properties) {
		this.properties = properties;
	}

	@Override
	public void run(ApplicationArguments args) {
		if (properties.hasSecretKey() && properties.hasWebhookSecret()) {
			LOGGER.info("Stripe payment configuration: available.");
			return;
		}

		LOGGER.warn("Stripe payment configuration: unavailable.");
	}
}
