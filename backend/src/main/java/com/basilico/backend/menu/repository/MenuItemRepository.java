package com.basilico.backend.menu.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.basilico.backend.menu.entity.MenuItem;

public interface MenuItemRepository extends JpaRepository<MenuItem, Long> {

	@EntityGraph(attributePaths = { "category", "dietaryTags" })
	@Query("select item from MenuItem item where item.active = true order by item.category.displayOrder, item.displayOrder")
	List<MenuItem> findActiveItemsForPublicMenu();

	@EntityGraph(attributePaths = { "category", "dietaryTags" })
	@Query("select item from MenuItem item order by item.category.displayOrder, item.displayOrder")
	List<MenuItem> findAllItemsForAdmin();

	@EntityGraph(attributePaths = { "category", "dietaryTags" })
	Optional<MenuItem> findBySlugAndActiveTrue(String slug);

	@EntityGraph(attributePaths = { "category", "dietaryTags" })
	Optional<MenuItem> findBySlug(String slug);

	@Override
	@EntityGraph(attributePaths = { "category", "dietaryTags" })
	Optional<MenuItem> findById(Long id);

	boolean existsBySlug(String slug);

	boolean existsBySlugAndIdNot(String slug, Long id);

	long countByCategorySlug(String categorySlug);
}
