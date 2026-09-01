package com.basilico.backend.payment.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.basilico.backend.order.entity.CustomerOrder;
import com.basilico.backend.payment.entity.PaymentAttempt;
import com.basilico.backend.payment.entity.PaymentAttemptStatus;

public interface PaymentAttemptRepository extends JpaRepository<PaymentAttempt, Long> {

	Optional<PaymentAttempt> findByStripeCheckoutSessionId(String stripeCheckoutSessionId);

	Optional<PaymentAttempt> findFirstByOrderAndStatusOrderByCreatedAtDesc(CustomerOrder order,
			PaymentAttemptStatus status);

	List<PaymentAttempt> findByOrderIdOrderByCreatedAtDesc(Long orderId);
}
