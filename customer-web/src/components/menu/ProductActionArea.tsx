"use client";

import Link from "next/link";
import { useState } from "react";
import { useCart } from "@/components/cart/CartProvider";
import { Button } from "@/components/ui/Button";
import { CART_MAX_QUANTITY } from "@/lib/cart/cart-types";
import { formatGbpPennies } from "@/lib/format-price";
import { routes } from "@/lib/routes";
import type { MenuItem } from "@/types/menu";

type ProductActionAreaProps = {
  item: MenuItem;
};

export function ProductActionArea({ item }: ProductActionAreaProps) {
  const isPizza = item.productType === "pizza";
  const isSoldOut = !item.available;
  const [added, setAdded] = useState(false);
  const { getMenuItemQuantity, hydrated, setMenuItemQuantity } = useCart();
  const quantity = hydrated ? getMenuItemQuantity(item) : 0;
  const unitPricePennies = item.pricePence;
  const linePricePennies = unitPricePennies * Math.max(quantity, 1);
  const canQuickAdd = item.available && !item.customizable;

  function decreaseQuantity() {
    setAdded(false);
    setMenuItemQuantity(item, quantity - 1);
  }

  function increaseQuantity() {
    setAdded(false);
    setMenuItemQuantity(item, quantity + 1);
  }

  function handleAddToOrder() {
    if (!canQuickAdd) {
      return;
    }

    setMenuItemQuantity(item, 1);
    setAdded(true);
  }

  return (
    <section className="product-action" aria-labelledby="product-action-title">
      <div>
        <h2 className="type-eyebrow product-action__eyebrow" id="product-action-title">
          Ordering
        </h2>
        <p className="type-small product-action__copy">
          {isPizza
            ? isSoldOut
              ? "This pizza is currently sold out."
              : item.customizable
                ? "Customise this pizza before adding it to your order."
                : quantity > 0
                  ? "This pizza is in your order. Adjust the basket quantity here."
                  : "Add this pizza as it is. Pizza customisation will be connected next."
            : isSoldOut
              ? "This dish is currently sold out."
              : item.customizable
                ? "Customise this dish before adding it to your order."
                : quantity > 0
                  ? "This dish is in your order. Adjust the basket quantity here."
                  : "Add this dish to your order."}
        </p>
      </div>

      <div className="product-action__controls">
        {quantity > 0 && canQuickAdd ? (
          <div
            className="product-action__quantity"
            role="group"
            aria-label={`Quantity for ${item.name}`}
          >
            <button
              aria-label={`Decrease ${item.name} quantity`}
              className="product-action__quantity-button"
              onClick={decreaseQuantity}
              type="button"
            >
              −
            </button>
            <span className="product-action__quantity-value" aria-live="polite">
              {quantity}
            </span>
            <button
              aria-label={`Increase ${item.name} quantity`}
              className="product-action__quantity-button"
              disabled={quantity >= CART_MAX_QUANTITY}
              onClick={increaseQuantity}
              type="button"
            >
              +
            </button>
          </div>
        ) : null}

        {quantity <= 0 || !canQuickAdd ? (
          <Button
            className="product-action__button"
            disabled={!hydrated || !canQuickAdd}
            onClick={handleAddToOrder}
          >
            {isSoldOut
              ? "SOLD OUT"
              : item.customizable
                ? "CUSTOMISE TO ORDER"
                : `ADD TO ORDER — ${formatGbpPennies(linePricePennies)}`}
          </Button>
        ) : null}

        {quantity > 0 && canQuickAdd ? (
          <p className="type-small product-action__confirmation" aria-live="polite">
            {added ? "Added to order." : "In your order."}{" "}
            <Link className="product-action__basket-link" href={routes.basket}>
              View Basket
            </Link>
          </p>
        ) : null}
      </div>
    </section>
  );
}
