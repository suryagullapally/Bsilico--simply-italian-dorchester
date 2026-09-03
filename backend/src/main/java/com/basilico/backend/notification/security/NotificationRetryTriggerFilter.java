package com.basilico.backend.notification.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import tools.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.basilico.backend.common.error.ApiErrorResponse;

@Component
public class NotificationRetryTriggerFilter extends OncePerRequestFilter {

	public static final String TRIGGER_PATH = "/api/internal/notifications/process-pending";
	public static final String TOKEN_HEADER = "X-Basilico-Retry-Token";

	private final String retryTriggerToken;
	private final ObjectMapper objectMapper;

	public NotificationRetryTriggerFilter(
			@Value("${basilico.notifications.email.retry-trigger-token:}") String retryTriggerToken,
			ObjectMapper objectMapper) {
		this.retryTriggerToken = retryTriggerToken;
		this.objectMapper = objectMapper;
	}

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		return !"POST".equalsIgnoreCase(request.getMethod())
				|| !TRIGGER_PATH.equals(requestPath(request));
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request,
			HttpServletResponse response,
			FilterChain filterChain) throws ServletException, IOException {
		String configuredToken = retryTriggerToken;
		if (configuredToken == null || configuredToken.isBlank()) {
			writeError(response, HttpServletResponse.SC_SERVICE_UNAVAILABLE,
					"SERVICE_UNAVAILABLE", "Notification retry trigger is not configured.");
			return;
		}

		String suppliedToken = request.getHeader(TOKEN_HEADER);
		if (suppliedToken == null || suppliedToken.isBlank()
				|| !constantTimeMatches(configuredToken, suppliedToken)) {
			writeError(response, HttpServletResponse.SC_UNAUTHORIZED,
					"UNAUTHORIZED", "Notification retry trigger is not authorized.");
			return;
		}

		filterChain.doFilter(request, response);
	}

	private boolean constantTimeMatches(String configuredToken, String suppliedToken) {
		return MessageDigest.isEqual(sha256(configuredToken), sha256(suppliedToken));
	}

	private String requestPath(HttpServletRequest request) {
		String path = request.getRequestURI();
		String contextPath = request.getContextPath();
		if (contextPath != null && !contextPath.isBlank() && path.startsWith(contextPath)) {
			return path.substring(contextPath.length());
		}
		return path;
	}

	private byte[] sha256(String value) {
		try {
			return MessageDigest.getInstance("SHA-256")
					.digest(value.getBytes(StandardCharsets.UTF_8));
		}
		catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException("SHA-256 is not available.", exception);
		}
	}

	private void writeError(HttpServletResponse response,
			int status,
			String code,
			String message) throws IOException {
		response.setStatus(status);
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		objectMapper.writeValue(response.getWriter(), new ApiErrorResponse(status, code, message));
	}
}
