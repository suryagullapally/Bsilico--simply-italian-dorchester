package com.basilico.backend.booking.dto;

import java.time.LocalDate;
import java.time.LocalTime;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateBookingRequest(
		@NotNull LocalDate date,
		@NotNull LocalTime time,
		@NotNull @Min(1) Integer partySize,
		@NotBlank @Size(max = 120) String firstName,
		@NotBlank @Size(max = 120) String lastName,
		@NotBlank @Size(max = 60) String phone,
		@NotBlank @Email @Size(max = 254) String email,
		@Size(max = 1000) String specialRequests
) {
}
