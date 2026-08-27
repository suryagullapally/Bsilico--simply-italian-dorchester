import { type CartAction, type CartLine, type CartState } from "@/lib/cart/cart-types";
import { clampCartQuantity } from "@/lib/cart/cart-utils";

export const initialCartState: CartState = {
  hydrated: false,
  items: [],
};

export function cartReducer(state: CartState, action: CartAction): CartState {
  switch (action.type) {
    case "hydrate":
      return {
        hydrated: true,
        items: action.items,
      };

    case "add-line":
      return {
        ...state,
        items: addOrMergeLine(state.items, action.line),
      };

    case "increment-line":
      return {
        ...state,
        items: state.items.map((item) =>
          item.id === action.lineId
            ? { ...item, quantity: clampCartQuantity(item.quantity + 1) }
            : item,
        ),
      };

    case "decrement-line":
      return {
        ...state,
        items: state.items.map((item) =>
          item.id === action.lineId
            ? { ...item, quantity: clampCartQuantity(item.quantity - 1) }
            : item,
        ),
      };

    case "remove-line":
      return {
        ...state,
        items: state.items.filter((item) => item.id !== action.lineId),
      };

    case "clear":
      return {
        ...state,
        items: [],
      };
  }
}

function addOrMergeLine(items: CartLine[], line: CartLine) {
  const existingLine = items.find((item) => item.id === line.id);

  if (!existingLine) {
    return [...items, line];
  }

  return items.map((item) =>
    item.id === line.id
      ? {
          ...line,
          quantity: clampCartQuantity(item.quantity + line.quantity),
        }
      : item,
  );
}
