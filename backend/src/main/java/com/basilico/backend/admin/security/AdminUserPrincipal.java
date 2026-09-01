package com.basilico.backend.admin.security;

import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.basilico.backend.admin.entity.AdminRole;
import com.basilico.backend.admin.entity.AdminUser;

public class AdminUserPrincipal implements UserDetails {

	private final Long id;
	private final String email;
	private final String passwordHash;
	private final String displayName;
	private final AdminRole role;
	private final boolean active;

	public AdminUserPrincipal(AdminUser user) {
		this.id = user.getId();
		this.email = user.getEmail();
		this.passwordHash = user.getPasswordHash();
		this.displayName = user.getDisplayName();
		this.role = user.getRole();
		this.active = user.isActive();
	}

	public Long id() {
		return id;
	}

	public String email() {
		return email;
	}

	public String displayName() {
		return displayName;
	}

	public AdminRole role() {
		return role;
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
	}

	@Override
	public String getPassword() {
		return passwordHash;
	}

	@Override
	public String getUsername() {
		return email;
	}

	@Override
	public boolean isEnabled() {
		return active;
	}
}
