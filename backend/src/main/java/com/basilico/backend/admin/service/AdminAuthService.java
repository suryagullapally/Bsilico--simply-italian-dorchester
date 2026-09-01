package com.basilico.backend.admin.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.basilico.backend.admin.dto.AdminUserResponse;
import com.basilico.backend.admin.repository.AdminUserRepository;
import com.basilico.backend.admin.security.AdminUserPrincipal;

@Service
public class AdminAuthService {

	private final AdminUserRepository adminUserRepository;

	public AdminAuthService(AdminUserRepository adminUserRepository) {
		this.adminUserRepository = adminUserRepository;
	}

	@Transactional
	public AdminUserResponse markLoginAndRespond(AdminUserPrincipal principal) {
		adminUserRepository.findById(principal.id())
				.ifPresent(user -> {
					user.markLoggedIn();
					adminUserRepository.save(user);
				});

		return toResponse(principal);
	}

	public AdminUserResponse toResponse(AdminUserPrincipal principal) {
		return new AdminUserResponse(
				principal.id(),
				principal.email(),
				principal.displayName(),
				principal.role()
		);
	}
}
