package com.basilico.backend.booking.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;

import com.basilico.backend.booking.entity.BookingStatus;

public record AdminBookingResponse(
		Long id,
		String bookingReference,
		BookingStatus status,
		LocalDate date,
		LocalTime time,
		int partySize,
		String firstName,
		String lastName,
		String phone,
		String email,
		String specialRequests,
		OffsetDateTime createdAt,
		OffsetDateTime updatedAt
) {
}
