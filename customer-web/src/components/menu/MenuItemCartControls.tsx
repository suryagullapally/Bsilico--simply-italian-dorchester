"use client";

import type { KeyboardEvent, MouseEvent } from "react";
import { useCart } from "@/components/cart/CartProvider";
import { CART_MAX_QUANTITY } from "@/lib/cart/cart-types";
import type { MenuItem } from "@/types/menu";

type MenuItemCartControlsProps = {
  item: MenuItem;
};

export function MenuItemCartControls({ item }: MenuItemCartControlsProps) {
  const { getMenuItemQuantity, hydrated, setMenuItemQuantity } = useCart();
  const quantity = hydrated ? getMenuItemQuantity(item) : 0;
  const isSoldOut = !item.available;
  const canQuickAdd = item.available && !item.customizable;

  function stopCardNavigation(
    event: KeyboardEvent<HTMLButtonElement> | MouseEvent<HTMLButtonElement>,
  ) {
    event.preventDefault();
    event.stopPropagation();
  }

  function setQuantity(nextQuantity: number) {
    setMenuItemQuantity(item, nextQuantity);
  }

  function handleKeyboardActivation(
    event: KeyboardEvent<HTMLButtonElement>,
    nextQuantity: number,
  ) {
    if (event.key !== "Enter" && event.key !== " ") {
      return;
    }

    stopCardNavigation(event);

    if (!event.repeat) {
      setQuantity(nextQuantity);
    }
  }

  if (isSoldOut) {
    return null;
  }

  if (!canQuickAdd) {
    return (
      <span className="menu-item-cart-control menu-item-cart-control--custom">
        Customise
      </span>
    );
  }

  if (quantity <= 0) {
    return (
      <div className="menu-item-cart-control">
        <button
          aria-label={`Add ${item.name} to basket`}
          className="menu-item-cart-control__add"
          disabled={!hydrated}
          onClick={(event) => {
            stopCardNavigation(event);
            setQuantity(1);
          }}
          onKeyDown={(event) => handleKeyboardActivation(event, 1)}
          type="button"
        >
          ADD TO BASKET
        </button>
      </div>
    );
  }

  return (
    <div
      aria-label={`Basket quantity for ${item.name}`}
      className="menu-item-cart-control menu-item-cart-control--quantity"
      role="group"
    >
      <button
        aria-label={`Decrease ${item.name} quantity`}
        className="menu-item-cart-control__button"
        onClick={(event) => {
          stopCardNavigation(event);
          setQuantity(quantity - 1);
        }}
        onKeyDown={(event) => handleKeyboardActivation(event, quantity - 1)}
        type="button"
      >
        −
      </button>
      <span className="menu-item-cart-control__value" aria-live="polite">
        {quantity}
      </span>
      <button
        aria-label={`Increase ${item.name} quantity`}
        className="menu-item-cart-control__button"
        disabled={quantity >= CART_MAX_QUANTITY}
        onClick={(event) => {
          stopCardNavigation(event);
          setQuantity(quantity + 1);
        }}
        onKeyDown={(event) => handleKeyboardActivation(event, quantity + 1)}
        type="button"
      >
        +
      </button>
    </div>
  );
}
