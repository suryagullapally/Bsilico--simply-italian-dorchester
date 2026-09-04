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
@Table(name = "payment_refunds")
public class PaymentRefund {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "order_id", nullable = false)
	private CustomerOrder order;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "payment_attempt_id", nullable = false)
	private PaymentAttempt paymentAttempt;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private PaymentProvider provider = PaymentProvider.STRIPE;

	@Column(name = "stripe_refund_id", unique = true)
	private String stripeRefundId;

	@Column(name = "amount_pence", nullable = false)
	private int amountPence;

	@Column(nullable = false, length = 3)
	private String currency = "GBP";

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private PaymentRefundStatus status = PaymentRefundStatus.CREATED;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 40)
	private PaymentRefundReason reason;

	@Column(columnDefinition = "text")
	private String note;

	@Column(name = "failure_reason", columnDefinition = "text")
	private String failureReason;

	@Column(name = "created_at", insertable = false, updatable = false)
	private OffsetDateTime createdAt;

	@Column(name = "updated_at", insertable = false, updatable = false)
	private OffsetDateTime updatedAt;

	@Column(name = "completed_at")
	private OffsetDateTime completedAt;

	protected PaymentRefund() {
	}

	public PaymentRefund(CustomerOrder order, PaymentAttempt paymentAttempt, int amountPence,
			PaymentRefundReason reason, String note) {
		this.order = order;
		this.paymentAttempt = paymentAttempt;
		this.amountPence = amountPence;
		this.reason = reason;
		this.note = note;
	}

	public Long getId() {
		return id;
	}

	public CustomerOrder getOrder() {
		return order;
	}

	public PaymentAttempt getPaymentAttempt() {
		return paymentAttempt;
	}

	public PaymentProvider getProvider() {
		return provider;
	}

	public String getStripeRefundId() {
		return stripeRefundId;
	}

	public void setStripeRefundId(String stripeRefundId) {
		this.stripeRefundId = stripeRefundId;
	}

	public int getAmountPence() {
		return amountPence;
	}

	public String getCurrency() {
		return currency;
	}

	public PaymentRefundStatus getStatus() {
		return status;
	}

	public void setStatus(PaymentRefundStatus status) {
		this.status = status;
	}

	public PaymentRefundReason getReason() {
		return reason;
	}

	public String getNote() {
		return note;
	}

	public String getFailureReason() {
		return failureReason;
	}

	public void setFailureReason(String failureReason) {
		this.failureReason = failureReason;
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
