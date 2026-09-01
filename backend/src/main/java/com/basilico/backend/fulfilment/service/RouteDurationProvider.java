package com.basilico.backend.fulfilment.service;

import java.util.Optional;

public interface RouteDurationProvider {

	Optional<RouteDuration> estimateDrivingDuration(GeoCoordinates origin, GeoCoordinates destination);

	boolean isConfigured();
}
