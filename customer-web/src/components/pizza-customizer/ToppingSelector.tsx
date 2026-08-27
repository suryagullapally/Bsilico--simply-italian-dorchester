import { ToppingOption } from "@/components/pizza-customizer/ToppingOption";

type ToppingSelectorProps = {
  onToggleTopping: (name: string) => void;
  selectedToppings: string[];
  toppingPricePennies: number;
  toppings: string[];
};

export function ToppingSelector({
  onToggleTopping,
  selectedToppings,
  toppingPricePennies,
  toppings,
}: ToppingSelectorProps) {
  const selectedToppingSet = new Set(selectedToppings);

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
            key={topping}
            name={topping}
            onToggle={onToggleTopping}
            selected={selectedToppingSet.has(topping)}
            toppingPricePennies={toppingPricePennies}
          />
        ))}
      </div>
    </section>
  );
}
