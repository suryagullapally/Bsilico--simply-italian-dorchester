package com.basilico.backend.payment.entity;

import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "stripe_webhook_events")
public class StripeWebhookEvent {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "stripe_event_id", nullable = false, unique = true)
	private String stripeEventId;

	@Column(name = "event_type", nullable = false, length = 120)
	private String eventType;

	@Column(name = "processed_at", insertable = false, updatable = false)
	private OffsetDateTime processedAt;

	protected StripeWebhookEvent() {
	}

	public StripeWebhookEvent(String stripeEventId, String eventType) {
		this.stripeEventId = stripeEventId;
		this.eventType = eventType;
	}

	public Long getId() {
		return id;
	}

	public String getStripeEventId() {
		return stripeEventId;
	}

	public String getEventType() {
		return eventType;
	}

	public OffsetDateTime getProcessedAt() {
		return processedAt;
	}
}
