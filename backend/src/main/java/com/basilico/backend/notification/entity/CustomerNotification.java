package com.basilico.backend.notification.entity;

import java.time.OffsetDateTime;

import com.basilico.backend.booking.entity.Booking;
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
@Table(name = "customer_notifications")
public class CustomerNotification {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private NotificationChannel channel = NotificationChannel.EMAIL;

	@Enumerated(EnumType.STRING)
	@Column(name = "notification_type", nullable = false, length = 60)
	private NotificationType notificationType;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "order_id")
	private CustomerOrder order;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "booking_id")
	private Booking booking;

	@Column(name = "recipient_email", nullable = false, length = 254)
	private String recipientEmail;

	@Column(name = "recipient_name", length = 240)
	private String recipientName;

	@Column(nullable = false, length = 240)
	private String subject;

	@Column(name = "body_text", nullable = false, columnDefinition = "text")
	private String bodyText;

	@Column(name = "body_html", columnDefinition = "text")
	private String bodyHtml;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private NotificationStatus status = NotificationStatus.PENDING;

	@Column(name = "attempt_count", nullable = false)
	private int attemptCount;

	@Column(name = "last_error", columnDefinition = "text")
	private String lastError;

	@Column(name = "deduplication_key", unique = true, length = 220)
	private String deduplicationKey;

	@Column(name = "created_at", insertable = false, updatable = false)
	private OffsetDateTime createdAt;

	@Column(name = "updated_at", insertable = false, updatable = false)
	private OffsetDateTime updatedAt;

	@Column(name = "sent_at")
	private OffsetDateTime sentAt;

	protected CustomerNotification() {
	}

	public CustomerNotification(NotificationType notificationType, CustomerOrder order,
			String recipientEmail, String recipientName, String subject, String bodyText,
			String bodyHtml, String deduplicationKey) {
		this.notificationType = notificationType;
		this.order = order;
		this.recipientEmail = recipientEmail;
		this.recipientName = recipientName;
		this.subject = subject;
		this.bodyText = bodyText;
		this.bodyHtml = bodyHtml;
		this.deduplicationKey = deduplicationKey;
	}

	public CustomerNotification(NotificationType notificationType, Booking booking,
			String recipientEmail, String recipientName, String subject, String bodyText,
			String bodyHtml, String deduplicationKey) {
		this.notificationType = notificationType;
		this.booking = booking;
		this.recipientEmail = recipientEmail;
		this.recipientName = recipientName;
		this.subject = subject;
		this.bodyText = bodyText;
		this.bodyHtml = bodyHtml;
		this.deduplicationKey = deduplicationKey;
	}

	public Long getId() {
		return id;
	}

	public NotificationChannel getChannel() {
		return channel;
	}

	public NotificationType getNotificationType() {
		return notificationType;
	}

	public CustomerOrder getOrder() {
		return order;
	}

	public Booking getBooking() {
		return booking;
	}

	public String getRecipientEmail() {
		return recipientEmail;
	}

	public String getRecipientName() {
		return recipientName;
	}

	public String getSubject() {
		return subject;
	}

	public String getBodyText() {
		return bodyText;
	}

	public String getBodyHtml() {
		return bodyHtml;
	}

	public NotificationStatus getStatus() {
		return status;
	}

	public void setStatus(NotificationStatus status) {
		this.status = status;
	}

	public int getAttemptCount() {
		return attemptCount;
	}

	public void incrementAttemptCount() {
		this.attemptCount++;
	}

	public String getLastError() {
		return lastError;
	}

	public void setLastError(String lastError) {
		this.lastError = lastError;
	}

	public String getDeduplicationKey() {
		return deduplicationKey;
	}

	public OffsetDateTime getCreatedAt() {
		return createdAt;
	}

	public OffsetDateTime getUpdatedAt() {
		return updatedAt;
	}

	public OffsetDateTime getSentAt() {
		return sentAt;
	}

	public void setSentAt(OffsetDateTime sentAt) {
		this.sentAt = sentAt;
	}
}
