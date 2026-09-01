package com.basilico.backend.payment.entity;

import java.time.OffsetDateTime;

import com.basilico.backend.order.entity.CustomerOrder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "payment_attempts")
public class PaymentAttempt {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "order_id", nullable = false)
	private CustomerOrder order;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private PaymentProvider provider = PaymentProvider.STRIPE;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private PaymentAttemptStatus status = PaymentAttemptStatus.CREATED;

	@Column(name = "amount_pence", nullable = false)
	private int amountPence;

	@Column(nullable = false, length = 3)
	private String currency = "GBP";

	@Column(name = "stripe_checkout_session_id", unique = true)
	private String stripeCheckoutSessionId;

	@Column(name = "stripe_payment_intent_id")
	private String stripePaymentIntentId;

	@Column(name = "created_at", insertable = false, updatable = false)
	private OffsetDateTime createdAt;

	@Column(name = "updated_at", insertable = false, updatable = false)
	private OffsetDateTime updatedAt;

	@Column(name = "completed_at")
	private OffsetDateTime completedAt;

	protected PaymentAttempt() {
	}

	public PaymentAttempt(CustomerOrder order, int amountPence) {
		this.order = order;
		this.amountPence = amountPence;
	}

	public Long getId() {
		return id;
	}

	public CustomerOrder getOrder() {
		return order;
	}

	public PaymentProvider getProvider() {
		return provider;
	}

	public PaymentAttemptStatus getStatus() {
		return status;
	}

	public void setStatus(PaymentAttemptStatus status) {
		this.status = status;
	}

	public int getAmountPence() {
		return amountPence;
	}

	public String getCurrency() {
		return currency;
	}

	public String getStripeCheckoutSessionId() {
		return stripeCheckoutSessionId;
	}

	public void setStripeCheckoutSessionId(String stripeCheckoutSessionId) {
		this.stripeCheckoutSessionId = stripeCheckoutSessionId;
	}

	public String getStripePaymentIntentId() {
		return stripePaymentIntentId;
	}

	public void setStripePaymentIntentId(String stripePaymentIntentId) {
		this.stripePaymentIntentId = stripePaymentIntentId;
	}

	public OffsetDateTime getCreatedAt() {
		return createdAt;
	}

	public OffsetDateTime getUpdatedAt() {
		return updatedAt;
	}

	public OffsetDateTime getCompletedAt() {
		return completedAt;
	}

	public void setCompletedAt(OffsetDateTime completedAt) {
		this.completedAt = completedAt;
	}
}
