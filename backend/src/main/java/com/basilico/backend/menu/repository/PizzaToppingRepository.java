package com.basilico.backend.menu.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.basilico.backend.menu.entity.PizzaTopping;

public interface PizzaToppingRepository extends JpaRepository<PizzaTopping, Long> {

	long countByCustomizerSlug(String customizerSlug);

	boolean existsByName(String name);
}
