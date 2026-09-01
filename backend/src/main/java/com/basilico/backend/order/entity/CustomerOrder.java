package com.basilico.backend.order.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

@Entity
@Table(name = "orders")
public class CustomerOrder {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "order_reference", nullable = false, unique = true, length = 32)
	private String orderReference;

	@Enumerated(EnumType.STRING)
	@Column(name = "fulfilment_type", nullable = false, length = 20)
	private FulfilmentType fulfilmentType;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private OrderStatus status = OrderStatus.PENDING_PAYMENT;

	@Enumerated(EnumType.STRING)
	@Column(name = "payment_status", nullable = false, length = 20)
	private PaymentStatus paymentStatus = PaymentStatus.UNPAID;

	@Column(name = "customer_first_name", nullable = false, length = 120)
	private String customerFirstName;

	@Column(name = "customer_last_name", nullable = false, length = 120)
	private String customerLastName;

	@Column(name = "customer_phone", nullable = false, length = 60)
	private String customerPhone;

	@Column(name = "customer_email", nullable = false, length = 254)
	private String customerEmail;

	@Column(name = "delivery_address_line1", length = 220)
	private String deliveryAddressLine1;

	@Column(name = "delivery_address_line2", length = 220)
	private String deliveryAddressLine2;

	@Column(name = "delivery_city", length = 120)
	private String deliveryCity;

	@Column(name = "delivery_postcode", length = 20)
	private String deliveryPostcode;

	@Enumerated(EnumType.STRING)
	@Column(name = "timing_type", nullable = false, length = 20)
	private TimingType timingType;

	@Column(name = "requested_date")
	private LocalDate requestedDate;

	@Column(name = "requested_time")
	private LocalTime requestedTime;

	@Column(name = "order_notes", columnDefinition = "text")
	private String orderNotes;

	@Column(name = "subtotal_pence", nullable = false)
	private int subtotalPence;

	@Column(name = "delivery_fee_pence", nullable = false)
	private int deliveryFeePence;

	@Column(name = "total_pence", nullable = false)
	private int totalPence;

	@Column(name = "delivery_distance_miles", precision = 6, scale = 2)
	private BigDecimal deliveryDistanceMiles;

	@Column(name = "delivery_preparation_minutes")
	private Integer deliveryPreparationMinutes;

	@Column(name = "delivery_travel_minutes")
	private Integer deliveryTravelMinutes;

	@Column(name = "estimated_delivery_minutes")
	private Integer estimatedDeliveryMinutes;

	@OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("id ASC")
	private List<OrderItem> items = new ArrayList<>();

	@Column(name = "created_at", insertable = false, updatable = false)
	private OffsetDateTime createdAt;

	@Column(name = "updated_at", insertable = false, updatable = false)
	private OffsetDateTime updatedAt;

	protected CustomerOrder() {
	}

	public CustomerOrder(String orderReference, FulfilmentType fulfilmentType, String customerFirstName,
			String customerLastName, String customerPhone, String customerEmail, TimingType timingType) {
		this.orderReference = orderReference;
		this.fulfilmentType = fulfilmentType;
		this.customerFirstName = customerFirstName;
		this.customerLastName = customerLastName;
		this.customerPhone = customerPhone;
		this.customerEmail = customerEmail;
		this.timingType = timingType;
	}

	public Long getId() {
		return id;
	}

	public String getOrderReference() {
		return orderReference;
	}

	public FulfilmentType getFulfilmentType() {
		return fulfilmentType;
	}

	public OrderStatus getStatus() {
		return status;
	}

	public void setStatus(OrderStatus status) {
		this.status = status;
	}

	public PaymentStatus getPaymentStatus() {
		return paymentStatus;
	}

	public void setPaymentStatus(PaymentStatus paymentStatus) {
		this.paymentStatus = paymentStatus;
	}

	public String getCustomerFirstName() {
		return customerFirstName;
	}

	public String getCustomerLastName() {
		return customerLastName;
	}

	public String getCustomerPhone() {
		return customerPhone;
	}

	public String getCustomerEmail() {
		return customerEmail;
	}

	public String getDeliveryAddressLine1() {
		return deliveryAddressLine1;
	}

	public void setDeliveryAddressLine1(String deliveryAddressLine1) {
		this.deliveryAddressLine1 = deliveryAddressLine1;
	}

	public String getDeliveryAddressLine2() {
		return deliveryAddressLine2;
	}

	public void setDeliveryAddressLine2(String deliveryAddressLine2) {
		this.deliveryAddressLine2 = deliveryAddressLine2;
	}

	public String getDeliveryCity() {
		return deliveryCity;
	}

	public void setDeliveryCity(String deliveryCity) {
		this.deliveryCity = deliveryCity;
	}

	public String getDeliveryPostcode() {
		return deliveryPostcode;
	}

	public void setDeliveryPostcode(String deliveryPostcode) {
		this.deliveryPostcode = deliveryPostcode;
	}

	public TimingType getTimingType() {
		return timingType;
	}

	public LocalDate getRequestedDate() {
		return requestedDate;
	}

	public void setRequestedDate(LocalDate requestedDate) {
		this.requestedDate = requestedDate;
	}

	public LocalTime getRequestedTime() {
		return requestedTime;
	}

	public void setRequestedTime(LocalTime requestedTime) {
		this.requestedTime = requestedTime;
	}

	public String getOrderNotes() {
		return orderNotes;
	}

	public void setOrderNotes(String orderNotes) {
		this.orderNotes = orderNotes;
	}

	public int getSubtotalPence() {
		return subtotalPence;
	}

	public void setSubtotalPence(int subtotalPence) {
		this.subtotalPence = subtotalPence;
	}

	public int getDeliveryFeePence() {
		return deliveryFeePence;
	}

	public void setDeliveryFeePence(int deliveryFeePence) {
		this.deliveryFeePence = deliveryFeePence;
	}

	public int getTotalPence() {
		return totalPence;
	}

	public void setTotalPence(int totalPence) {
		this.totalPence = totalPence;
	}

	public BigDecimal getDeliveryDistanceMiles() {
		return deliveryDistanceMiles;
	}

	public void setDeliveryDistanceMiles(BigDecimal deliveryDistanceMiles) {
		this.deliveryDistanceMiles = deliveryDistanceMiles;
	}

	public Integer getDeliveryPreparationMinutes() {
		return deliveryPreparationMinutes;
	}

	public void setDeliveryPreparationMinutes(Integer deliveryPreparationMinutes) {
		this.deliveryPreparationMinutes = deliveryPreparationMinutes;
	}

	public Integer getDeliveryTravelMinutes() {
		return deliveryTravelMinutes;
	}

	public void setDeliveryTravelMinutes(Integer deliveryTravelMinutes) {
		this.deliveryTravelMinutes = deliveryTravelMinutes;
	}

	public Integer getEstimatedDeliveryMinutes() {
		return estimatedDeliveryMinutes;
	}

	public void setEstimatedDeliveryMinutes(Integer estimatedDeliveryMinutes) {
		this.estimatedDeliveryMinutes = estimatedDeliveryMinutes;
	}

	public List<OrderItem> getItems() {
		return items;
	}

	public void addItem(OrderItem item) {
		item.setOrder(this);
		this.items.add(item);
	}

	public OffsetDateTime getCreatedAt() {
		return createdAt;
	}

	public OffsetDateTime getUpdatedAt() {
		return updatedAt;
	}
}
