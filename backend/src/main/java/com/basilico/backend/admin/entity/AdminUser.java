package com.basilico.backend.admin.entity;

import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "admin_users")
public class AdminUser {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true, length = 320)
	private String email;

	@Column(name = "password_hash", nullable = false)
	private String passwordHash;

	@Column(name = "display_name", nullable = false, length = 160)
	private String displayName;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 40)
	private AdminRole role;

	@Column(nullable = false)
	private boolean active = true;

	@Column(name = "last_login_at")
	private OffsetDateTime lastLoginAt;

	@Column(name = "created_at", insertable = false, updatable = false)
	private OffsetDateTime createdAt;

	@Column(name = "updated_at", insertable = false, updatable = false)
	private OffsetDateTime updatedAt;

	protected AdminUser() {
	}

	public AdminUser(String email, String passwordHash, String displayName, AdminRole role) {
		this.email = email;
		this.passwordHash = passwordHash;
		this.displayName = displayName;
		this.role = role;
	}

	public Long getId() {
		return id;
	}

	public String getEmail() {
		return email;
	}

	public String getPasswordHash() {
		return passwordHash;
	}

	public String getDisplayName() {
		return displayName;
	}

	public AdminRole getRole() {
		return role;
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	public OffsetDateTime getLastLoginAt() {
		return lastLoginAt;
	}

	public void markLoggedIn() {
		this.lastLoginAt = OffsetDateTime.now();
	}

	public OffsetDateTime getCreatedAt() {
		return createdAt;
	}

	public OffsetDateTime getUpdatedAt() {
		return updatedAt;
	}
}
