export type PizzaVisualLayerGroup =
  | "base"
  | "sauce"
  | "cheese"
  | "meat"
  | "vegetable"
  | "finish";

export type PizzaBaseVisual = {
  alt: string;
  group: "base";
  id: string;
  layerOrder: number;
  src: string;
  temporary?: boolean;
};

export type PizzaToppingVisualLayer = {
  alt: string;
  group: Exclude<PizzaVisualLayerGroup, "base">;
  layerOrder: number;
  src: string;
  topping: string;
};

export type PizzaPricing = {
  basePricePennies: number;
  extrasPricePennies: number;
  selectedToppingCount: number;
  toppingPricePennies: number;
  totalPricePennies: number;
};
