import { ToppingOption } from "@/components/pizza-customizer/ToppingOption";
import type { CreateYourOwnToppingOption } from "@/types/menu";

type ToppingSelectorProps = {
  onToggleTopping: (topping: CreateYourOwnToppingOption) => void;
  selectedToppingIds: number[];
  toppingPricePennies: number;
  toppings: CreateYourOwnToppingOption[];
};

export function ToppingSelector({
  onToggleTopping,
  selectedToppingIds,
  toppingPricePennies,
  toppings,
}: ToppingSelectorProps) {
  const selectedToppingSet = new Set(selectedToppingIds);

  return (
    <section
      className="topping-selector"
      aria-labelledby="topping-selector-title"
    >
      <div className="topping-selector__header">
        <p className="type-eyebrow topping-selector__eyebrow">Extra toppings</p>
        <h2 className="type-h3 topping-selector__title" id="topping-selector-title">
          Choose what goes on top.
        </h2>
      </div>

      <div className="topping-selector__grid">
        {toppings.map((topping) => (
          <ToppingOption
            key={topping.id}
            onToggle={onToggleTopping}
            selected={selectedToppingSet.has(topping.id)}
            topping={topping}
            toppingPricePennies={toppingPricePennies}
          />
        ))}
      </div>
    </section>
  );
}
