package com.basilico.backend.admin.security;

import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.basilico.backend.admin.entity.AdminUser;
import com.basilico.backend.admin.repository.AdminUserRepository;
import com.basilico.backend.admin.service.AdminEmail;

@Service
public class AdminUserDetailsService implements UserDetailsService {

	private final AdminUserRepository adminUserRepository;

	public AdminUserDetailsService(AdminUserRepository adminUserRepository) {
		this.adminUserRepository = adminUserRepository;
	}

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		AdminUser user = adminUserRepository.findByEmail(AdminEmail.normalize(username))
				.orElseThrow(() -> new UsernameNotFoundException("Invalid email or password."));

		if (!user.isActive()) {
			throw new DisabledException("Invalid email or password.");
		}

		return new AdminUserPrincipal(user);
	}
}
