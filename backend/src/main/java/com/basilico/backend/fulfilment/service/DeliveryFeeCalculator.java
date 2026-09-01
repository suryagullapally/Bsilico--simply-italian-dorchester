package com.basilico.backend.fulfilment.service;

import org.springframework.stereotype.Component;

import com.basilico.backend.common.error.ConflictException;
import com.basilico.backend.fulfilment.entity.FulfilmentSettings;

@Component
public class DeliveryFeeCalculator {

	private static final double BOUNDARY_EPSILON = 0.000001;

	public boolean isInsideRadius(double distanceMiles, FulfilmentSettings settings) {
		return distanceMiles <= requiredMiles(settings.getDeliveryRadiusMiles(), "Delivery radius") + BOUNDARY_EPSILON;
	}

	public int calculateRadiusBandFeePence(double distanceMiles, FulfilmentSettings settings) {
		double maximumMiles = requiredMiles(settings.getDeliveryRadiusMiles(), "Delivery radius");
		if (distanceMiles > maximumMiles + BOUNDARY_EPSILON) {
			throw new ConflictException("Sorry, we currently deliver within 6 miles of Basilico.");
		}

		double baseRadiusMiles = requiredMiles(settings.getBaseDeliveryRadiusMiles(), "Base delivery radius");
		Integer baseDeliveryFeePence = settings.getBaseDeliveryFeePence();
		Integer extraMileFeePence = settings.getExtraMileFeePence();
		if (baseDeliveryFeePence == null || extraMileFeePence == null) {
			throw new ConflictException("Delivery pricing is not currently configured.");
		}

		if (distanceMiles <= baseRadiusMiles + BOUNDARY_EPSILON) {
			return baseDeliveryFeePence;
		}

		int startedExtraMiles = (int) Math.ceil((distanceMiles - baseRadiusMiles) - BOUNDARY_EPSILON);
		return Math.toIntExact((long) baseDeliveryFeePence + (long) startedExtraMiles * extraMileFeePence);
	}

	private double requiredMiles(java.math.BigDecimal value, String label) {
		if (value == null) {
			throw new ConflictException(label + " is not currently configured.");
		}

		return value.doubleValue();
	}
}
