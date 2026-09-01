package com.basilico.backend.menu.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.basilico.backend.menu.entity.PizzaTopping;

public interface PizzaToppingRepository extends JpaRepository<PizzaTopping, Long> {

	@EntityGraph(attributePaths = "customizer")
	List<PizzaTopping> findByIdIn(Collection<Long> ids);

	long countByCustomizerSlug(String customizerSlug);

	boolean existsByName(String name);
}
