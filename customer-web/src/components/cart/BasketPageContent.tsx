"use client";

import Link from "next/link";
import { BasketItem } from "@/components/cart/BasketItem";
import { useCart } from "@/components/cart/CartProvider";
import { ButtonLink } from "@/components/ui/Button";
import { formatGbpPennies } from "@/lib/format-price";
import { routes } from "@/lib/routes";

export function BasketPageContent() {
  const { clearCart, hydrated, itemCount, items, subtotalPennies } = useCart();

  function handleClearBasket() {
    if (window.confirm("Clear your basket?")) {
      clearCart();
    }
  }

  if (!hydrated) {
    return (
      <section className="basket-loading" aria-labelledby="basket-page-title">
        <p className="type-eyebrow basket-page__eyebrow">Basket</p>
        <h1 className="type-h1 basket-page__title" id="basket-page-title">
          Your order.
        </h1>
        <p className="type-body basket-page__copy">Checking your basket.</p>
      </section>
    );
  }

  if (items.length === 0) {
    return (
      <section className="basket-empty" aria-labelledby="basket-page-title">
        <p className="type-eyebrow basket-page__eyebrow">Basket</p>
        <h1 className="type-h1 basket-page__title" id="basket-page-title">
          Your basket is empty.
        </h1>
        <p className="type-body basket-page__copy">
          Browse our menu and find something you fancy.
        </p>
        <ButtonLink href={routes.menu} variant="primary">
          VIEW MENU
        </ButtonLink>
      </section>
    );
  }

  return (
    <>
      <header className="basket-page__header">
        <p className="type-eyebrow basket-page__eyebrow">Basket</p>
        <h1 className="type-h1 basket-page__title" id="basket-page-title">
          Your order.
        </h1>
        <p className="type-body basket-page__copy">
          Review your Basilico order before heading to checkout.
        </p>
      </header>

      <div className="basket-page__layout">
        <section
          className="basket-items"
          aria-labelledby="basket-items-title"
        >
          <div className="basket-items__header">
            <h2 className="type-h3 basket-items__title" id="basket-items-title">
              Basket items
            </h2>
            <p className="type-small basket-items__count" aria-live="polite">
              {itemCount} item{itemCount === 1 ? "" : "s"}
            </p>
          </div>

          <div className="basket-items__list">
            {items.map((item) => (
              <BasketItem item={item} key={item.id} />
            ))}
          </div>
        </section>

        <aside className="basket-summary" aria-labelledby="basket-summary-title">
          <div>
            <p className="type-eyebrow basket-summary__eyebrow">Order summary</p>
            <h2 className="type-h3 basket-summary__title" id="basket-summary-title">
              Ready when you are.
            </h2>
          </div>

          <dl className="basket-summary__totals">
            <div>
              <dt>Items</dt>
              <dd>{itemCount}</dd>
            </div>
            <div className="basket-summary__subtotal">
              <dt>Subtotal</dt>
              <dd>{formatGbpPennies(subtotalPennies)}</dd>
            </div>
          </dl>

          <div className="basket-summary__actions">
            <ButtonLink
              className="basket-summary__checkout"
              href={routes.checkout}
              variant="primary"
            >
              CONTINUE TO CHECKOUT
            </ButtonLink>
            <Link className="basket-summary__menu-link" href={routes.menu}>
              Continue browsing menu
            </Link>
            <button
              className="basket-summary__clear"
              onClick={handleClearBasket}
              type="button"
            >
              Clear Basket
            </button>
          </div>
        </aside>
      </div>
    </>
  );
}
