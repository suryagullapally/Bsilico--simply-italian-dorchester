package com.basilico.backend.common.ratelimit;

import java.io.IOException;
import java.time.Instant;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import tools.jackson.databind.ObjectMapper;

import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.basilico.backend.common.error.ApiErrorResponse;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

	private static final String FORWARDED_FOR_HEADER = "X-Forwarded-For";

	private final RateLimitProperties properties;
	private final ObjectMapper objectMapper;
	private final ConcurrentMap<String, RateLimitWindow> windows = new ConcurrentHashMap<>();

	public RateLimitFilter(RateLimitProperties properties, ObjectMapper objectMapper) {
		this.properties = properties;
		this.objectMapper = objectMapper;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request,
			HttpServletResponse response,
			FilterChain filterChain) throws ServletException, IOException {
		RateLimitRule rule = ruleFor(request);
		if (!properties.enabled() || rule == null || rule.limit() <= 0) {
			filterChain.doFilter(request, response);
			return;
		}

		RateLimitResult result = recordAttempt(rule, clientKey(request));
		if (!result.allowed()) {
			writeRateLimitResponse(response, result.retryAfterSeconds());
			return;
		}

		filterChain.doFilter(request, response);
	}

	private RateLimitRule ruleFor(HttpServletRequest request) {
		if (!HttpMethod.POST.name().equals(request.getMethod())) {
			return null;
		}

		return switch (request.getRequestURI()) {
			case "/api/orders" -> new RateLimitRule("orders", properties.orderCreateLimit());
			case "/api/bookings" -> new RateLimitRule("bookings", properties.bookingCreateLimit());
			case "/api/payments/checkout-session" ->
					new RateLimitRule("checkout-session", properties.checkoutSessionLimit());
			case "/api/fulfilment/delivery-quote" ->
					new RateLimitRule("delivery-quote", properties.deliveryQuoteLimit());
			case "/api/admin/auth/login" -> new RateLimitRule("admin-login", properties.adminLoginLimit());
			default -> null;
		};
	}

	private RateLimitResult recordAttempt(RateLimitRule rule, String clientKey) {
		long now = Instant.now().toEpochMilli();
		long windowMillis = properties.safeWindowSeconds() * 1000L;
		String key = rule.name() + ":" + clientKey;
		AtomicReference<RateLimitResult> result = new AtomicReference<>();

		windows.compute(key, (ignored, current) -> {
			if (current == null || now - current.startedAtMillis() >= windowMillis) {
				result.set(new RateLimitResult(true, 0));
				return new RateLimitWindow(now, 1);
			}

			if (current.count() >= rule.limit()) {
				long retryAfterMillis = windowMillis - (now - current.startedAtMillis());
				result.set(new RateLimitResult(false, Math.max(1, Math.ceilDiv(retryAfterMillis, 1000L))));
				return current;
			}

			result.set(new RateLimitResult(true, 0));
			return new RateLimitWindow(current.startedAtMillis(), current.count() + 1);
		});

		cleanupExpiredWindows(now, windowMillis);
		return result.get();
	}

	private void cleanupExpiredWindows(long now, long windowMillis) {
		windows.entrySet().removeIf(entry -> now - entry.getValue().startedAtMillis() >= windowMillis * 2);
	}

	private String clientKey(HttpServletRequest request) {
		String forwardedFor = request.getHeader(FORWARDED_FOR_HEADER);
		if (forwardedFor != null && !forwardedFor.isBlank()) {
			return forwardedFor.split(",")[0].trim().toLowerCase(Locale.ROOT);
		}

		return request.getRemoteAddr();
	}

	private void writeRateLimitResponse(HttpServletResponse response, long retryAfterSeconds) throws IOException {
		response.setStatus(429);
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setHeader("Retry-After", Long.toString(retryAfterSeconds));
		objectMapper.writeValue(response.getWriter(), new ApiErrorResponse(
				429,
				"RATE_LIMITED",
				"Too many requests. Please wait a moment and try again."
		));
	}

	private record RateLimitRule(String name, int limit) {
	}

	private record RateLimitWindow(long startedAtMillis, int count) {
	}

	private record RateLimitResult(boolean allowed, long retryAfterSeconds) {
	}
}
