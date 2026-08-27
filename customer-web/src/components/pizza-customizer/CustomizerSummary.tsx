import Link from "next/link";
import { Button } from "@/components/ui/Button";
import { formatGbpPennies } from "@/lib/format-price";
import { routes } from "@/lib/routes";
import type { PizzaPricing } from "@/types/pizza-customizer";

type CustomizerSummaryProps = {
  added: boolean;
  onAddToOrder: () => void;
  onRemoveTopping: (name: string) => void;
  pricing: PizzaPricing;
  selectedToppings: string[];
};

export function CustomizerSummary({
  added,
  onAddToOrder,
  onRemoveTopping,
  pricing,
  selectedToppings,
}: CustomizerSummaryProps) {
  return (
    <aside className="customizer-summary" aria-labelledby="customizer-summary-title">
      <div className="customizer-summary__section">
        <p className="type-eyebrow customizer-summary__eyebrow">
          Your toppings
        </p>
        <h2 className="type-h3 customizer-summary__title" id="customizer-summary-title">
          {pricing.selectedToppingCount} selected
        </h2>

        {selectedToppings.length > 0 ? (
          <ul className="customizer-summary__toppings">
            {selectedToppings.map((topping) => (
              <li className="customizer-summary__topping" key={topping}>
                <span>{topping}</span>
                <button
                  className="customizer-summary__remove"
                  onClick={() => onRemoveTopping(topping)}
                  type="button"
                >
                  Remove
                </button>
              </li>
            ))}
          </ul>
        ) : (
          <p className="type-small customizer-summary__empty">
            No extra toppings selected.
          </p>
        )}
      </div>

      <dl className="customizer-summary__totals">
        <div>
          <dt>Base</dt>
          <dd>{formatGbpPennies(pricing.basePricePennies)}</dd>
        </div>
        <div>
          <dt>Extras</dt>
          <dd>{formatGbpPennies(pricing.extrasPricePennies)}</dd>
        </div>
        <div className="customizer-summary__total">
          <dt>Total</dt>
          <dd>{formatGbpPennies(pricing.totalPricePennies)}</dd>
        </div>
      </dl>

      <div className="customizer-summary__action">
        <Button
          aria-describedby="customizer-action-note"
          className="customizer-summary__button"
          onClick={onAddToOrder}
        >
          ADD TO ORDER — {formatGbpPennies(pricing.totalPricePennies)}
        </Button>
        <p className="type-small customizer-summary__note" id="customizer-action-note">
          {added ? (
            <>
              Added to your order.{" "}
              <Link className="customizer-summary__basket-link" href={routes.basket}>
                View Basket
              </Link>
            </>
          ) : (
            "You can keep editing this pizza after adding it."
          )}
        </p>
      </div>

      <p className="type-small customizer-summary__allergy">
        Please tell us about any allergies or dietary requirements before
        ordering.
      </p>
    </aside>
  );
}
