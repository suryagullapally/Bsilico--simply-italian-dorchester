package com.basilico.backend.payment.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.basilico.backend.payment.entity.PaymentRefund;
import com.basilico.backend.payment.entity.PaymentRefundStatus;

public interface PaymentRefundRepository extends JpaRepository<PaymentRefund, Long> {

	List<PaymentRefund> findByOrderIdOrderByCreatedAtDesc(Long orderId);

	Optional<PaymentRefund> findFirstByOrderIdOrderByCreatedAtDesc(Long orderId);

	Optional<PaymentRefund> findByStripeRefundId(String stripeRefundId);

	boolean existsByOrderIdAndStatusIn(Long orderId, Collection<PaymentRefundStatus> statuses);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select refund from PaymentRefund refund where refund.id = :id")
	Optional<PaymentRefund> findByIdForUpdate(@Param("id") Long id);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select refund from PaymentRefund refund where refund.stripeRefundId = :stripeRefundId")
	Optional<PaymentRefund> findByStripeRefundIdForUpdate(@Param("stripeRefundId") String stripeRefundId);

	@Query("""
			select refund from PaymentRefund refund
			where refund.paymentAttempt.stripePaymentIntentId = :paymentIntentId
			order by refund.createdAt desc, refund.id desc
			""")
	List<PaymentRefund> findByStripePaymentIntentId(@Param("paymentIntentId") String paymentIntentId);
}
