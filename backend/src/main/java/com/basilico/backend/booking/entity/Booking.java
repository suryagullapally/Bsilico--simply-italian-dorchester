package com.basilico.backend.booking.entity;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "bookings")
public class Booking {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "booking_reference", nullable = false, unique = true, length = 32)
	private String bookingReference;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private BookingStatus status = BookingStatus.REQUESTED;

	@Column(name = "booking_date", nullable = false)
	private LocalDate bookingDate;

	@Column(name = "booking_time", nullable = false)
	private LocalTime bookingTime;

	@Column(name = "party_size", nullable = false)
	private int partySize;

	@Column(name = "first_name", nullable = false, length = 120)
	private String firstName;

	@Column(name = "last_name", nullable = false, length = 120)
	private String lastName;

	@Column(nullable = false, length = 60)
	private String phone;

	@Column(nullable = false, length = 254)
	private String email;

	@Column(name = "special_requests", columnDefinition = "text")
	private String specialRequests;

	@Column(name = "created_at", insertable = false, updatable = false)
	private OffsetDateTime createdAt;

	@Column(name = "updated_at", insertable = false, updatable = false)
	private OffsetDateTime updatedAt;

	protected Booking() {
	}

	public Booking(String bookingReference, LocalDate bookingDate, LocalTime bookingTime, int partySize,
			String firstName, String lastName, String phone, String email) {
		this.bookingReference = bookingReference;
		this.bookingDate = bookingDate;
		this.bookingTime = bookingTime;
		this.partySize = partySize;
		this.firstName = firstName;
		this.lastName = lastName;
		this.phone = phone;
		this.email = email;
	}

	public Long getId() {
		return id;
	}

	public String getBookingReference() {
		return bookingReference;
	}

	public BookingStatus getStatus() {
		return status;
	}

	public void setStatus(BookingStatus status) {
		this.status = status;
	}

	public LocalDate getBookingDate() {
		return bookingDate;
	}

	public LocalTime getBookingTime() {
		return bookingTime;
	}

	public int getPartySize() {
		return partySize;
	}

	public String getFirstName() {
		return firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public String getPhone() {
		return phone;
	}

	public String getEmail() {
		return email;
	}

	public String getSpecialRequests() {
		return specialRequests;
	}

	public void setSpecialRequests(String specialRequests) {
		this.specialRequests = specialRequests;
	}

	public OffsetDateTime getCreatedAt() {
		return createdAt;
	}

	public OffsetDateTime getUpdatedAt() {
		return updatedAt;
	}
}
