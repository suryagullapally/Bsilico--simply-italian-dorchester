package com.basilico.backend.fulfilment.service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import jakarta.annotation.PostConstruct;
import tools.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class OpenRouteServiceDurationProvider implements RouteDurationProvider {

	private static final Logger logger = LoggerFactory.getLogger(OpenRouteServiceDurationProvider.class);
	private static final double METERS_PER_MILE = 1609.344;

	private final OpenRouteServiceProperties properties;
	private final ObjectMapper objectMapper;
	private final HttpClient httpClient;
	private final ConcurrentMap<String, CachedRoute> cache = new ConcurrentHashMap<>();

	public OpenRouteServiceDurationProvider(OpenRouteServiceProperties properties, ObjectMapper objectMapper) {
		this.properties = properties;
		this.objectMapper = objectMapper;
		this.httpClient = HttpClient.newBuilder()
				.connectTimeout(properties.safeTimeout())
				.build();
	}

	@PostConstruct
	void logConfigurationState() {
		if (isConfigured()) {
			logger.info("openrouteservice driving-time estimates are configured.");
		} else {
			logger.info("openrouteservice driving-time estimates are not configured; delivery eligibility and fees still work.");
		}
	}

	@Override
	public Optional<RouteDuration> estimateDrivingDuration(GeoCoordinates origin, GeoCoordinates destination) {
		if (!isConfigured()) {
			return Optional.empty();
		}

		String cacheKey = cacheKey(origin, destination);
		long now = Instant.now().getEpochSecond();
		CachedRoute cached = cache.get(cacheKey);
		if (cached != null && cached.expiresAtEpochSeconds() > now) {
			return cached.value();
		}

		Optional<RouteDuration> routeDuration = lookup(origin, destination);
		store(cacheKey, routeDuration, now);
		return routeDuration;
	}

	@Override
	public boolean isConfigured() {
		return properties.hasApiKey();
	}

	private Optional<RouteDuration> lookup(GeoCoordinates origin, GeoCoordinates destination) {
		String body = String.format(Locale.ROOT, """
				{
				  "coordinates": [
				    [%f, %f],
				    [%f, %f]
				  ],
				  "instructions": false
				}
				""",
				origin.longitude(),
				origin.latitude(),
				destination.longitude(),
				destination.latitude()
		);
		HttpRequest request = HttpRequest.newBuilder(endpoint())
				.timeout(properties.safeTimeout())
				.header("Authorization", properties.safeApiKey())
				.header("Content-Type", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString(body))
				.build();

		try {
			HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
			if (response.statusCode() < 200 || response.statusCode() >= 300) {
				logger.warn("openrouteservice duration lookup failed with status {}", response.statusCode());
				return Optional.empty();
			}

			OpenRouteServiceResponse parsed = objectMapper.readValue(response.body(), OpenRouteServiceResponse.class);
			if (parsed.routes() == null || parsed.routes().isEmpty()
					|| parsed.routes().getFirst().summary() == null
					|| parsed.routes().getFirst().summary().duration() == null) {
				return Optional.empty();
			}

			OpenRouteServiceSummary summary = parsed.routes().getFirst().summary();
			int travelMinutes = (int) Math.ceil(summary.duration() / 60.0);
			Double drivingDistanceMiles = summary.distance() == null ? null : summary.distance() / METERS_PER_MILE;
			return Optional.of(new RouteDuration(Math.max(travelMinutes, 0), drivingDistanceMiles));
		} catch (IOException exception) {
			logger.warn("openrouteservice duration lookup failed");
			return Optional.empty();
		} catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
			return Optional.empty();
		}
	}

	private URI endpoint() {
		return URI.create(properties.safeBaseUrl() + "/v2/directions/driving-car/json");
	}

	private void store(String cacheKey, Optional<RouteDuration> value, long now) {
		if (cache.size() >= properties.safeCacheMaxEntries()) {
			cache.clear();
		}

		cache.put(cacheKey, new CachedRoute(now + properties.safeCacheTtlSeconds(), value));
	}

	private String cacheKey(GeoCoordinates origin, GeoCoordinates destination) {
		return String.format(Locale.ROOT, "%.4f,%.4f:%.4f,%.4f",
				origin.latitude(),
				origin.longitude(),
				destination.latitude(),
				destination.longitude()
		);
	}

	private record CachedRoute(
			long expiresAtEpochSeconds,
			Optional<RouteDuration> value
	) {
	}

	private record OpenRouteServiceResponse(
			List<OpenRouteServiceRoute> routes
	) {
	}

	private record OpenRouteServiceRoute(
			OpenRouteServiceSummary summary
	) {
	}

	private record OpenRouteServiceSummary(
			Double duration,
			Double distance
	) {
	}
}
