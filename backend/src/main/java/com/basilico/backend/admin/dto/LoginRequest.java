package com.basilico.backend.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
		@NotBlank @Size(max = 254) String email,
		@NotBlank @Size(max = 200) String password
) {
}
