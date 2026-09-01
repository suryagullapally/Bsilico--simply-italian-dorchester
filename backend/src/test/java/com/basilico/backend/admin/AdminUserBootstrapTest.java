package com.basilico.backend.admin;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.basilico.backend.admin.entity.AdminRole;
import com.basilico.backend.admin.entity.AdminUser;
import com.basilico.backend.admin.repository.AdminUserRepository;
import com.basilico.backend.admin.service.AdminUserBootstrap;

class AdminUserBootstrapTest {

	private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

	@Test
	void createsOwnerWhenBootstrapEnvironmentIsComplete() {
		AdminUserRepository repository = mock(AdminUserRepository.class);
		AdminUserBootstrap bootstrap = new AdminUserBootstrap(
				repository,
				passwordEncoder,
				"OWNER@BASILICO.TEST",
				"not-a-real-secret",
				"Basilico Owner"
		);

		when(repository.findByEmail("owner@basilico.test")).thenReturn(Optional.empty());

		bootstrap.run(new DefaultApplicationArguments());

		verify(repository).save(any(AdminUser.class));
	}

	@Test
	void doesNotCreateOwnerWhenBootstrapEnvironmentIsIncomplete() {
		AdminUserRepository repository = mock(AdminUserRepository.class);
		AdminUserBootstrap bootstrap = new AdminUserBootstrap(
				repository,
				passwordEncoder,
				"",
				"",
				""
		);

		bootstrap.run(new DefaultApplicationArguments());

		verify(repository, never()).save(any(AdminUser.class));
	}

	@Test
	void existingOwnerIsNotDuplicatedOrOverwritten() {
		AdminUserRepository repository = mock(AdminUserRepository.class);
		AdminUser existing = new AdminUser(
				"owner@basilico.test",
				passwordEncoder.encode("original-password"),
				"Basilico Owner",
				AdminRole.OWNER
		);
		AdminUserBootstrap bootstrap = new AdminUserBootstrap(
				repository,
				passwordEncoder,
				"owner@basilico.test",
				"changed-bootstrap-password",
				"Basilico Owner"
		);

		when(repository.findByEmail("owner@basilico.test")).thenReturn(Optional.of(existing));

		bootstrap.run(new DefaultApplicationArguments());

		verify(repository, never()).save(any(AdminUser.class));
	}
}
