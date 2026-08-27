"use client";

import Link from "next/link";
import { useState } from "react";
import { useCart } from "@/components/cart/CartProvider";
import { Button } from "@/components/ui/Button";
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
  const [quantity, setQuantity] = useState(1);
  const { addMenuItem } = useCart();
  const unitPricePennies = item.pricePence;
  const linePricePennies = unitPricePennies * quantity;

  function decreaseQuantity() {
    setAdded(false);
    setQuantity((currentQuantity) => Math.max(1, currentQuantity - 1));
  }

  function increaseQuantity() {
    setAdded(false);
    setQuantity((currentQuantity) => Math.min(99, currentQuantity + 1));
  }

  function handleAddToOrder() {
    if (isSoldOut) {
      return;
    }

    addMenuItem(item, quantity);
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
              : "Add this pizza as it is. Pizza customisation will be connected next."
            : isSoldOut
              ? "This dish is currently sold out."
              : "Choose a quantity and add this dish to your order."}
        </p>
      </div>

      <div className="product-action__controls">
        <div
          className="product-action__quantity"
          role="group"
          aria-label={`Quantity for ${item.name}`}
        >
          <button
            aria-label={`Decrease ${item.name} quantity`}
            className="product-action__quantity-button"
            disabled={isSoldOut || quantity <= 1}
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
            disabled={isSoldOut || quantity >= 99}
            onClick={increaseQuantity}
            type="button"
          >
            +
          </button>
        </div>

        <Button
          className="product-action__button"
          disabled={isSoldOut}
          onClick={handleAddToOrder}
        >
          {isSoldOut
            ? "SOLD OUT"
            : `ADD TO ORDER — ${formatGbpPennies(linePricePennies)}`}
        </Button>

        {added ? (
          <p className="type-small product-action__confirmation" aria-live="polite">
            Added to order.{" "}
            <Link className="product-action__basket-link" href={routes.basket}>
              View Basket
            </Link>
          </p>
        ) : null}
      </div>
    </section>
  );
}
