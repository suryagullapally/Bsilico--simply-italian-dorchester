package com.basilico.backend.menu.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.basilico.backend.menu.entity.MenuCustomizer;

public interface MenuCustomizerRepository extends JpaRepository<MenuCustomizer, Long> {

	@EntityGraph(attributePaths = { "category", "toppings" })
	@Query("select customizer from MenuCustomizer customizer where customizer.active = true order by customizer.category.displayOrder, customizer.displayOrder")
	List<MenuCustomizer> findActiveCustomizersForPublicMenu();

	@EntityGraph(attributePaths = { "category", "toppings" })
	Optional<MenuCustomizer> findBySlugAndActiveTrue(String slug);

	long countBySlug(String slug);
}
