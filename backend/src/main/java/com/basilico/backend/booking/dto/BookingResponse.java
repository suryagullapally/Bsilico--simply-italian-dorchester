package com.basilico.backend.booking.dto;

import java.time.LocalDate;
import java.time.LocalTime;

import com.basilico.backend.booking.entity.BookingStatus;

public record BookingResponse(
		String bookingReference,
		BookingStatus status,
		LocalDate date,
		LocalTime time,
		int partySize
) {
}
