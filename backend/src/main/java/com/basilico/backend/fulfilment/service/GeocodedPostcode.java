package com.basilico.backend.fulfilment.service;

public record GeocodedPostcode(
		String normalizedPostcode,
		GeoCoordinates coordinates
) {
}
