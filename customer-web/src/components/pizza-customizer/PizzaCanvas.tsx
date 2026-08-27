import Image from "next/image";
import {
  getSelectedToppingVisualLayers,
  pizzaBaseVisual,
} from "@/data/pizza-visuals";

type PizzaCanvasProps = {
  selectedToppings: string[];
};

export function PizzaCanvas({ selectedToppings }: PizzaCanvasProps) {
  const visibleToppingLayers = getSelectedToppingVisualLayers(selectedToppings);

  return (
    <figure
      className="pizza-canvas"
      aria-label="Visual preview of your custom Basilico pizza"
    >
      <div className="pizza-canvas__stage">
        <Image
          alt={pizzaBaseVisual.alt}
          className="pizza-canvas__image pizza-canvas__image--base"
          fill
          priority
          sizes="(max-width: 767px) 92vw, (max-width: 1279px) 46vw, 38rem"
          src={pizzaBaseVisual.src}
        />

        {visibleToppingLayers.map((layer) => (
          <Image
            alt=""
            aria-hidden="true"
            className="pizza-canvas__image pizza-canvas__image--topping"
            fill
            key={layer.topping}
            sizes="(max-width: 767px) 92vw, (max-width: 1279px) 46vw, 38rem"
            src={layer.src}
            style={{ zIndex: layer.layerOrder }}
          />
        ))}
      </div>
      <figcaption className="type-small pizza-canvas__caption">
        Your choices update the price and summary as you build.
      </figcaption>
    </figure>
  );
}
