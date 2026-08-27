import { getApiUrl } from "@/lib/api/config";
import { formatGbpPennies } from "@/lib/format-price";
import type {
  BackendMenuCustomizer,
  BackendMenuItem,
  BackendMenuResponse,
  BackendProductType,
} from "@/types/backend-menu";
import type {
  CreateYourOwnConfiguration,
  DietaryTag,
  MenuCategory,
  MenuCategoryId,
  MenuData,
  MenuItem,
  MenuProductType,
} from "@/types/menu";

const CATEGORY_NAV_LABELS = {
  "bites-to-start": "Bites to Start",
  "side-salad": "Side Salad",
  "sourdough-pizza-calzone": "Pizza & Calzone",
  specials: "Specials",
  "sweet-tooth": "Sweet Tooth",
} satisfies Record<MenuCategoryId, string>;

const MENU_CATEGORY_IDS = Object.keys(CATEGORY_NAV_LABELS) as MenuCategoryId[];
const CREATE_YOUR_OWN_SLUG = "create-your-own";

type MenuApiErrorCode = "HTTP_ERROR" | "INVALID_RESPONSE" | "NOT_FOUND";

export class MenuApiError extends Error {
  readonly code: MenuApiErrorCode;
  readonly status?: number;

  constructor(message: string, code: MenuApiErrorCode, status?: number) {
    super(message);
    this.name = "MenuApiError";
    this.code = code;
    this.status = status;
  }
}

export function isMenuApiNotFoundError(error: unknown) {
  return error instanceof MenuApiError && error.code === "NOT_FOUND";
}

export async function getMenu() {
  const response = await fetchMenuApi<BackendMenuResponse>("/api/menu");

  return mapMenuResponse(response);
}

export async function getMenuItemBySlug(slug: string) {
  const item = await fetchMenuApi<BackendMenuItem>(
    `/api/menu/items/${encodeURIComponent(slug)}`,
  );

  return mapMenuItem(item);
}

export async function getCreateYourOwnCustomizer() {
  const customizer = await fetchMenuApi<BackendMenuCustomizer>(
    `/api/menu/customizers/${CREATE_YOUR_OWN_SLUG}`,
  );

  return mapCustomizer(customizer);
}

export function findMenuCategory(menu: MenuData, categoryId: MenuCategoryId) {
  return menu.categories.find((category) => category.id === categoryId);
}

export function findRelatedMenuItems(
  menu: MenuData,
  item: MenuItem,
  limit = 3,
) {
  return menu.itemsByCategory[item.category]
    .filter((candidate) => candidate.slug !== item.slug)
    .slice(0, limit);
}

async function fetchMenuApi<T>(path: string) {
  let response: Response;

  try {
    response = await fetch(getApiUrl(path), {
      cache: "no-store",
      headers: {
        Accept: "application/json",
      },
    });
  } catch {
    throw new MenuApiError("Menu API request failed", "HTTP_ERROR");
  }

  if (response.status === 404) {
    throw new MenuApiError("Menu resource not found", "NOT_FOUND", 404);
  }

  if (!response.ok) {
    throw new MenuApiError(
      "Menu API returned an unsuccessful response",
      "HTTP_ERROR",
      response.status,
    );
  }

  return (await response.json()) as T;
}

function mapMenuResponse(response: BackendMenuResponse): MenuData {
  const backendCategories = [...response.categories]
    .sort(
      (firstCategory, secondCategory) =>
        firstCategory.displayOrder - secondCategory.displayOrder,
    );

  const itemsByCategory = createEmptyItemsByCategory();
  const categories: MenuCategory[] = backendCategories.map((category) => {
    const categoryId = toMenuCategoryId(category.slug);

    itemsByCategory[categoryId] = category.items
      .map(mapMenuItem)
      .sort(
        (firstItem, secondItem) =>
          (firstItem.displayOrder ?? 0) - (secondItem.displayOrder ?? 0),
      );

    return {
      displayOrder: category.displayOrder,
      id: categoryId,
      navLabel: CATEGORY_NAV_LABELS[categoryId],
      title: category.name,
    };
  });

  const items = categories.flatMap((category) => itemsByCategory[category.id]);
  const customizers = [...response.customizers]
    .sort(
      (firstCustomizer, secondCustomizer) =>
        firstCustomizer.displayOrder - secondCustomizer.displayOrder,
    )
    .map(mapCustomizer);

  return {
    categories,
    createYourOwn: customizers.find(
      (customizer) => customizer.slug === CREATE_YOUR_OWN_SLUG,
    ),
    customizers,
    items,
    itemsByCategory,
  };
}

function mapMenuItem(item: BackendMenuItem): MenuItem {
  const category = toMenuCategoryId(item.categorySlug);

  return {
    available: item.available,
    category,
    customizable: item.customizable,
    description: item.description ?? undefined,
    displayOrder: item.displayOrder,
    dietaryTags: item.dietaryTags.filter(isDietaryTag),
    featured: item.featured,
    id: String(item.id),
    image: item.imagePath
      ? {
          alt: `${item.name} at Basilico`,
          src: item.imagePath,
        }
      : undefined,
    name: item.name,
    pricePence: item.pricePence,
    productType: mapProductType(item.productType, category),
    slug: item.slug,
  };
}

function mapCustomizer(
  customizer: BackendMenuCustomizer,
): CreateYourOwnConfiguration {
  const toppings = customizer.toppings
    .filter((topping) => topping.available)
    .sort(
      (firstTopping, secondTopping) =>
        firstTopping.displayOrder - secondTopping.displayOrder,
    )
    .map((topping) => topping.name);

  return {
    basePricePence: customizer.basePricePence,
    extraToppingCopy: `EXTRA TOPPINGS AVAILABLE — ${formatGbpPennies(
      customizer.extraToppingPricePence,
    )} EACH`,
    extraToppingPricePence: customizer.extraToppingPricePence,
    pricingCopy: `FROM ${formatGbpPennies(
      customizer.basePricePence,
    )} + NO TOPPINGS`,
    slug: customizer.slug,
    title: customizer.name,
    toppings,
  };
}

function mapProductType(
  productType: BackendProductType,
  category: MenuCategoryId,
): MenuProductType {
  if (productType === "PIZZA") {
    return "pizza";
  }

  switch (category) {
    case "bites-to-start":
      return "starter";
    case "side-salad":
      return "salad";
    case "specials":
      return "special";
    case "sweet-tooth":
      return "dessert";
    case "sourdough-pizza-calzone":
      return "calzone";
  }
}

function toMenuCategoryId(slug: string): MenuCategoryId {
  if (isMenuCategoryId(slug)) {
    return slug;
  }

  throw new MenuApiError(
    `Unexpected menu category slug "${slug}"`,
    "INVALID_RESPONSE",
  );
}

function isMenuCategoryId(value: string): value is MenuCategoryId {
  return MENU_CATEGORY_IDS.includes(value as MenuCategoryId);
}

function isDietaryTag(value: string): value is DietaryTag {
  return value === "V" || value === "GF" || value === "VE";
}

function createEmptyItemsByCategory() {
  return MENU_CATEGORY_IDS.reduce(
    (itemsByCategory, categoryId) => ({
      ...itemsByCategory,
      [categoryId]: [],
    }),
    {} as Record<MenuCategoryId, MenuItem[]>,
  );
}
