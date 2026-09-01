package com.basilico.backend.fulfilment.service;

import java.util.Optional;

public interface PostcodeGeocoder {

	Optional<GeocodedPostcode> geocode(String normalizedPostcode);
}
