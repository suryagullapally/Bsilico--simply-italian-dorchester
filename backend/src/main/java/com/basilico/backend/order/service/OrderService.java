package com.basilico.backend.order.service;

import java.security.SecureRandom;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.basilico.backend.common.RestaurantSchedule;
import com.basilico.backend.common.dto.PageResponse;
import com.basilico.backend.common.error.BadRequestException;
import com.basilico.backend.common.error.ConflictException;
import com.basilico.backend.common.error.ResourceNotFoundException;
import com.basilico.backend.fulfilment.service.FulfilmentPricing;
import com.basilico.backend.fulfilment.service.FulfilmentService;
import com.basilico.backend.menu.entity.MenuCustomizer;
import com.basilico.backend.menu.entity.MenuItem;
import com.basilico.backend.menu.entity.PizzaTopping;
import com.basilico.backend.menu.repository.MenuCustomizerRepository;
import com.basilico.backend.menu.repository.MenuItemRepository;
import com.basilico.backend.menu.repository.PizzaToppingRepository;
import com.basilico.backend.notification.service.CustomerNotificationService;
import com.basilico.backend.order.dto.AdminOrderResponse;
import com.basilico.backend.order.dto.AdminOrderSummaryResponse;
import com.basilico.backend.order.dto.CreateOrderItemRequest;
import com.basilico.backend.order.dto.CreateOrderRequest;
import com.basilico.backend.order.dto.CustomerResponse;
import com.basilico.backend.order.dto.DeliveryAddressRequest;
import com.basilico.backend.order.dto.DeliveryAddressResponse;
import com.basilico.backend.order.dto.OrderCustomerRequest;
import com.basilico.backend.order.dto.OrderItemResponse;
import com.basilico.backend.order.dto.OrderItemToppingResponse;
import com.basilico.backend.order.dto.OrderResponse;
import com.basilico.backend.order.dto.OrderTimingRequest;
import com.basilico.backend.order.dto.OrderTimingResponse;
import com.basilico.backend.order.entity.CustomerOrder;
import com.basilico.backend.order.entity.FulfilmentType;
import com.basilico.backend.order.entity.OrderItem;
import com.basilico.backend.order.entity.OrderItemTopping;
import com.basilico.backend.order.entity.OrderItemType;
import com.basilico.backend.order.entity.OrderStatus;
import com.basilico.backend.order.entity.PaymentStatus;
import com.basilico.backend.order.entity.TimingType;
import com.basilico.backend.order.repository.CustomerOrderRepository;
import com.basilico.backend.payment.dto.PaymentAttemptResponse;
import com.basilico.backend.payment.dto.PaymentRefundResponse;
import com.basilico.backend.payment.entity.PaymentAttemptStatus;
import com.basilico.backend.payment.entity.PaymentRefund;
import com.basilico.backend.payment.entity.PaymentRefundStatus;
import com.basilico.backend.payment.repository.PaymentAttemptRepository;
import com.basilico.backend.payment.repository.PaymentRefundRepository;
import com.basilico.backend.payment.service.ManualPaymentStatusProperties;

@Service
public class OrderService {

	private static final int MAX_ADMIN_PAGE_SIZE = 100;
	private static final int REFERENCE_RANDOM_LENGTH = 6;
	private static final char[] REFERENCE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();
	private static final DateTimeFormatter REFERENCE_DATE_FORMAT = DateTimeFormatter.BASIC_ISO_DATE;

	private final CustomerOrderRepository orderRepository;
	private final MenuItemRepository menuItemRepository;
	private final MenuCustomizerRepository customizerRepository;
	private final PizzaToppingRepository toppingRepository;
	private final PaymentAttemptRepository paymentAttemptRepository;
	private final PaymentRefundRepository paymentRefundRepository;
	private final ManualPaymentStatusProperties manualPaymentStatusProperties;
	private final FulfilmentService fulfilmentService;
	private final CustomerNotificationService notificationService;
	private final RestaurantSchedule restaurantSchedule;
	private final SecureRandom secureRandom = new SecureRandom();

	public OrderService(CustomerOrderRepository orderRepository, MenuItemRepository menuItemRepository,
			MenuCustomizerRepository customizerRepository, PizzaToppingRepository toppingRepository,
			PaymentAttemptRepository paymentAttemptRepository,
			PaymentRefundRepository paymentRefundRepository,
			ManualPaymentStatusProperties manualPaymentStatusProperties,
			FulfilmentService fulfilmentService,
			CustomerNotificationService notificationService,
			RestaurantSchedule restaurantSchedule) {
		this.orderRepository = orderRepository;
		this.menuItemRepository = menuItemRepository;
		this.customizerRepository = customizerRepository;
		this.toppingRepository = toppingRepository;
		this.paymentAttemptRepository = paymentAttemptRepository;
		this.paymentRefundRepository = paymentRefundRepository;
		this.manualPaymentStatusProperties = manualPaymentStatusProperties;
		this.fulfilmentService = fulfilmentService;
		this.notificationService = notificationService;
		this.restaurantSchedule = restaurantSchedule;
	}

	@Transactional
	public OrderResponse createOrder(CreateOrderRequest request) {
		validateItemsPresent(request);

		OrderCustomerRequest customer = request.customer();
		OrderTimingRequest timing = request.timing();
		validateTiming(timing);

		CustomerOrder order = new CustomerOrder(
				generateOrderReference(),
				request.fulfilmentType(),
				trimRequired(customer.firstName(), "Customer first name is required"),
				trimRequired(customer.lastName(), "Customer last name is required"),
				trimRequired(customer.phone(), "Customer phone is required"),
				trimRequired(customer.email(), "Customer email is required"),
				timing.type()
		);
		order.setOrderNotes(blankToNull(request.notes()));

		applyTiming(order, timing);

		int subtotalPence = 0;
		for (CreateOrderItemRequest itemRequest : request.items()) {
			OrderItem item = createOrderItem(itemRequest);
			subtotalPence = checkedAdd(subtotalPence, item.getLineTotalPence());
			order.addItem(item);
		}

		order.setSubtotalPence(subtotalPence);
		FulfilmentPricing fulfilmentPricing = fulfilmentService.calculatePricing(request.fulfilmentType(),
				request.deliveryAddress(), subtotalPence);
		order.setDeliveryFeePence(fulfilmentPricing.deliveryFeePence());
		order.setTotalPence(fulfilmentPricing.totalPence());
		applyDeliveryAddress(order, request.fulfilmentType(), request.deliveryAddress(), fulfilmentPricing);
		applyDeliverySnapshot(order, fulfilmentPricing);
		return toOrderResponse(orderRepository.save(order));
	}

	@Transactional(readOnly = true)
	public PageResponse<AdminOrderSummaryResponse> getAdminOrders(OrderStatus status, PaymentStatus paymentStatus,
			FulfilmentType fulfilmentType, int page, int size) {
		int safePage = Math.max(page, 0);
		int safeSize = Math.min(Math.max(size, 1), MAX_ADMIN_PAGE_SIZE);
		PageRequest pageRequest = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));
		return PageResponse.from(orderRepository.findForAdmin(status, paymentStatus, fulfilmentType, pageRequest)
				.map(this::toAdminOrderSummaryResponse));
	}

	@Transactional(readOnly = true)
	public AdminOrderResponse getAdminOrder(Long id) {
		return orderRepository.findDetailedById(id)
				.map(this::toAdminOrderResponse)
				.orElseThrow(() -> new ResourceNotFoundException("Order not found"));
	}

	@Transactional
	public AdminOrderResponse updateStatus(Long id, OrderStatus status) {
		CustomerOrder order = findOrder(id);
		OrderStatus previousStatus = order.getStatus();
		order.setStatus(status);
		notificationService.queueOrderStatusNotification(order, previousStatus);
		return toAdminOrderResponse(order);
	}

	@Transactional
	public AdminOrderResponse updatePaymentStatus(Long id, PaymentStatus paymentStatus) {
		if (!manualPaymentStatusProperties.enabled()) {
			throw new ConflictException("Manual payment status control is disabled.");
		}

		CustomerOrder order = findOrder(id);
		order.setPaymentStatus(paymentStatus);
		return toAdminOrderResponse(order);
	}

	private void validateItemsPresent(CreateOrderRequest request) {
		if (request.items() == null || request.items().isEmpty()) {
			throw new BadRequestException("Order must contain at least one item");
		}
	}

	private void validateTiming(OrderTimingRequest timing) {
		if (timing.type() == TimingType.ASAP) {
			if (!restaurantSchedule.isAsapAvailableNow()) {
				throw new BadRequestException(
						"Basilico is currently closed. Please choose an available order time.");
			}
			return;
		}

		if (timing.type() != TimingType.SCHEDULED) {
			throw new BadRequestException("Order timing is invalid");
		}

		if (timing.requestedDate() == null || timing.requestedTime() == null) {
			throw new BadRequestException("Scheduled orders require a requested date and time");
		}

		RestaurantSchedule.ScheduleValidation validation =
				restaurantSchedule.validateScheduledOrder(timing.requestedDate(), timing.requestedTime());
		if (!validation.valid()) {
			throw new BadRequestException(validation.message());
		}
	}

	private void applyDeliveryAddress(CustomerOrder order, FulfilmentType fulfilmentType,
			DeliveryAddressRequest deliveryAddress, FulfilmentPricing fulfilmentPricing) {
		if (fulfilmentType == FulfilmentType.COLLECTION) {
			return;
		}

		if (fulfilmentType != FulfilmentType.DELIVERY) {
			throw new BadRequestException("Fulfilment type is invalid");
		}

		if (deliveryAddress == null) {
			throw new BadRequestException("Delivery address is required");
		}

		order.setDeliveryAddressLine1(trimRequired(deliveryAddress.line1(), "Delivery address line 1 is required"));
		order.setDeliveryAddressLine2(blankToNull(deliveryAddress.line2()));
		order.setDeliveryCity(trimRequired(deliveryAddress.city(), "Delivery city is required"));
		order.setDeliveryPostcode(fulfilmentPricing.normalizedPostcode());
	}

	private void applyDeliverySnapshot(CustomerOrder order, FulfilmentPricing fulfilmentPricing) {
		order.setDeliveryDistanceMiles(fulfilmentPricing.deliveryDistanceMiles());
		order.setDeliveryPreparationMinutes(fulfilmentPricing.preparationMinutes());
		order.setDeliveryTravelMinutes(fulfilmentPricing.travelMinutes());
		order.setEstimatedDeliveryMinutes(fulfilmentPricing.estimatedDeliveryMinutes());
	}

	private void applyTiming(CustomerOrder order, OrderTimingRequest timing) {
		if (timing.type() == TimingType.SCHEDULED) {
			order.setRequestedDate(timing.requestedDate());
			order.setRequestedTime(timing.requestedTime());
		}
	}

	private OrderItem createOrderItem(CreateOrderItemRequest itemRequest) {
		int quantity = itemRequest.quantity();
		if (itemRequest.type() == OrderItemType.MENU_ITEM) {
			return createMenuOrderItem(itemRequest, quantity);
		}

		if (itemRequest.type() == OrderItemType.CUSTOM_PIZZA) {
			return createCustomPizzaOrderItem(itemRequest, quantity);
		}

		throw new BadRequestException("Order item type is invalid");
	}

	private OrderItem createMenuOrderItem(CreateOrderItemRequest itemRequest, int quantity) {
		if (itemRequest.menuItemId() == null) {
			throw new BadRequestException("Menu item id is required");
		}

		MenuItem menuItem = menuItemRepository.findById(itemRequest.menuItemId())
				.orElseThrow(() -> new ResourceNotFoundException("Menu item not found"));

		if (!menuItem.isActive()) {
			throw new ConflictException(menuItem.getName() + " is not currently available to order");
		}

		if (!menuItem.isAvailable()) {
			throw new ConflictException(menuItem.getName() + " is currently unavailable");
		}

		int unitPricePence = menuItem.getPricePence();
		int lineTotalPence = checkedMultiply(unitPricePence, quantity);
		return new OrderItem(menuItem, menuItem.getName(), menuItem.getSlug(), unitPricePence, quantity,
				lineTotalPence);
	}

	private OrderItem createCustomPizzaOrderItem(CreateOrderItemRequest itemRequest, int quantity) {
		if (itemRequest.customizerId() == null) {
			throw new BadRequestException("Customizer id is required");
		}

		MenuCustomizer customizer = customizerRepository.findById(itemRequest.customizerId())
				.orElseThrow(() -> new ResourceNotFoundException("Menu customizer not found"));

		if (!customizer.isActive()) {
			throw new ConflictException(customizer.getName() + " is not currently available");
		}

		List<PizzaTopping> toppings = resolveSelectedToppings(customizer, itemRequest.toppingIds());
		int extrasPence = checkedMultiply(customizer.getExtraToppingPricePence(), toppings.size());
		int unitPricePence = checkedAdd(customizer.getBasePricePence(), extrasPence);
		int lineTotalPence = checkedMultiply(unitPricePence, quantity);

		OrderItem orderItem = new OrderItem(customizer, customizer.getName(), customizer.getSlug(), unitPricePence,
				quantity, lineTotalPence);
		for (PizzaTopping topping : toppings) {
			orderItem.addTopping(new OrderItemTopping(topping, topping.getName(),
					customizer.getExtraToppingPricePence()));
		}

		return orderItem;
	}

	private List<PizzaTopping> resolveSelectedToppings(MenuCustomizer customizer, List<Long> toppingIds) {
		if (toppingIds == null || toppingIds.isEmpty()) {
			return List.of();
		}

		Set<Long> uniqueIds = new LinkedHashSet<>(toppingIds);
		if (uniqueIds.size() != toppingIds.size()) {
			throw new BadRequestException("Duplicate pizza toppings are not allowed");
		}

		List<PizzaTopping> toppings = new ArrayList<>(toppingRepository.findByIdIn(uniqueIds));
		if (toppings.size() != uniqueIds.size()) {
			throw new ResourceNotFoundException("Pizza topping not found");
		}

		for (PizzaTopping topping : toppings) {
			if (!topping.getCustomizer().getId().equals(customizer.getId())) {
				throw new BadRequestException("Pizza topping does not belong to this customizer");
			}

			if (!topping.isAvailable()) {
				throw new ConflictException(topping.getName() + " is currently unavailable");
			}
		}

		toppings.sort(Comparator.comparingInt(PizzaTopping::getDisplayOrder));
		return toppings;
	}

	private CustomerOrder findOrder(Long id) {
		return orderRepository.findDetailedById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Order not found"));
	}

	private String generateOrderReference() {
		for (int attempt = 0; attempt < 10; attempt++) {
			String reference = "BAS-" + restaurantSchedule.currentDate().format(REFERENCE_DATE_FORMAT) + "-"
					+ randomReferenceSuffix();
			if (!orderRepository.existsByOrderReference(reference)) {
				return reference;
			}
		}

		throw new ConflictException("Could not create a unique order reference");
	}

	private String randomReferenceSuffix() {
		StringBuilder value = new StringBuilder(REFERENCE_RANDOM_LENGTH);
		for (int index = 0; index < REFERENCE_RANDOM_LENGTH; index++) {
			value.append(REFERENCE_ALPHABET[secureRandom.nextInt(REFERENCE_ALPHABET.length)]);
		}
		return value.toString();
	}

	private OrderResponse toOrderResponse(CustomerOrder order) {
		return new OrderResponse(
				order.getOrderReference(),
				order.getStatus(),
				order.getPaymentStatus(),
				order.getFulfilmentType(),
				order.getSubtotalPence(),
				order.getDeliveryFeePence(),
				order.getTotalPence(),
				toDouble(order.getDeliveryDistanceMiles()),
				order.getDeliveryPreparationMinutes(),
				order.getDeliveryTravelMinutes(),
				order.getEstimatedDeliveryMinutes(),
				toOrderItemResponses(order.getItems())
		);
	}

	private AdminOrderSummaryResponse toAdminOrderSummaryResponse(CustomerOrder order) {
		return new AdminOrderSummaryResponse(
				order.getId(),
				order.getOrderReference(),
				order.getStatus(),
				order.getPaymentStatus(),
				order.getFulfilmentType(),
				order.getCustomerFirstName() + " " + order.getCustomerLastName(),
				order.getSubtotalPence(),
				order.getDeliveryFeePence(),
				order.getTotalPence(),
				toDouble(order.getDeliveryDistanceMiles()),
				order.getEstimatedDeliveryMinutes(),
				order.getCreatedAt(),
				order.getUpdatedAt()
		);
	}

	private AdminOrderResponse toAdminOrderResponse(CustomerOrder order) {
		List<PaymentRefundResponse> refunds = toPaymentRefundResponses(order);
		PaymentRefund latestRefund = latestRefund(order).orElse(null);
		return new AdminOrderResponse(
				order.getId(),
				order.getOrderReference(),
				order.getStatus(),
				order.getPaymentStatus(),
				order.getFulfilmentType(),
				new CustomerResponse(
						order.getCustomerFirstName(),
						order.getCustomerLastName(),
						order.getCustomerPhone(),
						order.getCustomerEmail()
				),
				toDeliveryAddressResponse(order),
				new OrderTimingResponse(order.getTimingType(), order.getRequestedDate(), order.getRequestedTime()),
				order.getOrderNotes(),
				order.getSubtotalPence(),
				order.getDeliveryFeePence(),
				order.getTotalPence(),
				toDouble(order.getDeliveryDistanceMiles()),
				order.getDeliveryPreparationMinutes(),
				order.getDeliveryTravelMinutes(),
				order.getEstimatedDeliveryMinutes(),
				toOrderItemResponses(order.getItems()),
				toPaymentAttemptResponses(order),
				refunds,
				isRefundEligible(order),
				latestRefund == null ? null : latestRefund.getStatus(),
				latestRefund == null ? null : latestRefund.getAmountPence(),
				latestRefund == null ? null : latestRefund.getReason(),
				latestRefund == null ? null : latestRefund.getCreatedAt(),
				latestRefund == null ? null : latestRefund.getCompletedAt(),
				latestRefund == null ? null : latestRefund.getFailureReason(),
				order.getCreatedAt(),
				order.getUpdatedAt()
		);
	}

	private boolean isRefundEligible(CustomerOrder order) {
		if (order.getPaymentStatus() != PaymentStatus.PAID) {
			return false;
		}

		boolean hasPaidPaymentIntent = paymentAttemptRepository
				.findFirstByOrderAndStatusAndStripePaymentIntentIdIsNotNullOrderByCreatedAtDesc(
						order, PaymentAttemptStatus.PAID)
				.filter(attempt -> attempt.getAmountPence() == order.getTotalPence())
				.isPresent();
		if (!hasPaidPaymentIntent) {
			return false;
		}

		return !paymentRefundRepository.existsByOrderIdAndStatusIn(order.getId(), List.of(
				PaymentRefundStatus.CREATED,
				PaymentRefundStatus.PENDING,
				PaymentRefundStatus.REQUIRES_ACTION,
				PaymentRefundStatus.SUCCEEDED
		));
	}

	private List<PaymentAttemptResponse> toPaymentAttemptResponses(CustomerOrder order) {
		return paymentAttemptRepository.findByOrderIdOrderByCreatedAtDesc(order.getId())
				.stream()
				.map(attempt -> new PaymentAttemptResponse(
						attempt.getId(),
						attempt.getProvider(),
						attempt.getStatus(),
						attempt.getAmountPence(),
						attempt.getCurrency(),
						attempt.getStripeCheckoutSessionId(),
						attempt.getStripePaymentIntentId(),
						attempt.getCreatedAt(),
						attempt.getUpdatedAt(),
						attempt.getCompletedAt()
				))
				.toList();
	}

	private List<PaymentRefundResponse> toPaymentRefundResponses(CustomerOrder order) {
		return paymentRefundRepository.findByOrderIdOrderByCreatedAtDesc(order.getId())
				.stream()
				.map(this::toPaymentRefundResponse)
				.toList();
	}

	private PaymentRefundResponse toPaymentRefundResponse(PaymentRefund refund) {
		return new PaymentRefundResponse(
				refund.getId(),
				refund.getProvider(),
				refund.getStatus(),
				refund.getAmountPence(),
				refund.getCurrency(),
				refund.getReason(),
				refund.getNote(),
				refund.getFailureReason(),
				refund.getCreatedAt(),
				refund.getUpdatedAt(),
				refund.getCompletedAt()
		);
	}

	private java.util.Optional<PaymentRefund> latestRefund(CustomerOrder order) {
		return paymentRefundRepository.findFirstByOrderIdOrderByCreatedAtDesc(order.getId());
	}

	private DeliveryAddressResponse toDeliveryAddressResponse(CustomerOrder order) {
		if (order.getFulfilmentType() == FulfilmentType.COLLECTION) {
			return null;
		}

		return new DeliveryAddressResponse(
				order.getDeliveryAddressLine1(),
				order.getDeliveryAddressLine2(),
				order.getDeliveryCity(),
				order.getDeliveryPostcode()
		);
	}

	private List<OrderItemResponse> toOrderItemResponses(List<OrderItem> items) {
		return items.stream()
				.map(item -> new OrderItemResponse(
						item.getItemType(),
						item.getProductNameSnapshot(),
						item.getProductSlugSnapshot(),
						item.getUnitPricePence(),
						item.getQuantity(),
						item.getLineTotalPence(),
						item.getToppings()
								.stream()
								.map(topping -> new OrderItemToppingResponse(
										topping.getToppingNameSnapshot(),
										topping.getPricePenceSnapshot()
								))
								.toList()
				))
				.toList();
	}

	private String trimRequired(String value, String message) {
		if (value == null || value.isBlank()) {
			throw new BadRequestException(message);
		}

		return value.trim();
	}

	private String blankToNull(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}

		return value.trim();
	}

	private int checkedAdd(int left, int right) {
		return Math.toIntExact((long) left + right);
	}

	private int checkedMultiply(int pricePence, int quantity) {
		return Math.toIntExact((long) pricePence * quantity);
	}

	private Double toDouble(java.math.BigDecimal value) {
		return value == null ? null : value.doubleValue();
	}
}
