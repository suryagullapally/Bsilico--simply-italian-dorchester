package com.basilico.backend.notification.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SendManualEmailRequest(
		Long orderId,
		Long bookingId,
		@NotBlank
		@Size(max = 180)
		String subject,
		@NotBlank
		@Size(max = 5000)
		String message
) {
}
