package com.basilico.backend.fulfilment.service;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import tools.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class PostcodesIoGeocoder implements PostcodeGeocoder {

	private static final Logger logger = LoggerFactory.getLogger(PostcodesIoGeocoder.class);

	private final PostcodesIoProperties properties;
	private final ObjectMapper objectMapper;
	private final HttpClient httpClient;
	private final ConcurrentMap<String, CachedPostcode> cache = new ConcurrentHashMap<>();

	public PostcodesIoGeocoder(PostcodesIoProperties properties, ObjectMapper objectMapper) {
		this.properties = properties;
		this.objectMapper = objectMapper;
		this.httpClient = HttpClient.newBuilder()
				.connectTimeout(properties.safeTimeout())
				.build();
	}

	@Override
	public Optional<GeocodedPostcode> geocode(String normalizedPostcode) {
		String cacheKey = normalizedPostcode.replace(" ", "");
		CachedPostcode cached = cache.get(cacheKey);
		long now = Instant.now().getEpochSecond();
		if (cached != null && cached.expiresAtEpochSeconds() > now) {
			return cached.value();
		}

		Optional<GeocodedPostcode> geocodedPostcode = lookup(normalizedPostcode);
		store(cacheKey, geocodedPostcode, now);
		return geocodedPostcode;
	}

	private Optional<GeocodedPostcode> lookup(String normalizedPostcode) {
		URI uri = URI.create(properties.safeBaseUrl() + "/postcodes/"
				+ URLEncoder.encode(normalizedPostcode.replace(" ", ""), StandardCharsets.UTF_8));
		HttpRequest request = HttpRequest.newBuilder(uri)
				.timeout(properties.safeTimeout())
				.GET()
				.build();

		try {
			HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
			if (response.statusCode() == 404) {
				return Optional.empty();
			}

			if (response.statusCode() < 200 || response.statusCode() >= 300) {
				logger.warn("Postcodes.io lookup failed with status {}", response.statusCode());
				throw new DeliveryProviderException("Postcode lookup is temporarily unavailable.");
			}

			PostcodesIoResponse body = objectMapper.readValue(response.body(), PostcodesIoResponse.class);
			if (body.result() == null || body.result().postcode() == null
					|| body.result().latitude() == null || body.result().longitude() == null) {
				return Optional.empty();
			}

			return Optional.of(new GeocodedPostcode(
					body.result().postcode(),
					new GeoCoordinates(body.result().latitude(), body.result().longitude())
			));
		} catch (IOException exception) {
			logger.warn("Postcodes.io lookup failed");
			throw new DeliveryProviderException("Postcode lookup is temporarily unavailable.", exception);
		} catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
			throw new DeliveryProviderException("Postcode lookup was interrupted.", exception);
		}
	}

	private void store(String cacheKey, Optional<GeocodedPostcode> value, long now) {
		if (cache.size() >= properties.safeCacheMaxEntries()) {
			cache.clear();
		}

		cache.put(cacheKey, new CachedPostcode(now + properties.safeCacheTtlSeconds(), value));
	}

	private record CachedPostcode(
			long expiresAtEpochSeconds,
			Optional<GeocodedPostcode> value
	) {
	}

	private record PostcodesIoResponse(
			int status,
			PostcodesIoResult result
	) {
	}

	private record PostcodesIoResult(
			String postcode,
			Double latitude,
			Double longitude
	) {
	}
}
