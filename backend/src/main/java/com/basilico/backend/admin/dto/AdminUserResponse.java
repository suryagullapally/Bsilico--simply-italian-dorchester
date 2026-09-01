package com.basilico.backend.admin.dto;

import com.basilico.backend.admin.entity.AdminRole;

public record AdminUserResponse(
		Long id,
		String email,
		String displayName,
		AdminRole role
) {
}
