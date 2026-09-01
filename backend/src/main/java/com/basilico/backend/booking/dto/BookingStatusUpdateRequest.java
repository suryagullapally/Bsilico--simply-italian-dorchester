package com.basilico.backend.booking.dto;

import jakarta.validation.constraints.NotNull;

import com.basilico.backend.booking.entity.BookingStatus;

public record BookingStatusUpdateRequest(
		@NotNull BookingStatus status
) {
}
