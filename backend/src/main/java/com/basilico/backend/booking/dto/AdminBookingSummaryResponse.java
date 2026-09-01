package com.basilico.backend.booking.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;

import com.basilico.backend.booking.entity.BookingStatus;

public record AdminBookingSummaryResponse(
		Long id,
		String bookingReference,
		BookingStatus status,
		LocalDate date,
		LocalTime time,
		int partySize,
		String customerName,
		OffsetDateTime createdAt,
		OffsetDateTime updatedAt
) {
}
