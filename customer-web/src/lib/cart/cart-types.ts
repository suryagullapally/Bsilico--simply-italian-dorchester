import type {
  CreateYourOwnToppingOption,
  DietaryTag,
  MenuItem,
  MenuItemImage,
  MenuProductType,
} from "@/types/menu";

export const CART_STORAGE_KEY = "basilico:cart:v2";
export const CART_STORAGE_VERSION = 2;
export const CART_MAX_QUANTITY = 99;

export type CartLineType = "menu-item" | "custom-pizza";

export type CartLine = {
  basePricePennies?: number;
  dietaryTags: DietaryTag[];
  extrasPricePennies?: number;
  id: string;
  image?: MenuItemImage;
  lineType: CartLineType;
  menuItemId?: number;
  name: string;
  productId: string;
  productSlug: string;
  productType: MenuProductType;
  quantity: number;
  customizerId?: number;
  selectedToppingIds?: number[];
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
  | { line?: CartLine; lineId: string; quantity: number; type: "set-line-quantity" }
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
  customizerId: number;
  extrasPricePennies: number;
  image?: MenuItemImage;
  quantity?: number;
  selectedToppings: CreateYourOwnToppingOption[];
  totalPricePennies: number;
};
