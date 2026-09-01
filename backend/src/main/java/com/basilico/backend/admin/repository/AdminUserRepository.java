package com.basilico.backend.admin.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.basilico.backend.admin.entity.AdminUser;

public interface AdminUserRepository extends JpaRepository<AdminUser, Long> {

	Optional<AdminUser> findByEmail(String email);
}
