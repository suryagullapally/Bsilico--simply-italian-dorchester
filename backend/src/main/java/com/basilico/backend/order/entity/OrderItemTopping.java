package com.basilico.backend.order.entity;

import java.time.OffsetDateTime;

import com.basilico.backend.menu.entity.PizzaTopping;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "order_item_toppings")
public class OrderItemTopping {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "order_item_id", nullable = false)
	private OrderItem orderItem;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "pizza_topping_id")
	private PizzaTopping pizzaTopping;

	@Column(name = "topping_name_snapshot", nullable = false, length = 180)
	private String toppingNameSnapshot;

	@Column(name = "price_pence_snapshot", nullable = false)
	private int pricePenceSnapshot;

	@Column(name = "created_at", insertable = false, updatable = false)
	private OffsetDateTime createdAt;

	protected OrderItemTopping() {
	}

	public OrderItemTopping(PizzaTopping pizzaTopping, String toppingNameSnapshot, int pricePenceSnapshot) {
		this.pizzaTopping = pizzaTopping;
		this.toppingNameSnapshot = toppingNameSnapshot;
		this.pricePenceSnapshot = pricePenceSnapshot;
	}

	public Long getId() {
		return id;
	}

	public OrderItem getOrderItem() {
		return orderItem;
	}

	public void setOrderItem(OrderItem orderItem) {
		this.orderItem = orderItem;
	}

	public PizzaTopping getPizzaTopping() {
		return pizzaTopping;
	}

	public String getToppingNameSnapshot() {
		return toppingNameSnapshot;
	}

	public int getPricePenceSnapshot() {
		return pricePenceSnapshot;
	}

	public OffsetDateTime getCreatedAt() {
		return createdAt;
	}
}
