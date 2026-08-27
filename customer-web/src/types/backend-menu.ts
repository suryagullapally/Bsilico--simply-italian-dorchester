import type { DietaryTag } from "@/types/menu";

export type BackendProductType = "STANDARD" | "PIZZA";

export type BackendMenuResponse = {
  categories: BackendMenuCategory[];
  customizers: BackendMenuCustomizer[];
};

export type BackendMenuCategory = {
  displayOrder: number;
  id: number;
  items: BackendMenuItem[];
  name: string;
  slug: string;
};

export type BackendMenuItem = {
  active: boolean;
  available: boolean;
  categoryName: string;
  categorySlug: string;
  customizable: boolean;
  description: string | null;
  dietaryTags: DietaryTag[];
  displayOrder: number;
  featured: boolean;
  id: number;
  imagePath: string | null;
  name: string;
  pricePence: number;
  productType: BackendProductType;
  slug: string;
};

export type BackendMenuCustomizer = {
  active: boolean;
  basePricePence: number;
  categoryName: string;
  categorySlug: string;
  displayOrder: number;
  extraToppingPricePence: number;
  id: number;
  name: string;
  slug: string;
  toppings: BackendPizzaTopping[];
};

export type BackendPizzaTopping = {
  available: boolean;
  displayOrder: number;
  id: number;
  name: string;
  priceOverridePence: number | null;
};
