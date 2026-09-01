package com.basilico.backend.fulfilment.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.basilico.backend.fulfilment.entity.FulfilmentSettings;

public interface FulfilmentSettingsRepository extends JpaRepository<FulfilmentSettings, Long> {

	Optional<FulfilmentSettings> findFirstByOrderByIdAsc();
}
