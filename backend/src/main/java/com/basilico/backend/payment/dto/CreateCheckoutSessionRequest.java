package com.basilico.backend.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCheckoutSessionRequest(
		@NotBlank @Size(max = 32) String orderReference
) {
}
