package com.basilico.backend.fulfilment.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.basilico.backend.fulfilment.entity.DeliveryPostcodeRule;

public interface DeliveryPostcodeRuleRepository extends JpaRepository<DeliveryPostcodeRule, Long> {

	List<DeliveryPostcodeRule> findAllByOrderByDisplayOrderAsc();

	List<DeliveryPostcodeRule> findByActiveTrueOrderByDisplayOrderAsc();

	boolean existsByActiveTrue();

	boolean existsByPostcodePattern(String postcodePattern);

	Optional<DeliveryPostcodeRule> findByPostcodePattern(String postcodePattern);
}
