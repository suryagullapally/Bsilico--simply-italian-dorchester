import type { DietaryTag, ProductType } from "@/types/admin";

export type MenuItemRequestPayload = {
  categoryId: number;
  slug?: string;
  name: string;
  description: string | null;
  pricePence: number;
  imagePath: string | null;
  productType: ProductType;
  available: boolean;
  active: boolean;
  featured: boolean;
  customizable: boolean;
  displayOrder: number;
  dietaryTags: DietaryTag[];
};

export type AvailabilityUpdateRequest = {
  available: boolean;
};

export type ActiveUpdateRequest = {
  active: boolean;
};

export type FeaturedUpdateRequest = {
  featured: boolean;
};
