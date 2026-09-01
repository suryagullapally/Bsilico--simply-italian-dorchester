import type { CartLine } from "@/lib/cart/cart-types";
import type { CheckoutState } from "@/lib/checkout/checkout-types";
import type {
  BackendCreateOrderItemRequest,
  BackendCreateOrderRequest,
} from "@/types/backend-order";

export class CheckoutOrderRequestError extends Error {
  constructor(message: string) {
    super(message);
    this.name = "CheckoutOrderRequestError";
  }
}

export function buildCreateOrderRequest(
  checkoutState: CheckoutState,
  cartItems: readonly CartLine[],
): BackendCreateOrderRequest {
  const fulfilmentType = checkoutState.fulfilmentType.toUpperCase();

  if (fulfilmentType !== "COLLECTION" && fulfilmentType !== "DELIVERY") {
    throw new CheckoutOrderRequestError("Choose delivery or collection.");
  }

  const request: BackendCreateOrderRequest = {
    customer: {
      email: checkoutState.customer.email,
      firstName: checkoutState.customer.firstName,
      lastName: checkoutState.customer.lastName,
      phone: checkoutState.customer.phone,
    },
    fulfilmentType,
    items: cartItems.map(toOrderItemRequest),
    notes: checkoutState.notes || undefined,
    timing:
      checkoutState.timing.type === "scheduled"
        ? {
            requestedDate: checkoutState.timing.requestedDate,
            requestedTime: checkoutState.timing.requestedTime,
            type: "SCHEDULED",
          }
        : {
            type: "ASAP",
          },
  };

  if (fulfilmentType === "DELIVERY") {
    request.deliveryAddress = {
      city: checkoutState.deliveryAddress.city,
      line1: checkoutState.deliveryAddress.line1,
      line2: checkoutState.deliveryAddress.line2 || undefined,
      postcode: checkoutState.deliveryAddress.postcode,
    };
  }

  return request;
}

function toOrderItemRequest(item: CartLine): BackendCreateOrderItemRequest {
  if (item.lineType === "menu-item") {
    if (!isPositiveInteger(item.menuItemId)) {
      throw new CheckoutOrderRequestError(
        `${item.name} needs to be added again before checkout.`,
      );
    }

    return {
      menuItemId: item.menuItemId,
      quantity: item.quantity,
      type: "MENU_ITEM",
    };
  }

  if (!isPositiveInteger(item.customizerId)) {
    throw new CheckoutOrderRequestError(
      "Your custom pizza needs to be added again before checkout.",
    );
  }

  return {
    customizerId: item.customizerId,
    quantity: item.quantity,
    toppingIds: item.selectedToppingIds ?? [],
    type: "CUSTOM_PIZZA",
  };
}

function isPositiveInteger(value: unknown): value is number {
  return typeof value === "number" && Number.isInteger(value) && value > 0;
}
