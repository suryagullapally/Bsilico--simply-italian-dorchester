export type DietaryTag = "V" | "GF" | "VE";

export type MenuCategoryId =
  | "bites-to-start"
  | "sourdough-pizza-calzone"
  | "specials"
  | "sweet-tooth"
  | "side-salad";

export type MenuProductType =
  | "starter"
  | "pizza"
  | "calzone"
  | "special"
  | "dessert"
  | "salad";

export type MenuCategory = {
  displayOrder: number;
  id: MenuCategoryId;
  navLabel: string;
  title: string;
};

export type MenuItemImage = {
  alt: string;
  src: string;
};

export type MenuItem = {
  available: boolean;
  category: MenuCategoryId;
  customizable?: boolean;
  description?: string;
  displayOrder?: number;
  dietaryTags: DietaryTag[];
  featured?: boolean;
  id: string;
  image?: MenuItemImage;
  name: string;
  pricePence: number;
  productType: MenuProductType;
  slug: string;
};

export type DietaryLegendItem = {
  description: string;
  tag: DietaryTag;
};

export type CreateYourOwnConfiguration = {
  basePricePence: number;
  extraToppingCopy: string;
  extraToppingPricePence: number;
  pricingCopy: string;
  slug?: string;
  title: string;
  toppings: string[];
};

export type MenuData = {
  categories: MenuCategory[];
  createYourOwn?: CreateYourOwnConfiguration;
  customizers: CreateYourOwnConfiguration[];
  items: MenuItem[];
  itemsByCategory: Record<MenuCategoryId, MenuItem[]>;
};
