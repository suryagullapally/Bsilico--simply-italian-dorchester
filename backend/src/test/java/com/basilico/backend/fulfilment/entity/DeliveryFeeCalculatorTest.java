package com.basilico.backend.fulfilment.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.basilico.backend.common.error.ConflictException;
import com.basilico.backend.fulfilment.service.DeliveryFeeCalculator;

class DeliveryFeeCalculatorTest {

	private final DeliveryFeeCalculator calculator = new DeliveryFeeCalculator();

	@Test
	void radiusBandPricingUsesStartedExtraMilesAtExactBoundaries() {
		FulfilmentSettings settings = radiusSettings();

		assertThat(calculator.calculateRadiusBandFeePence(0.5, settings)).isEqualTo(200);
		assertThat(calculator.calculateRadiusBandFeePence(2.99, settings)).isEqualTo(200);
		assertThat(calculator.calculateRadiusBandFeePence(3.00, settings)).isEqualTo(200);
		assertThat(calculator.calculateRadiusBandFeePence(3.01, settings)).isEqualTo(300);
		assertThat(calculator.calculateRadiusBandFeePence(3.99, settings)).isEqualTo(300);
		assertThat(calculator.calculateRadiusBandFeePence(4.00, settings)).isEqualTo(300);
		assertThat(calculator.calculateRadiusBandFeePence(4.01, settings)).isEqualTo(400);
		assertThat(calculator.calculateRadiusBandFeePence(4.99, settings)).isEqualTo(400);
		assertThat(calculator.calculateRadiusBandFeePence(5.00, settings)).isEqualTo(400);
		assertThat(calculator.calculateRadiusBandFeePence(5.01, settings)).isEqualTo(500);
		assertThat(calculator.calculateRadiusBandFeePence(5.99, settings)).isEqualTo(500);
		assertThat(calculator.calculateRadiusBandFeePence(6.00, settings)).isEqualTo(500);
	}

	@Test
	void distanceOverSixMilesIsRejected() {
		FulfilmentSettings settings = radiusSettings();

		assertThat(calculator.isInsideRadius(6.00, settings)).isTrue();
		assertThat(calculator.isInsideRadius(6.01, settings)).isFalse();
		assertThatThrownBy(() -> calculator.calculateRadiusBandFeePence(6.01, settings))
				.isInstanceOf(ConflictException.class)
				.hasMessage("Sorry, we currently deliver within 6 miles of Basilico.");
	}

	private FulfilmentSettings radiusSettings() {
		FulfilmentSettings settings = new FulfilmentSettings();
		settings.setDeliveryRadiusMiles(new BigDecimal("6.00"));
		settings.setBaseDeliveryRadiusMiles(new BigDecimal("3.00"));
		settings.setBaseDeliveryFeePence(200);
		settings.setExtraMileFeePence(100);
		return settings;
	}
}
