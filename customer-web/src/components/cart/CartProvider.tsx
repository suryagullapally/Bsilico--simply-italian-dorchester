"use client";

import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useReducer,
  type ReactNode,
} from "react";
import {
  CART_STORAGE_KEY,
  type AddCustomPizzaInput,
  type CartLine,
} from "@/lib/cart/cart-types";
import { cartReducer, initialCartState } from "@/lib/cart/cart-reducer";
import {
  createCustomPizzaCartLine,
  createMenuCartLine,
  getCartItemCount,
  getCartSubtotalPennies,
  getMenuCartLineId,
  getMenuItemCartQuantity,
  parsePersistedCart,
  serializeCart,
} from "@/lib/cart/cart-utils";
import type { MenuItem } from "@/types/menu";

type CartContextValue = {
  addCustomPizza: (input: AddCustomPizzaInput) => void;
  addMenuItem: (item: MenuItem, quantity?: number) => void;
  clearCart: () => void;
  decrementLine: (lineId: string) => void;
  getMenuItemQuantity: (item: MenuItem) => number;
  hydrated: boolean;
  incrementLine: (lineId: string) => void;
  itemCount: number;
  items: CartLine[];
  removeLine: (lineId: string) => void;
  setMenuItemQuantity: (item: MenuItem, quantity: number) => void;
  subtotalPennies: number;
};

const CartContext = createContext<CartContextValue | null>(null);

type CartProviderProps = {
  children: ReactNode;
};

export function CartProvider({ children }: CartProviderProps) {
  const [state, dispatch] = useReducer(cartReducer, initialCartState);

  useEffect(() => {
    const storedCart = readStoredCart();

    dispatch({
      items: storedCart,
      type: "hydrate",
    });
  }, []);

  useEffect(() => {
    if (!state.hydrated) {
      return;
    }

    writeStoredCart(state.items);
  }, [state.hydrated, state.items]);

  const addMenuItem = useCallback((item: MenuItem, quantity = 1) => {
    dispatch({
      line: createMenuCartLine({ item, quantity }),
      type: "add-line",
    });
  }, []);

  const addCustomPizza = useCallback((input: AddCustomPizzaInput) => {
    dispatch({
      line: createCustomPizzaCartLine(input),
      type: "add-line",
    });
  }, []);

  const incrementLine = useCallback((lineId: string) => {
    dispatch({ lineId, type: "increment-line" });
  }, []);

  const decrementLine = useCallback((lineId: string) => {
    dispatch({ lineId, type: "decrement-line" });
  }, []);

  const setMenuItemQuantity = useCallback((item: MenuItem, quantity: number) => {
    if (!item.available && quantity > 0) {
      return;
    }

    const lineId = getMenuCartLineId(item);

    dispatch({
      line: quantity > 0 ? createMenuCartLine({ item, quantity }) : undefined,
      lineId,
      quantity,
      type: "set-line-quantity",
    });
  }, []);

  const getMenuItemQuantity = useCallback(
    (item: MenuItem) => getMenuItemCartQuantity(state.items, item),
    [state.items],
  );

  const removeLine = useCallback((lineId: string) => {
    dispatch({ lineId, type: "remove-line" });
  }, []);

  const clearCart = useCallback(() => {
    dispatch({ type: "clear" });
  }, []);

  const value = useMemo(
    () => ({
      addCustomPizza,
      addMenuItem,
      clearCart,
      decrementLine,
      getMenuItemQuantity,
      hydrated: state.hydrated,
      incrementLine,
      itemCount: getCartItemCount(state.items),
      items: state.items,
      removeLine,
      setMenuItemQuantity,
      subtotalPennies: getCartSubtotalPennies(state.items),
    }),
    [
      addCustomPizza,
      addMenuItem,
      clearCart,
      decrementLine,
      getMenuItemQuantity,
      incrementLine,
      removeLine,
      setMenuItemQuantity,
      state.hydrated,
      state.items,
    ],
  );

  return <CartContext.Provider value={value}>{children}</CartContext.Provider>;
}

export function useCart() {
  const value = useContext(CartContext);

  if (!value) {
    throw new Error("useCart must be used within CartProvider");
  }

  return value;
}

function readStoredCart() {
  try {
    return parsePersistedCart(window.localStorage.getItem(CART_STORAGE_KEY));
  } catch {
    return [];
  }
}

function writeStoredCart(items: CartLine[]) {
  try {
    window.localStorage.setItem(
      CART_STORAGE_KEY,
      JSON.stringify(serializeCart(items)),
    );
  } catch {
    // If storage is unavailable, the in-memory basket still works for this tab.
  }
}
