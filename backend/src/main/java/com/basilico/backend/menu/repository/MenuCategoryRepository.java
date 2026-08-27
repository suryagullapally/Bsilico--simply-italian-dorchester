package com.basilico.backend.menu.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.basilico.backend.menu.entity.MenuCategory;

public interface MenuCategoryRepository extends JpaRepository<MenuCategory, Long> {

	List<MenuCategory> findByActiveTrueOrderByDisplayOrderAsc();

	Optional<MenuCategory> findBySlug(String slug);
}
