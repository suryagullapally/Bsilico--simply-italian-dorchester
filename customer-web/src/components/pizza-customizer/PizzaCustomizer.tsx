"use client";

import { useMemo, useState } from "react";
import { useCart } from "@/components/cart/CartProvider";
import { CustomizerSummary } from "@/components/pizza-customizer/CustomizerSummary";
import { PizzaCanvas } from "@/components/pizza-customizer/PizzaCanvas";
import { PizzaPrice } from "@/components/pizza-customizer/PizzaPrice";
import { ToppingSelector } from "@/components/pizza-customizer/ToppingSelector";
import { pizzaBaseVisual } from "@/data/pizza-visuals";
import { formatGbpPennies } from "@/lib/format-price";
import { getCreateYourOwnPricing } from "@/lib/pizza-pricing";
import type { CreateYourOwnConfiguration } from "@/types/menu";

type PizzaCustomizerProps = {
  configuration: CreateYourOwnConfiguration;
};

export function PizzaCustomizer({ configuration }: PizzaCustomizerProps) {
  const { addCustomPizza } = useCart();
  const [added, setAdded] = useState(false);
  const [selectedToppings, setSelectedToppings] = useState<string[]>([]);

  const pricing = useMemo(
    () => getCreateYourOwnPricing(configuration, selectedToppings.length),
    [configuration, selectedToppings.length],
  );

  function toggleTopping(topping: string) {
    setAdded(false);
    setSelectedToppings((currentToppings) =>
      currentToppings.includes(topping)
        ? currentToppings.filter((currentTopping) => currentTopping !== topping)
        : [...currentToppings, topping],
    );
  }

  function handleAddToOrder() {
    addCustomPizza({
      basePricePennies: pricing.basePricePennies,
      extrasPricePennies: pricing.extrasPricePennies,
      image: {
        alt: pizzaBaseVisual.alt,
        src: pizzaBaseVisual.src,
      },
      selectedToppings,
      totalPricePennies: pricing.totalPricePennies,
    });
    setAdded(true);
  }

  return (
    <section
      className="pizza-customizer"
      aria-labelledby="pizza-customizer-title"
    >
      <div className="pizza-customizer__layout">
        <header className="pizza-customizer__intro">
          <p className="type-eyebrow pizza-customizer__eyebrow">Your pizza</p>
          <h1 className="type-h1 pizza-customizer__title" id="pizza-customizer-title">
            Create your own.
          </h1>
          <p className="type-body pizza-customizer__copy">
            Start with our sourdough pizza base and make it yours.
          </p>
          <div className="pizza-customizer__price-row">
            <p className="type-small pizza-customizer__base-price">
              From {formatGbpPennies(configuration.basePricePence)}
            </p>
            <p className="type-small pizza-customizer__extra-price">
              Extra toppings {formatGbpPennies(pricing.toppingPricePennies)} each
            </p>
          </div>
          <PizzaPrice pricing={pricing} />
        </header>

        <div className="pizza-customizer__visual">
          <PizzaCanvas selectedToppings={selectedToppings} />
        </div>

        <div className="pizza-customizer__selector">
          <ToppingSelector
            onToggleTopping={toggleTopping}
            selectedToppings={selectedToppings}
            toppingPricePennies={pricing.toppingPricePennies}
            toppings={configuration.toppings}
          />
        </div>

        <div className="pizza-customizer__summary">
          <CustomizerSummary
            added={added}
            onAddToOrder={handleAddToOrder}
            onRemoveTopping={toggleTopping}
            pricing={pricing}
            selectedToppings={selectedToppings}
          />
        </div>
      </div>
    </section>
  );
}
