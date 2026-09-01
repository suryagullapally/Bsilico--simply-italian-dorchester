package com.basilico.backend.fulfilment.service;

import org.springframework.stereotype.Component;

@Component
public class DeliveryDistanceCalculator {

	private static final double EARTH_RADIUS_MILES = 3958.7613;

	public double distanceMiles(GeoCoordinates origin, GeoCoordinates destination) {
		double originLatitude = Math.toRadians(origin.latitude());
		double destinationLatitude = Math.toRadians(destination.latitude());
		double latitudeDelta = Math.toRadians(destination.latitude() - origin.latitude());
		double longitudeDelta = Math.toRadians(destination.longitude() - origin.longitude());

		double haversine = Math.sin(latitudeDelta / 2) * Math.sin(latitudeDelta / 2)
				+ Math.cos(originLatitude) * Math.cos(destinationLatitude)
				* Math.sin(longitudeDelta / 2) * Math.sin(longitudeDelta / 2);
		double centralAngle = 2 * Math.atan2(Math.sqrt(haversine), Math.sqrt(1 - haversine));

		return EARTH_RADIUS_MILES * centralAngle;
	}
}
