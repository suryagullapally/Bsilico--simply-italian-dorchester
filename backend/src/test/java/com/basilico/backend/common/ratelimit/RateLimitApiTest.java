package com.basilico.backend.common.ratelimit;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.DayOfWeek;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@SpringBootTest(properties = {
		"basilico.rate-limit.admin-login-limit=2",
		"basilico.rate-limit.booking-create-limit=1",
		"basilico.rate-limit.delivery-quote-limit=1",
		"basilico.rate-limit.window-seconds=60"
})
@AutoConfigureMockMvc
class RateLimitApiTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void adminLoginIsTemporarilyThrottledAfterRepeatedAttempts() throws Exception {
		String payload = """
				{
				  "email": "missing@basilico.test",
				  "password": "wrong-password"
				}
				""";

		mockMvc.perform(post("/api/admin/auth/login")
						.with(csrf())
						.with(remoteAddress("203.0.113.10"))
						.contentType(MediaType.APPLICATION_JSON)
						.content(payload))
				.andExpect(status().isUnauthorized());

		mockMvc.perform(post("/api/admin/auth/login")
						.with(csrf())
						.with(remoteAddress("203.0.113.10"))
						.contentType(MediaType.APPLICATION_JSON)
						.content(payload))
				.andExpect(status().isUnauthorized());

		mockMvc.perform(post("/api/admin/auth/login")
						.with(csrf())
						.with(remoteAddress("203.0.113.10"))
						.contentType(MediaType.APPLICATION_JSON)
						.content(payload))
				.andExpect(status().isTooManyRequests())
				.andExpect(header().string("Retry-After", "60"))
				.andExpect(jsonPath("$.error").value("RATE_LIMITED"));
	}

	@Test
	void publicBookingCreationIsRateLimitedPerClient() throws Exception {
		String payload = """
				{
				  "date": "%s",
				  "time": "18:30",
				  "partySize": 2,
				  "firstName": "John",
				  "lastName": "Smith",
				  "phone": "07123456789",
				  "email": "john@example.com",
				  "specialRequests": ""
				}
				""".formatted(nextOpenDate());

		mockMvc.perform(post("/api/bookings")
						.with(remoteAddress("203.0.113.20"))
						.contentType(MediaType.APPLICATION_JSON)
						.content(payload))
				.andExpect(status().isCreated());

		mockMvc.perform(post("/api/bookings")
						.with(remoteAddress("203.0.113.20"))
						.contentType(MediaType.APPLICATION_JSON)
						.content(payload))
				.andExpect(status().isTooManyRequests())
				.andExpect(jsonPath("$.message").value("Too many requests. Please wait a moment and try again."));
	}

	@Test
	void publicDeliveryQuoteIsRateLimitedPerClient() throws Exception {
		String payload = """
				{
				  "latitude": 50.71405,
				  "longitude": -2.43819
				}
				""";

		mockMvc.perform(post("/api/fulfilment/delivery-quote")
						.with(remoteAddress("203.0.113.25"))
						.contentType(MediaType.APPLICATION_JSON)
						.content(payload))
				.andExpect(status().isOk());

		mockMvc.perform(post("/api/fulfilment/delivery-quote")
						.with(remoteAddress("203.0.113.25"))
						.contentType(MediaType.APPLICATION_JSON)
						.content(payload))
				.andExpect(status().isTooManyRequests())
				.andExpect(jsonPath("$.error").value("RATE_LIMITED"));
	}

	@Test
	void stripeWebhookIsNotRateLimited() throws Exception {
		for (int attempt = 0; attempt < 3; attempt++) {
			mockMvc.perform(post("/api/payments/stripe/webhook")
							.with(remoteAddress("203.0.113.30"))
							.contentType(MediaType.APPLICATION_JSON)
							.content("{}"))
					.andExpect(status().isBadRequest())
					.andExpect(jsonPath("$.error").value("BAD_REQUEST"));
		}
	}

	private RequestPostProcessor remoteAddress(String remoteAddress) {
		return request -> {
			request.setRemoteAddr(remoteAddress);
			return request;
		};
	}

	private LocalDate nextOpenDate() {
		LocalDate date = LocalDate.now().plusDays(1);
		while (date.getDayOfWeek() == DayOfWeek.TUESDAY) {
			date = date.plusDays(1);
		}
		return date;
	}
}
