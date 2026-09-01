package com.basilico.backend.order.entity;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import com.basilico.backend.menu.entity.MenuCustomizer;
import com.basilico.backend.menu.entity.MenuItem;

import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

@Entity
@Table(name = "order_items")
public class OrderItem {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "order_id", nullable = false)
	private CustomerOrder order;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "menu_item_id")
	private MenuItem menuItem;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "customizer_id")
	private MenuCustomizer customizer;

	@Enumerated(EnumType.STRING)
	@Column(name = "item_type", nullable = false, length = 30)
	private OrderItemType itemType;

	@Column(name = "product_name_snapshot", nullable = false, length = 220)
	private String productNameSnapshot;

	@Column(name = "product_slug_snapshot", nullable = false, length = 160)
	private String productSlugSnapshot;

	@Column(name = "unit_price_pence", nullable = false)
	private int unitPricePence;

	@Column(nullable = false)
	private int quantity;

	@Column(name = "line_total_pence", nullable = false)
	private int lineTotalPence;

	@OneToMany(mappedBy = "orderItem", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("id ASC")
	private List<OrderItemTopping> toppings = new ArrayList<>();

	@Column(name = "created_at", insertable = false, updatable = false)
	private OffsetDateTime createdAt;

	protected OrderItem() {
	}

	public OrderItem(MenuItem menuItem, String productNameSnapshot, String productSlugSnapshot, int unitPricePence,
			int quantity, int lineTotalPence) {
		this.menuItem = menuItem;
		this.itemType = OrderItemType.MENU_ITEM;
		this.productNameSnapshot = productNameSnapshot;
		this.productSlugSnapshot = productSlugSnapshot;
		this.unitPricePence = unitPricePence;
		this.quantity = quantity;
		this.lineTotalPence = lineTotalPence;
	}

	public OrderItem(MenuCustomizer customizer, String productNameSnapshot, String productSlugSnapshot,
			int unitPricePence, int quantity, int lineTotalPence) {
		this.customizer = customizer;
		this.itemType = OrderItemType.CUSTOM_PIZZA;
		this.productNameSnapshot = productNameSnapshot;
		this.productSlugSnapshot = productSlugSnapshot;
		this.unitPricePence = unitPricePence;
		this.quantity = quantity;
		this.lineTotalPence = lineTotalPence;
	}

	public Long getId() {
		return id;
	}

	public CustomerOrder getOrder() {
		return order;
	}

	public void setOrder(CustomerOrder order) {
		this.order = order;
	}

	public MenuItem getMenuItem() {
		return menuItem;
	}

	public MenuCustomizer getCustomizer() {
		return customizer;
	}

	public OrderItemType getItemType() {
		return itemType;
	}

	public String getProductNameSnapshot() {
		return productNameSnapshot;
	}

	public String getProductSlugSnapshot() {
		return productSlugSnapshot;
	}

	public int getUnitPricePence() {
		return unitPricePence;
	}

	public int getQuantity() {
		return quantity;
	}

	public int getLineTotalPence() {
		return lineTotalPence;
	}

	public List<OrderItemTopping> getToppings() {
		return toppings;
	}

	public void addTopping(OrderItemTopping topping) {
		topping.setOrderItem(this);
		this.toppings.add(topping);
	}

	public OffsetDateTime getCreatedAt() {
		return createdAt;
	}
}
