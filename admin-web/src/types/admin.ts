export type DietaryTag = "V" | "GF" | "VE";
export type ProductType = "STANDARD" | "PIZZA";
export type AdminRole = "OWNER" | "MANAGER" | "STAFF";
export type FulfilmentType = "DELIVERY" | "COLLECTION";
export type OrderStatus =
  | "PENDING_PAYMENT"
  | "NEW"
  | "ACCEPTED"
  | "PREPARING"
  | "READY"
  | "COMPLETED"
  | "CANCELLED";
export type PaymentStatus = "UNPAID" | "PAID" | "FAILED" | "REFUNDED";
export type PaymentAttemptStatus =
  | "CREATED"
  | "EXPIRED"
  | "FAILED"
  | "OPEN"
  | "PAID";
export type PaymentRefundStatus =
  | "CREATED"
  | "PENDING"
  | "SUCCEEDED"
  | "FAILED"
  | "CANCELED"
  | "REQUIRES_ACTION";
export type PaymentRefundReason =
  | "CUSTOMER_REQUESTED"
  | "DUPLICATE"
  | "FRAUDULENT"
  | "OTHER";
export type PaymentProvider = "STRIPE";
export type BookingStatus =
  | "REQUESTED"
  | "CONFIRMED"
  | "DECLINED"
  | "CANCELLED"
  | "COMPLETED"
  | "NO_SHOW";
export type OrderItemType = "MENU_ITEM" | "CUSTOM_PIZZA";
export type DeliveryAreaMode = "POSTCODE_RULES" | "RADIUS";
export type DeliveryPricingMode = "FLAT_FEE" | "RADIUS_BANDS";

export type CurrentAdmin = {
  id: number;
  email: string;
  displayName: string;
  role: AdminRole;
};

export type CsrfTokenResponse = {
  token: string;
  headerName: string;
};

export type PageResponse<T> = {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
};

export type MenuItemResponse = {
  id: number;
  slug: string;
  name: string;
  description: string | null;
  pricePence: number;
  imagePath: string | null;
  productType: ProductType;
  available: boolean;
  active: boolean;
  featured: boolean;
  customizable: boolean;
  displayOrder: number;
  categorySlug: string;
  categoryName: string;
  dietaryTags: DietaryTag[];
};

export type MenuCategoryResponse = {
  id: number;
  slug: string;
  name: string;
  displayOrder: number;
  items: MenuItemResponse[];
};

export type MenuResponse = {
  categories: MenuCategoryResponse[];
  customizers: MenuCustomizerResponse[];
};

export type MenuCustomizerResponse = {
  id: number;
  slug: string;
  name: string;
  basePricePence: number;
  extraToppingPricePence: number;
  active: boolean;
  displayOrder: number;
  categorySlug: string;
  categoryName: string;
  toppings: PizzaToppingResponse[];
};

export type PizzaToppingResponse = {
  id: number;
  name: string;
  priceOverridePence: number | null;
  available: boolean;
  displayOrder: number;
};

export type MenuItemRequest = {
  categoryId: number;
  slug?: string;
  name: string;
  description: string | null;
  pricePence: number;
  imagePath: string | null;
  productType: ProductType;
  available: boolean;
  active: boolean;
  featured: boolean;
  customizable: boolean;
  displayOrder: number;
  dietaryTags: DietaryTag[];
};

export type AdminOrderSummaryResponse = {
  id: number;
  orderReference: string;
  status: OrderStatus;
  paymentStatus: PaymentStatus;
  fulfilmentType: FulfilmentType;
  customerName: string;
  deliveryDistanceMiles: number | null;
  subtotalPence: number;
  deliveryFeePence: number;
  estimatedDeliveryMinutes: number | null;
  totalPence: number;
  createdAt: string;
  updatedAt: string;
};

export type CustomerResponse = {
  firstName: string;
  lastName: string;
  phone: string;
  email: string;
};

export type DeliveryAddressResponse = {
  line1: string;
  line2: string | null;
  city: string;
  postcode: string;
};

export type OrderTimingResponse = {
  type: "ASAP" | "SCHEDULED";
  requestedDate: string | null;
  requestedTime: string | null;
};

export type OrderItemToppingResponse = {
  name: string;
  pricePence: number;
};

export type OrderItemResponse = {
  itemType: OrderItemType;
  productName: string;
  productSlug: string | null;
  unitPricePence: number;
  quantity: number;
  lineTotalPence: number;
  toppings: OrderItemToppingResponse[];
};

export type AdminOrderResponse = {
  id: number;
  orderReference: string;
  status: OrderStatus;
  paymentStatus: PaymentStatus;
  fulfilmentType: FulfilmentType;
  customer: CustomerResponse;
  deliveryAddress: DeliveryAddressResponse | null;
  timing: OrderTimingResponse;
  notes: string | null;
  deliveryDistanceMiles: number | null;
  subtotalPence: number;
  deliveryFeePence: number;
  deliveryPreparationMinutes: number | null;
  deliveryTravelMinutes: number | null;
  estimatedDeliveryMinutes: number | null;
  totalPence: number;
  items: OrderItemResponse[];
  paymentAttempts: PaymentAttemptResponse[];
  refunds: PaymentRefundResponse[];
  refundEligible: boolean;
  refundStatus: PaymentRefundStatus | null;
  refundAmountPence: number | null;
  refundReason: PaymentRefundReason | null;
  refundRequestedAt: string | null;
  refundCompletedAt: string | null;
  refundFailureReason: string | null;
  createdAt: string;
  updatedAt: string;
};

export type PaymentAttemptResponse = {
  id: number;
  provider: PaymentProvider;
  status: PaymentAttemptStatus;
  amountPence: number;
  currency: string;
  stripeCheckoutSessionId: string | null;
  stripePaymentIntentId: string | null;
  createdAt: string | null;
  updatedAt: string | null;
  completedAt: string | null;
};

export type PaymentRefundResponse = {
  id: number;
  provider: PaymentProvider;
  status: PaymentRefundStatus;
  amountPence: number;
  currency: string;
  reason: PaymentRefundReason;
  note: string | null;
  failureReason: string | null;
  createdAt: string | null;
  updatedAt: string | null;
  completedAt: string | null;
};

export type RefundOrderRequest = {
  reason: PaymentRefundReason;
  note?: string;
};

export type AdminBookingSummaryResponse = {
  id: number;
  bookingReference: string;
  status: BookingStatus;
  date: string;
  time: string;
  partySize: number;
  customerName: string;
  createdAt: string;
  updatedAt: string;
};

export type AdminBookingResponse = {
  id: number;
  bookingReference: string;
  status: BookingStatus;
  date: string;
  time: string;
  partySize: number;
  firstName: string;
  lastName: string;
  phone: string;
  email: string;
  specialRequests: string | null;
  createdAt: string;
  updatedAt: string;
};

export type AdminFulfilmentSettingsResponse = {
  id: number;
  collectionEnabled: boolean;
  deliveryEnabled: boolean;
  deliveryAreaMode: DeliveryAreaMode;
  restaurantPostcode: string | null;
  restaurantLatitude: number | null;
  restaurantLongitude: number | null;
  deliveryRadiusMiles: number | null;
  preparationTimeMinutes: number | null;
  deliveryPricingMode: DeliveryPricingMode;
  baseDeliveryRadiusMiles: number | null;
  baseDeliveryFeePence: number | null;
  extraMileFeePence: number | null;
  minimumDeliveryOrderPence: number | null;
  deliveryFeePence: number | null;
  freeDeliveryThresholdPence: number | null;
  drivingTimeConfigured: boolean;
  createdAt: string;
  updatedAt: string;
};

export type AdminFulfilmentSettingsRequest = {
  collectionEnabled: boolean;
  deliveryEnabled: boolean;
  deliveryAreaMode: DeliveryAreaMode;
  restaurantPostcode: string | null;
  restaurantLatitude: number | null;
  restaurantLongitude: number | null;
  deliveryRadiusMiles: number | null;
  preparationTimeMinutes: number | null;
  deliveryPricingMode: DeliveryPricingMode;
  baseDeliveryRadiusMiles: number | null;
  baseDeliveryFeePence: number | null;
  extraMileFeePence: number | null;
  minimumDeliveryOrderPence: number | null;
  deliveryFeePence: number | null;
  freeDeliveryThresholdPence: number | null;
};

export type DeliveryPostcodeRuleResponse = {
  id: number;
  postcodePattern: string;
  active: boolean;
  displayOrder: number;
  createdAt: string;
  updatedAt: string;
};

export type CreateDeliveryPostcodeRuleRequest = {
  postcodePattern: string;
  displayOrder?: number;
};

export type NotificationChannel = "EMAIL";
export type NotificationStatus = "PENDING" | "SENDING" | "SENT" | "FAILED";
export type NotificationType =
  | "ORDER_RECEIVED"
  | "ORDER_ACCEPTED"
  | "ORDER_READY"
  | "ORDER_CANCELLED"
  | "ORDER_REFUNDED"
  | "BOOKING_REQUEST_RECEIVED"
  | "BOOKING_CONFIRMED"
  | "BOOKING_DECLINED"
  | "BOOKING_CANCELLED"
  | "RESTAURANT_NEW_ORDER"
  | "RESTAURANT_NEW_BOOKING"
  | "MANUAL_MESSAGE";

export type NotificationResponse = {
  id: number;
  channel: NotificationChannel;
  notificationType: NotificationType;
  orderId: number | null;
  orderReference: string | null;
  bookingId: number | null;
  bookingReference: string | null;
  recipientEmail: string;
  recipientName: string | null;
  subject: string;
  bodyText: string;
  status: NotificationStatus;
  attemptCount: number;
  lastError: string | null;
  createdAt: string;
  updatedAt: string;
  sentAt: string | null;
};

export type SendManualEmailRequest = {
  orderId?: number;
  bookingId?: number;
  subject: string;
  message: string;
};
