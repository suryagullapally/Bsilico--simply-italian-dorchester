package com.basilico.backend.payment.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.basilico.backend.payment.entity.StripeWebhookEvent;

public interface StripeWebhookEventRepository extends JpaRepository<StripeWebhookEvent, Long> {

	boolean existsByStripeEventId(String stripeEventId);
}
