package com.basilico.backend.admin.dto;

public record CsrfTokenResponse(
		String token,
		String headerName
) {
}
