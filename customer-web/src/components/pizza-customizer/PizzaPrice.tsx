import { formatGbpPennies } from "@/lib/format-price";
import type { PizzaPricing } from "@/types/pizza-customizer";

type PizzaPriceProps = {
  pricing: PizzaPricing;
};

export function PizzaPrice({ pricing }: PizzaPriceProps) {
  return (
    <div className="pizza-price" aria-live="polite">
      <p className="type-eyebrow pizza-price__label">Current total</p>
      <p className="type-price pizza-price__value">
        {formatGbpPennies(pricing.totalPricePennies)}
      </p>
    </div>
  );
}
