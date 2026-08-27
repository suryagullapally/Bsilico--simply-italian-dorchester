import {
  CART_MAX_QUANTITY,
  CART_STORAGE_VERSION,
  type AddCustomPizzaInput,
  type AddMenuItemInput,
  type CartLine,
  type PersistedCart,
} from "@/lib/cart/cart-types";
import type { DietaryTag, MenuProductType } from "@/types/menu";

const allowedDietaryTags = new Set<DietaryTag>(["GF", "V", "VE"]);
const allowedProductTypes = new Set<MenuProductType>([
  "starter",
  "pizza",
  "calzone",
  "special",
  "dessert",
  "salad",
]);

export function clampCartQuantity(quantity: number) {
  if (!Number.isFinite(quantity)) {
    return 1;
  }

  return Math.min(CART_MAX_QUANTITY, Math.max(1, Math.trunc(quantity)));
}

export function getCartItemCount(items: readonly CartLine[]) {
  return items.reduce((total, item) => total + item.quantity, 0);
}

export function getCartSubtotalPennies(items: readonly CartLine[]) {
  return items.reduce(
    (total, item) => total + item.unitPricePennies * item.quantity,
    0,
  );
}

export function createMenuCartLine({ item, quantity = 1 }: AddMenuItemInput) {
  return {
    dietaryTags: item.dietaryTags,
    id: `menu:${item.id}`,
    image: item.image,
    lineType: "menu-item",
    name: item.name,
    productId: item.id,
    productSlug: item.slug,
    productType: item.productType,
    quantity: clampCartQuantity(quantity),
    unitPricePennies: item.pricePence,
  } satisfies CartLine;
}

export function createCustomPizzaCartLine({
  basePricePennies,
  extrasPricePennies,
  image,
  quantity = 1,
  selectedToppings,
  totalPricePennies,
}: AddCustomPizzaInput) {
  const normalizedToppings = normalizeToppings(selectedToppings);
  const identity = normalizedToppings.map(slugifyCartPart).join("+") || "no-toppings";

  return {
    basePricePennies,
    dietaryTags: [],
    extrasPricePennies,
    id: `custom:create-your-own:${identity}`,
    image,
    lineType: "custom-pizza",
    name: "CREATE YOUR OWN",
    productId: "create-your-own",
    productSlug: "create-your-own",
    productType: "pizza",
    quantity: clampCartQuantity(quantity),
    selectedToppings: normalizedToppings,
    unitPricePennies: totalPricePennies,
  } satisfies CartLine;
}

export function normalizeToppings(toppings: readonly string[]) {
  return [...new Set(toppings)]
    .map((topping) => topping.trim())
    .filter(Boolean)
    .sort((firstTopping, secondTopping) =>
      firstTopping.localeCompare(secondTopping, "en-GB"),
    );
}

export function serializeCart(items: readonly CartLine[]): PersistedCart {
  return {
    items: items.map((item) => ({ ...item })),
    version: CART_STORAGE_VERSION,
  };
}

export function parsePersistedCart(value: string | null) {
  if (!value) {
    return [];
  }

  try {
    const parsedValue: unknown = JSON.parse(value);

    if (!isRecord(parsedValue) || parsedValue.version !== CART_STORAGE_VERSION) {
      return [];
    }

    if (!Array.isArray(parsedValue.items)) {
      return [];
    }

    return parsedValue.items
      .map(parseCartLine)
      .filter((item): item is CartLine => Boolean(item));
  } catch {
    return [];
  }
}

function parseCartLine(value: unknown): CartLine | null {
  if (!isRecord(value)) {
    return null;
  }

  const unitPricePennies = value.unitPricePennies;
  const productType = value.productType;

  if (
    !isNonEmptyString(value.id) ||
    !isNonEmptyString(value.name) ||
    !isNonEmptyString(value.productId) ||
    !isNonEmptyString(value.productSlug) ||
    !isNonNegativeInteger(unitPricePennies) ||
    !allowedProductTypes.has(productType as MenuProductType)
  ) {
    return null;
  }

  const lineType = value.lineType === "custom-pizza" ? "custom-pizza" : "menu-item";
  const dietaryTags = Array.isArray(value.dietaryTags)
    ? value.dietaryTags.filter((tag): tag is DietaryTag =>
        allowedDietaryTags.has(tag as DietaryTag),
      )
    : [];

  const line: CartLine = {
    dietaryTags,
    id: value.id,
    image: parseImage(value.image),
    lineType,
    name: value.name,
    productId: value.productId,
    productSlug: value.productSlug,
    productType: productType as MenuProductType,
    quantity: clampCartQuantity(Number(value.quantity)),
    unitPricePennies,
  };

  if (lineType === "custom-pizza") {
    const selectedToppings = Array.isArray(value.selectedToppings)
      ? value.selectedToppings.filter(isNonEmptyString)
      : [];

    line.selectedToppings = normalizeToppings(selectedToppings);

    if (isNonNegativeInteger(value.basePricePennies)) {
      line.basePricePennies = value.basePricePennies;
    }

    if (isNonNegativeInteger(value.extrasPricePennies)) {
      line.extrasPricePennies = value.extrasPricePennies;
    }
  }

  return line;
}

function parseImage(value: unknown) {
  if (
    isRecord(value) &&
    isNonEmptyString(value.alt) &&
    isNonEmptyString(value.src)
  ) {
    return {
      alt: value.alt,
      src: value.src,
    };
  }

  return undefined;
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null;
}

function isNonEmptyString(value: unknown): value is string {
  return typeof value === "string" && value.trim().length > 0;
}

function isNonNegativeInteger(value: unknown): value is number {
  return Number.isInteger(value) && typeof value === "number" && value >= 0;
}

function slugifyCartPart(value: string) {
  return value
    .normalize("NFKD")
    .replace(/[\u0300-\u036f]/g, "")
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, "-")
    .replace(/^-|-$/g, "");
}
