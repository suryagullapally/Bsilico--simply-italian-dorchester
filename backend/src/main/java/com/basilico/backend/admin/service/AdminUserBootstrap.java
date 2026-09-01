package com.basilico.backend.admin.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.basilico.backend.admin.entity.AdminRole;
import com.basilico.backend.admin.entity.AdminUser;
import com.basilico.backend.admin.repository.AdminUserRepository;

@Component
public class AdminUserBootstrap implements ApplicationRunner {

	private static final Logger LOGGER = LoggerFactory.getLogger(AdminUserBootstrap.class);

	private final AdminUserRepository adminUserRepository;
	private final PasswordEncoder passwordEncoder;
	private final String email;
	private final String password;
	private final String displayName;

	public AdminUserBootstrap(
			AdminUserRepository adminUserRepository,
			PasswordEncoder passwordEncoder,
			@Value("${basilico.admin.bootstrap.email:}") String email,
			@Value("${basilico.admin.bootstrap.password:}") String password,
			@Value("${basilico.admin.bootstrap.display-name:}") String displayName) {
		this.adminUserRepository = adminUserRepository;
		this.passwordEncoder = passwordEncoder;
		this.email = email;
		this.password = password;
		this.displayName = displayName;
	}

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		if (isBlank(email) || isBlank(password) || isBlank(displayName)) {
			LOGGER.warn("No Basilico admin bootstrap account was created because bootstrap environment variables are missing.");
			return;
		}

		String normalizedEmail = AdminEmail.normalize(email);
		if (adminUserRepository.findByEmail(normalizedEmail).isPresent()) {
			LOGGER.info("Basilico admin bootstrap user already exists; password was not overwritten.");
			return;
		}

		adminUserRepository.save(new AdminUser(
				normalizedEmail,
				passwordEncoder.encode(password),
				displayName.trim(),
				AdminRole.OWNER
		));
		LOGGER.info("Basilico OWNER admin bootstrap account created for {}.", normalizedEmail);
	}

	private boolean isBlank(String value) {
		return value == null || value.isBlank();
	}
}
