package com.basilico.backend.order.dto;

public record CustomerResponse(
		String firstName,
		String lastName,
		String phone,
		String email
) {
}
