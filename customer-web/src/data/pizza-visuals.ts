import type {
  PizzaBaseVisual,
  PizzaToppingVisualLayer,
  PizzaVisualLayerGroup,
} from "@/types/pizza-customizer";

export const pizzaVisualLayerOrder: Record<PizzaVisualLayerGroup, number> = {
  base: 0,
  sauce: 100,
  cheese: 200,
  meat: 300,
  vegetable: 400,
  finish: 500,
};

export const pizzaBaseVisual: PizzaBaseVisual = {
  alt: "Basilico pizza visual preview",
  group: "base",
  id: "temporary-margherita-foundation",
  layerOrder: pizzaVisualLayerOrder.base,
  src: "/images/menu/margherita.png",
  temporary: true,
};

export const toppingVisualLayers: Partial<Record<string, PizzaToppingVisualLayer>> =
  {};

export function getSelectedToppingVisualLayers(
  selectedToppings: readonly string[],
) {
  return selectedToppings
    .map((topping) => toppingVisualLayers[topping])
    .filter((layer): layer is PizzaToppingVisualLayer => Boolean(layer))
    .sort((firstLayer, secondLayer) => {
      if (firstLayer.layerOrder === secondLayer.layerOrder) {
        return firstLayer.topping.localeCompare(secondLayer.topping);
      }

      return firstLayer.layerOrder - secondLayer.layerOrder;
    });
}
