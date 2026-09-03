package com.basilico.backend.order;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.basilico.backend.common.RestaurantSchedule;
import com.basilico.backend.menu.entity.MenuItem;
import com.basilico.backend.menu.repository.MenuItemRepository;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class OrderScheduleValidationApiTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private MenuItemRepository menuItemRepository;

	@Autowired
	private MutableClock clock;

	private Long margheritaId;

	@BeforeEach
	void setUp() {
		margheritaId = menuItemRepository.findBySlug("pizza-margherita")
				.map(MenuItem::getId)
				.orElseThrow();
	}

	@Test
	void rejectsAsapWhenRestaurantIsClosedBeforeOpening() throws Exception {
		clock.setInstant("2026-08-31T08:00:00Z");

		mockMvc.perform(post("/api/orders")
						.contentType(MediaType.APPLICATION_JSON)
						.content(orderPayload("""
								"timing": { "type": "ASAP" }
								""")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value(
						"Basilico is currently closed. Please choose an available order time."));
	}

	@Test
	void acceptsAsapDuringOrderWindow() throws Exception {
		clock.setInstant("2026-08-31T11:00:00Z");

		mockMvc.perform(post("/api/orders")
						.contentType(MediaType.APPLICATION_JSON)
						.content(orderPayload("""
								"timing": { "type": "ASAP" }
								""")))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.status").value("PENDING_PAYMENT"));
	}

	@Test
	void rejectsSameDayScheduledTimeThatHasPassed() throws Exception {
		clock.setInstant("2026-08-31T17:32:00Z");

		mockMvc.perform(post("/api/orders")
						.contentType(MediaType.APPLICATION_JSON)
						.content(orderPayload("""
								"timing": {
								  "type": "SCHEDULED",
								  "requestedDate": "2026-08-31",
								  "requestedTime": "18:30"
								}
								""")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value(
						"Requested order time is no longer available. Please choose a later time."));
	}

	@Test
	void acceptsFutureOpenDayScheduledNoonSlot() throws Exception {
		clock.setInstant("2026-08-31T08:00:00Z");

		mockMvc.perform(post("/api/orders")
						.contentType(MediaType.APPLICATION_JSON)
						.content(orderPayload("""
								"timing": {
								  "type": "SCHEDULED",
								  "requestedDate": "2026-09-02",
								  "requestedTime": "12:00"
								}
								""")))
				.andExpect(status().isCreated());
	}

	@Test
	void publicFulfilmentOptionsExposeAuthoritativeOrderAvailability() throws Exception {
		clock.setInstant("2026-09-01T12:00:00Z");

		mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/fulfilment/options"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.orderAvailability.restaurantTimezone").value("Europe/London"))
				.andExpect(jsonPath("$.orderAvailability.asapAvailable").value(false))
				.andExpect(jsonPath("$.orderAvailability.nextAvailableDate").value("2026-09-02"))
				.andExpect(jsonPath("$.orderAvailability.nextAvailableTime").value("12:00"))
				.andExpect(jsonPath("$.orderAvailability.statusMessage").value(
						"Basilico is closed today. You can still order for later. Next available: Wednesday 12:00."))
				.andExpect(jsonPath("$.orderAvailability.validOrderDates[0].date").value("2026-09-02"));
	}

	private String orderPayload(String timingJson) {
		return """
				{
				  "fulfilmentType": "COLLECTION",
				  "customer": {
				    "firstName": "John",
				    "lastName": "Smith",
				    "phone": "07123456789",
				    "email": "john@example.com"
				  },
				  %s,
				  "items": [
				    { "type": "MENU_ITEM", "menuItemId": %d, "quantity": 1 }
				  ]
				}
				""".formatted(timingJson, margheritaId);
	}

	static class MutableClock extends Clock {

		private Instant instant = Instant.parse("2026-08-31T08:00:00Z");

		void setInstant(String value) {
			instant = Instant.parse(value);
		}

		@Override
		public ZoneId getZone() {
			return RestaurantSchedule.RESTAURANT_ZONE;
		}

		@Override
		public Clock withZone(ZoneId zone) {
			return this;
		}

		@Override
		public Instant instant() {
			return instant;
		}
	}

	@TestConfiguration
	static class TestClockConfig {

		@Bean
		@Primary
		MutableClock mutableClock() {
			return new MutableClock();
		}
	}
}
