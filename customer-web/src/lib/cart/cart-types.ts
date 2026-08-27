import type { DietaryTag, MenuItem, MenuItemImage, MenuProductType } from "@/types/menu";

export const CART_STORAGE_KEY = "basilico:cart:v1";
export const CART_STORAGE_VERSION = 1;
export const CART_MAX_QUANTITY = 99;

export type CartLineType = "menu-item" | "custom-pizza";

export type CartLine = {
  basePricePennies?: number;
  dietaryTags: DietaryTag[];
  extrasPricePennies?: number;
  id: string;
  image?: MenuItemImage;
  lineType: CartLineType;
  name: string;
  productId: string;
  productSlug: string;
  productType: MenuProductType;
  quantity: number;
  selectedToppings?: string[];
  unitPricePennies: number;
};

export type CartState = {
  hydrated: boolean;
  items: CartLine[];
};

export type CartAction =
  | { items: CartLine[]; type: "hydrate" }
  | { line: CartLine; type: "add-line" }
  | { lineId: string; type: "increment-line" }
  | { lineId: string; type: "decrement-line" }
  | { lineId: string; type: "remove-line" }
  | { type: "clear" };

export type PersistedCart = {
  items: CartLine[];
  version: typeof CART_STORAGE_VERSION;
};

export type AddMenuItemInput = {
  item: MenuItem;
  quantity?: number;
};

export type AddCustomPizzaInput = {
  basePricePennies: number;
  extrasPricePennies: number;
  image?: MenuItemImage;
  quantity?: number;
  selectedToppings: string[];
  totalPricePennies: number;
};
