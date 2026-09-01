export type BackendFulfilmentType = "COLLECTION" | "DELIVERY";
export type BackendOrderItemType = "CUSTOM_PIZZA" | "MENU_ITEM";
export type BackendOrderStatus =
  | "ACCEPTED"
  | "CANCELLED"
  | "COMPLETED"
  | "NEW"
  | "PENDING_PAYMENT"
  | "PREPARING"
  | "READY";
export type BackendPaymentStatus = "FAILED" | "PAID" | "REFUNDED" | "UNPAID";
export type BackendTimingType = "ASAP" | "SCHEDULED";

export type BackendCreateOrderRequest = {
  customer: {
    email: string;
    firstName: string;
    lastName: string;
    phone: string;
  };
  deliveryAddress?: {
    city: string;
    line1: string;
    line2?: string;
    postcode: string;
  };
  fulfilmentType: BackendFulfilmentType;
  items: BackendCreateOrderItemRequest[];
  notes?: string;
  timing: BackendOrderTimingRequest;
};

export type BackendOrderTimingRequest =
  | {
      type: "ASAP";
    }
  | {
      requestedDate: string;
      requestedTime: string;
      type: "SCHEDULED";
    };

export type BackendCreateOrderItemRequest =
  | {
      menuItemId: number;
      quantity: number;
      type: "MENU_ITEM";
    }
  | {
      customizerId: number;
      quantity: number;
      toppingIds: number[];
      type: "CUSTOM_PIZZA";
    };

export type BackendOrderResponse = {
  deliveryDistanceMiles: number | null;
  fulfilmentType: BackendFulfilmentType;
  items: BackendOrderItemResponse[];
  orderReference: string;
  paymentStatus: BackendPaymentStatus;
  status: BackendOrderStatus;
  deliveryFeePence: number;
  deliveryPreparationMinutes: number | null;
  deliveryTravelMinutes: number | null;
  estimatedDeliveryMinutes: number | null;
  subtotalPence: number;
  totalPence: number;
};

export type BackendOrderItemResponse = {
  itemType: BackendOrderItemType;
  lineTotalPence: number;
  productName: string;
  productSlug: string;
  quantity: number;
  toppings: BackendOrderItemToppingResponse[];
  unitPricePence: number;
};

export type BackendOrderItemToppingResponse = {
  name: string;
  pricePence: number;
};
