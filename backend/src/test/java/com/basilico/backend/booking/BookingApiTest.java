package com.basilico.backend.booking;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

import java.time.DayOfWeek;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class BookingApiTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void createsValidWednesdayToMondayBookingAsRequested() throws Exception {
		mockMvc.perform(post("/api/bookings")
						.contentType(MediaType.APPLICATION_JSON)
						.content(bookingPayload(nextOpenDate(), "18:30", 4)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.bookingReference").exists())
				.andExpect(jsonPath("$.status").value("REQUESTED"))
				.andExpect(jsonPath("$.partySize").value(4));
	}

	@Test
	void rejectsTuesdayBooking() throws Exception {
		mockMvc.perform(post("/api/bookings")
						.contentType(MediaType.APPLICATION_JSON)
						.content(bookingPayload(nextTuesday(), "18:30", 4)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Basilico is closed on Tuesdays. Please choose another day."));
	}

	@Test
	void rejectsPastDate() throws Exception {
		mockMvc.perform(post("/api/bookings")
						.contentType(MediaType.APPLICATION_JSON)
						.content(bookingPayload(LocalDate.now().minusDays(1), "18:30", 4)))
				.andExpect(status().isBadRequest());
	}

	@Test
	void rejectsTimeBeforeOpening() throws Exception {
		mockMvc.perform(post("/api/bookings")
						.contentType(MediaType.APPLICATION_JSON)
						.content(bookingPayload(nextOpenDate(), "11:45", 4)))
				.andExpect(status().isBadRequest());
	}

	@Test
	void rejectsClosingTime() throws Exception {
		mockMvc.perform(post("/api/bookings")
						.contentType(MediaType.APPLICATION_JSON)
						.content(bookingPayload(nextOpenDate(), "23:00", 4)))
				.andExpect(status().isBadRequest());
	}

	@Test
	void acceptsEighteenThirtyBooking() throws Exception {
		mockMvc.perform(post("/api/bookings")
						.contentType(MediaType.APPLICATION_JSON)
						.content(bookingPayload(nextOpenDate(), "18:30", 2)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.time").value("18:30:00"));
	}

	@Test
	void rejectsPartySizeBelowOne() throws Exception {
		mockMvc.perform(post("/api/bookings")
						.contentType(MediaType.APPLICATION_JSON)
						.content(bookingPayload(nextOpenDate(), "18:30", 0)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
	}

	@Test
	void rejectsOversizedSpecialRequests() throws Exception {
		mockMvc.perform(post("/api/bookings")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "date": "%s",
								  "time": "18:30",
								  "partySize": 2,
								  "firstName": "John",
								  "lastName": "Smith",
								  "phone": "07123456789",
								  "email": "john@example.com",
								  "specialRequests": "%s"
								}
								""".formatted(nextOpenDate(), "x".repeat(1001))))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
	}

	@Test
	void adminCanUpdateBookingStatus() throws Exception {
		mockMvc.perform(post("/api/bookings")
						.contentType(MediaType.APPLICATION_JSON)
						.content(bookingPayload(nextOpenDate(), "19:30", 4)))
				.andExpect(status().isCreated());

		String bookingId = com.jayway.jsonpath.JsonPath.read(
				mockMvc.perform(get("/api/admin/bookings")
								.with(adminUser()))
						.andExpect(status().isOk())
						.andReturn()
						.getResponse()
						.getContentAsString(),
				"$.content[0].id"
		).toString();

		mockMvc.perform(patch("/api/admin/bookings/{id}/status", bookingId)
						.with(adminUser())
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "status": "CONFIRMED"
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("CONFIRMED"));
	}

	private String bookingPayload(LocalDate date, String time, int partySize) {
		return """
				{
				  "date": "%s",
				  "time": "%s",
				  "partySize": %d,
				  "firstName": "John",
				  "lastName": "Smith",
				  "phone": "07123456789",
				  "email": "john@example.com",
				  "specialRequests": "Birthday dinner"
				}
				""".formatted(date, time, partySize);
	}

	private LocalDate nextOpenDate() {
		LocalDate date = LocalDate.now().plusDays(1);
		while (date.getDayOfWeek() == DayOfWeek.TUESDAY) {
			date = date.plusDays(1);
		}
		return date;
	}

	private LocalDate nextTuesday() {
		LocalDate date = LocalDate.now().plusDays(1);
		while (date.getDayOfWeek() != DayOfWeek.TUESDAY) {
			date = date.plusDays(1);
		}
		return date;
	}

	private org.springframework.test.web.servlet.request.RequestPostProcessor adminUser() {
		return user("owner@basilico.test").roles("OWNER");
	}
}
