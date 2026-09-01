import {
  CART_MAX_QUANTITY,
  CART_STORAGE_VERSION,
  type AddCustomPizzaInput,
  type AddMenuItemInput,
  type CartLine,
  type PersistedCart,
} from "@/lib/cart/cart-types";
import type {
  CreateYourOwnToppingOption,
  DietaryTag,
  MenuItem,
  MenuProductType,
} from "@/types/menu";

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
  const menuItemId = item.backendId ?? parsePositiveInteger(item.id);

  return {
    dietaryTags: item.dietaryTags,
    id: `menu:${item.id}`,
    image: item.image,
    lineType: "menu-item",
    menuItemId,
    name: item.name,
    productId: item.id,
    productSlug: item.slug,
    productType: item.productType,
    quantity: clampCartQuantity(quantity),
    unitPricePennies: item.pricePence,
  } satisfies CartLine;
}

export function getMenuCartLineId(item: Pick<MenuItem, "id">) {
  return `menu:${item.id}`;
}

export function getMenuItemCartQuantity(
  items: readonly CartLine[],
  item: Pick<MenuItem, "backendId" | "id">,
) {
  const menuItemLineId = getMenuCartLineId(item);

  return items
    .filter(
      (line) =>
        line.lineType === "menu-item" &&
        (line.id === menuItemLineId ||
          (item.backendId !== undefined && line.menuItemId === item.backendId)),
    )
    .reduce((quantity, line) => quantity + line.quantity, 0);
}

export function createCustomPizzaCartLine({
  basePricePennies,
  customizerId,
  extrasPricePennies,
  image,
  quantity = 1,
  selectedToppings,
  totalPricePennies,
}: AddCustomPizzaInput) {
  const normalizedToppings = normalizeToppingOptions(selectedToppings);
  const selectedToppingIds = normalizedToppings.map((topping) => topping.id);
  const selectedToppingNames = normalizedToppings.map((topping) => topping.name);
  const identity = selectedToppingIds.join("+") || "no-toppings";

  return {
    basePricePennies,
    customizerId,
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
    selectedToppingIds,
    selectedToppings: selectedToppingNames,
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

export function normalizeToppingOptions(
  toppings: readonly CreateYourOwnToppingOption[],
) {
  const toppingsById = new Map<number, CreateYourOwnToppingOption>();

  for (const topping of toppings) {
    if (!Number.isInteger(topping.id) || topping.id <= 0) {
      continue;
    }

    const name = topping.name.trim();
    if (!name) {
      continue;
    }

    toppingsById.set(topping.id, {
      ...topping,
      name,
    });
  }

  return [...toppingsById.values()].sort(
    (firstTopping, secondTopping) => firstTopping.id - secondTopping.id,
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
  const menuItemId = isPositiveInteger(value.menuItemId)
    ? value.menuItemId
    : undefined;
  const customizerId = isPositiveInteger(value.customizerId)
    ? value.customizerId
    : undefined;

  if (lineType === "menu-item" && !menuItemId) {
    return null;
  }

  if (lineType === "custom-pizza" && !customizerId) {
    return null;
  }

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
    menuItemId,
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
    const selectedToppingIds = Array.isArray(value.selectedToppingIds)
      ? value.selectedToppingIds.filter(isPositiveInteger)
      : [];

    line.customizerId = customizerId;
    line.selectedToppingIds = [...new Set(selectedToppingIds)].sort(
      (firstId, secondId) => firstId - secondId,
    );
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

function parsePositiveInteger(value: string) {
  const parsedValue = Number.parseInt(value, 10);

  if (!Number.isInteger(parsedValue) || parsedValue <= 0) {
    throw new Error("Menu item is missing a backend id.");
  }

  return parsedValue;
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

function isPositiveInteger(value: unknown): value is number {
  return Number.isInteger(value) && typeof value === "number" && value > 0;
}
