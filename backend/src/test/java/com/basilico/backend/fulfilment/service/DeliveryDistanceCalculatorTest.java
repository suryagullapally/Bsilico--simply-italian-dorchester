package com.basilico.backend.fulfilment.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class DeliveryDistanceCalculatorTest {

	private static final GeoCoordinates BASILICO = new GeoCoordinates(50.71405, -2.43819);

	private final DeliveryDistanceCalculator calculator = new DeliveryDistanceCalculator();

	@Test
	void samePointIsZeroMiles() {
		assertThat(calculator.distanceMiles(BASILICO, BASILICO)).isCloseTo(0.0, withinMiles());
	}

	@Test
	void knownNearbyPointIsInsideSixMiles() {
		double distance = calculator.distanceMiles(BASILICO, pointEast(5.5));

		assertThat(distance).isLessThan(6.0);
	}

	@Test
	void pointAtSixMilesIsEligibleWithinSmallTolerance() {
		double distance = calculator.distanceMiles(BASILICO, pointEast(6.0));

		assertThat(distance).isCloseTo(6.0, withinMiles());
	}

	@Test
	void pointSlightlyOverSixMilesIsOutsideRadius() {
		double distance = calculator.distanceMiles(BASILICO, pointEast(6.1));

		assertThat(distance).isGreaterThan(6.0);
	}

	private GeoCoordinates pointEast(double miles) {
		double longitudeDelta = miles / (69.172 * Math.cos(Math.toRadians(BASILICO.latitude())));
		return new GeoCoordinates(BASILICO.latitude(), BASILICO.longitude() + longitudeDelta);
	}

	private org.assertj.core.data.Offset<Double> withinMiles() {
		return org.assertj.core.data.Offset.offset(0.03);
	}
}
