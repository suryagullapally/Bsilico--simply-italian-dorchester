package com.basilico.backend.order.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record OrderCustomerRequest(
		@NotBlank @Size(max = 120) String firstName,
		@NotBlank @Size(max = 120) String lastName,
		@NotBlank @Size(max = 60) String phone,
		@NotBlank @Email @Size(max = 254) String email
) {
}
