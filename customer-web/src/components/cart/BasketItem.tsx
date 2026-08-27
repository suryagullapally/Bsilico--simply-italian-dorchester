"use client";

import Image from "next/image";
import { useCart } from "@/components/cart/CartProvider";
import { formatGbpPennies } from "@/lib/format-price";
import type { CartLine } from "@/lib/cart/cart-types";

type BasketItemProps = {
  item: CartLine;
};

export function BasketItem({ item }: BasketItemProps) {
  const { decrementLine, incrementLine, removeLine } = useCart();
  const lineTotalPennies = item.unitPricePennies * item.quantity;
  const hasSelectedToppings =
    item.lineType === "custom-pizza" && item.selectedToppings?.length;

  return (
    <article
      className={[
        "basket-item",
        item.image ? "basket-item--with-image" : "basket-item--text-only",
      ]
        .filter(Boolean)
        .join(" ")}
    >
      {item.image ? (
        <div className="basket-item__media">
          <Image
            alt={item.image.alt}
            className="basket-item__image"
            fill
            sizes="(max-width: 767px) 5.5rem, 8rem"
            src={item.image.src}
          />
        </div>
      ) : null}

      <div className="basket-item__body">
        <div className="basket-item__heading">
          <div>
            <h2 className="type-h3 basket-item__title">{item.name}</h2>
            {item.lineType === "custom-pizza" ? (
              <p className="type-small basket-item__custom">
                {hasSelectedToppings
                  ? item.selectedToppings?.join(" · ")
                  : "No extra toppings selected."}
              </p>
            ) : null}
          </div>
          <p className="type-price basket-item__line-total">
            {formatGbpPennies(lineTotalPennies)}
          </p>
        </div>

        <div className="basket-item__meta">
          <p className="type-small basket-item__unit">
            {formatGbpPennies(item.unitPricePennies)} each
          </p>

          <div
            className="basket-quantity"
            role="group"
            aria-label={`Quantity for ${item.name}`}
          >
            <button
              aria-label={`Decrease ${item.name} quantity`}
              className="basket-quantity__button"
              disabled={item.quantity <= 1}
              onClick={() => decrementLine(item.id)}
              type="button"
            >
              −
            </button>
            <span className="basket-quantity__value" aria-live="polite">
              {item.quantity}
            </span>
            <button
              aria-label={`Increase ${item.name} quantity`}
              className="basket-quantity__button"
              disabled={item.quantity >= 99}
              onClick={() => incrementLine(item.id)}
              type="button"
            >
              +
            </button>
          </div>

          <button
            aria-label={`Remove ${item.name}`}
            className="basket-item__remove"
            onClick={() => removeLine(item.id)}
            type="button"
          >
            Remove
          </button>
        </div>
      </div>
    </article>
  );
}
