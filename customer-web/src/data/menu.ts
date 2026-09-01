import type {
  CreateYourOwnConfiguration,
  CreateYourOwnToppingOption,
  DietaryLegendItem,
  MenuCategory,
  MenuCategoryId,
  MenuItem,
} from "@/types/menu";
import { priceToPennies } from "@/lib/format-price";

// Temporary reference/fallback copy while the live menu is loaded from Spring Boot.
// Runtime menu pages use src/lib/api/menu-api.ts as their source of truth.
type ReferenceMenuItem = Omit<MenuItem, "pricePence"> & {
  price: number;
};

type ReferenceCreateYourOwnConfiguration = Omit<
  CreateYourOwnConfiguration,
  "basePricePence" | "extraToppingPricePence" | "toppings"
> & {
  basePrice: number;
  extraToppingPrice: number;
  toppings: string[];
};

export const menuCategories: MenuCategory[] = [
  {
    displayOrder: 1,
    id: "bites-to-start",
    navLabel: "Bites to Start",
    title: "BITES TO START",
  },
  {
    displayOrder: 2,
    id: "sourdough-pizza-calzone",
    navLabel: "Pizza & Calzone",
    title: "SOURDOUGH PIZZA & CALZONE",
  },
  {
    displayOrder: 3,
    id: "specials",
    navLabel: "Specials",
    title: "SPECIALS",
  },
  {
    displayOrder: 4,
    id: "sweet-tooth",
    navLabel: "Sweet Tooth",
    title: "SWEET TOOTH",
  },
  {
    displayOrder: 5,
    id: "side-salad",
    navLabel: "Side Salad",
    title: "SIDE SALAD",
  },
];

export const dietaryLegend: DietaryLegendItem[] = [
  { description: "Vegetarian", tag: "V" },
  { description: "Gluten Free", tag: "GF" },
  { description: "Vegan", tag: "VE" },
];

export const dietaryLabels = {
  GF: "Gluten Free",
  V: "Vegetarian",
  VE: "Vegan",
} as const;

const referenceMenuItems: ReferenceMenuItem[] = [
  {
    available: true,
    category: "bites-to-start",
    description: "Mixed olives from Nocellara of Belice.",
    dietaryTags: ["V", "GF", "VE"],
    id: "olive-di-nocellara",
    name: "OLIVE DI NOCELLARA",
    price: 3.45,
    productType: "starter",
    slug: "olive-di-nocellara",
  },
  {
    available: true,
    category: "bites-to-start",
    description: "Pizza bread with garlic oil & rosemary sprinkled with sea salt.",
    dietaryTags: ["V", "VE"],
    featured: true,
    id: "schiacciatella",
    image: {
      alt: "Schiacciatella garlic bread at Basilico",
      src: "/images/menu/schiacciatella.png",
    },
    name: "SCHIACCIATELLA",
    price: 4.45,
    productType: "starter",
    slug: "schiacciatella",
  },
  {
    available: true,
    category: "bites-to-start",
    description:
      "Isle of wight tomatoes, red onions and roasted garlic marinated in a special and delicious dressing, served on toasted sourdough, with a light and fresh base of parmesan.",
    dietaryTags: ["V", "VE"],
    id: "bruschetta",
    name: "BRUSCHETTA",
    price: 5.45,
    productType: "starter",
    slug: "bruschetta",
  },
  {
    available: true,
    category: "bites-to-start",
    description:
      "Pizza bread with cheese, garlic oil & rosemary sprinkled with sea salt.",
    dietaryTags: ["V", "VE"],
    id: "schiacciata-al-formaggio",
    name: "SCHIACCIATA AL FORMAGGIO",
    price: 5.65,
    productType: "starter",
    slug: "schiacciata-al-formaggio",
  },
  {
    available: true,
    category: "bites-to-start",
    description:
      "Made from sourdough, toscano, bread stick and mini pizzette served with olive oil and basil.",
    dietaryTags: ["V", "VE"],
    id: "oppane-the-bread",
    name: "OPPĀNE (THE BREAD)",
    price: 5.95,
    productType: "starter",
    slug: "oppane-the-bread",
  },
  {
    available: true,
    category: "bites-to-start",
    description:
      "Slow-roasted Isle of Wight tomato hummus, roasted seeds and olives, served with homemade flat bread.",
    dietaryTags: ["V", "VE"],
    id: "piadina-e-crema-di-pomodoro",
    name: "PIADINA E CREMA DI POMODORO",
    price: 6.45,
    productType: "starter",
    slug: "piadina-e-crema-di-pomodoro",
  },
  {
    available: true,
    category: "bites-to-start",
    description:
      "Buffalo mozzarella from campana isle of wight tomato, beef tomato with Trapani salt, Grana Padano, rocket, onions and extra basil.",
    dietaryTags: ["V", "GF", "VE"],
    id: "sfizio-al-pomodoro",
    name: "SFIZIO AL POMODORO",
    price: 6.95,
    productType: "starter",
    slug: "sfizio-al-pomodoro",
  },
  {
    available: true,
    category: "bites-to-start",
    description:
      "Buffalo mozzarella from campana isle of wight tomato, beef tomato with trapani salt, grana padano, rocket, onions and extra basil.",
    dietaryTags: ["VE"],
    id: "tagliere-misto-sharing-platter-for-2",
    name: "TAGLIERE MISTO (A SHARING PLATTER FOR 2)",
    price: 12.95,
    productType: "starter",
    slug: "tagliere-misto-sharing-platter-for-2",
  },
  {
    available: true,
    category: "sourdough-pizza-calzone",
    customizable: false,
    description:
      "Crushed San Marzano tomatoes, fior di latte cheese, parmesan DOP, extra virgin olive oil and fresh basil. [Choose Buffalo mozzarella instead on any pizza for £2.75]",
    dietaryTags: ["V", "GF", "VE"],
    featured: true,
    id: "margherita",
    image: {
      alt: "Pizza Margherita at Basilico",
      src: "/images/menu/margherita.png",
    },
    name: "PIZZA MARGHERITA",
    price: 8.99,
    productType: "pizza",
    slug: "pizza-margherita",
  },
  {
    available: true,
    category: "sourdough-pizza-calzone",
    description:
      "Crushed San Marzano tomatoes, fior di latte cheese, fresh caramelised pineapple pieces and honey roasted ham.",
    dietaryTags: ["V", "GF", "VE"],
    id: "lerrore-the-mistake",
    name: "L'ERRORE (THE MISTAKE)",
    price: 9.99,
    productType: "pizza",
    slug: "lerrore-the-mistake",
  },
  {
    available: true,
    category: "sourdough-pizza-calzone",
    description:
      "For those who love a bit of a kick, our Piccante Formaggiata pizza is a must-try. This pizza features a zesty tomato base loaded with crunchy onions, bell peppers, fresh tomatoes, and spicy jalapeños, all generously topped with double cheese for a mouthwatering finish.",
    dietaryTags: ["V", "GF", "VE"],
    id: "piccante-formaggiata",
    name: "PICCANTE FORMAGGIATA",
    price: 11.99,
    productType: "pizza",
    slug: "piccante-formaggiata",
  },
  {
    available: true,
    category: "sourdough-pizza-calzone",
    description:
      "Discover a delightful fusion of Mediterranean flavours with our Tofu Mediterraneo pizza. A rich tomato base is generously topped with marinated tofu cubes, sun-dried tomatoes, roasted red peppers, artichoke hearts, and a sprinkle of fresh oregano. This plant-based pizza offers a perfect balance of savoury and tangy notes, making it a delicious and healthy choice for all pizza lovers.",
    dietaryTags: ["V", "GF"],
    id: "tofu-mediterraneo",
    name: "TOFU MEDITERRANEO",
    price: 12.99,
    productType: "pizza",
    slug: "tofu-mediterraneo",
  },
  {
    available: true,
    category: "sourdough-pizza-calzone",
    description:
      "Fior di latte cheese, blanched asparagus tips, creamy goat cheese, red onion marmalade, mild chilli & lemon compote dressed with extra virgin oil.",
    dietaryTags: ["V", "GF", "VE"],
    id: "a-bella-figliola",
    name: "A BELLA FIGLIOLA",
    price: 11.99,
    productType: "pizza",
    slug: "a-bella-figliola",
  },
  {
    available: true,
    category: "sourdough-pizza-calzone",
    description:
      "White base of ricotta and fior di latte cheese, courgettes, Isle of Wight tomatoes and capers finished with caramelised lemon zest. [Vegan cheese instead of mozzarella or as an extra topping £1.95]",
    dietaryTags: ["V", "GF", "VE"],
    id: "pizza-e-fantasia",
    name: "PIZZA E FANTASIA",
    price: 11.99,
    productType: "pizza",
    slug: "pizza-e-fantasia",
  },
  {
    available: true,
    category: "sourdough-pizza-calzone",
    description:
      "White base pizza topped with fior di latte cheese, goat cheese, ricotta cheese and gorgonzola finished with freshly ground black pepper. [Add tomato sauce or rainbow cherry tomatoes for £1.95]",
    dietaryTags: ["V", "GF"],
    id: "pizza-cacio-e-pepe",
    name: "PIZZA CACIO E PEPE",
    price: 10.99,
    productType: "pizza",
    slug: "pizza-cacio-e-pepe",
  },
  {
    available: true,
    category: "sourdough-pizza-calzone",
    description:
      "San Marzano tomato sauce, fior di latte cheese, Isle of Wight cherry tomatoes, Pantelleria capers, Nocellara olives, Cetara anchovies, garlic oil and fresh basil. [Add chilli & lemon compote for £1.95]",
    dietaryTags: ["V", "GF", "VE"],
    id: "alicella-sagliuta",
    name: "ALICELLA SAGLIUTA",
    price: 12.99,
    productType: "pizza",
    slug: "alicella-sagliuta",
  },
  {
    available: true,
    category: "sourdough-pizza-calzone",
    description:
      "Experience the freshness of our Verdure Fresche pizza, adorned with a vibrant mix of bell peppers, mushrooms, raw onions, juicy fresh tomatoes, and sweetcorn. Perfect for vegetable lovers, this pizza is a celebration of crisp, garden-fresh flavours on a classic tomato base.",
    dietaryTags: ["V", "GF", "VE"],
    id: "verdure-fresche",
    name: "VERDURE FRESCHE",
    price: 10.99,
    productType: "pizza",
    slug: "verdure-fresche",
  },
  {
    available: true,
    category: "sourdough-pizza-calzone",
    description:
      "Savour the ultimate cheesy delight with our Pepperoni Doppio Formaggio pizza. A robust tomato base is layered with spicy pepperoni and smothered in double cheese, delivering a rich and satisfying flavour in every slice. Perfect for cheese and meat lovers alike.",
    dietaryTags: ["GF"],
    id: "pepperoni-doppio-formaggio",
    name: "PEPPERONI DOPPIO FORMAGGIO",
    price: 12.95,
    productType: "pizza",
    slug: "pepperoni-doppio-formaggio",
  },
  {
    available: true,
    category: "sourdough-pizza-calzone",
    description:
      "Isle of Wight roasted tomato sauce, smoked garlic and Buffalo mozzarella, mushrooms, Nocellara olives, artichokes and roasted ham, finished Extra virgin oil & fresh parmesan",
    dietaryTags: ["V", "GF", "VE"],
    id: "o-core-e-napule",
    name: "O CORE E NAPULE",
    price: 12.95,
    productType: "pizza",
    slug: "o-core-e-napule",
  },
  {
    available: true,
    category: "sourdough-pizza-calzone",
    description:
      "Lightly smoked beechwood spicy salami, Isle of Wight roasted tomato salsa, fior di latte cheese, red onion marmalade and crumble of gorgonzola dressed with 'nduja, mild chilli & lemon compote.",
    dietaryTags: ["GF"],
    id: "pizza-do-putecaro",
    name: "PIZZA DO PUTECARO",
    price: 12.95,
    productType: "pizza",
    slug: "pizza-do-putecaro",
  },
  {
    available: true,
    category: "sourdough-pizza-calzone",
    description:
      "White base with Buffalo mozzarella, Parma ham, rainbow cherry tomatoes, shaved Parmesan cheese, basil & extra virgin olive oil. [Add rocket salad as an extra topping £1.95]",
    dietaryTags: ["V", "GF", "VE"],
    id: "pizza-bufala",
    name: "PIZZA BUFALA",
    price: 11.95,
    productType: "pizza",
    slug: "pizza-bufala",
  },
  {
    available: true,
    category: "sourdough-pizza-calzone",
    description:
      "Beef Meat Ragout, fior di latte cheese, Buffalo meatballs, Buffalo Ricotta Cheese, basil and extra virgin olive oil.",
    dietaryTags: ["GF"],
    id: "lazzarella",
    name: "LAZZARELLA",
    price: 12.95,
    productType: "pizza",
    slug: "lazzarella",
  },
  {
    available: true,
    category: "sourdough-pizza-calzone",
    description:
      "Indulge in our Pollo Funghi Dolce pizza, featuring a savoury tomato base topped with tender chicken, earthy mushrooms, and sweetcorn. A delightful combination that brings together rich flavours and a touch of sweetness in every bite.",
    dietaryTags: ["GF"],
    id: "pollo-funghi-dolce",
    name: "POLLO FUNGHI DOLCE",
    price: 12.95,
    productType: "pizza",
    slug: "pollo-funghi-dolce",
  },
  {
    available: true,
    category: "specials",
    description:
      "Layers of baked pasta with creamy Bolognese sauce, egg, mozzarella cheese and Parmesan DOP.",
    dietaryTags: [],
    featured: true,
    id: "lasagna",
    image: {
      alt: "Lasagna at Basilico",
      src: "/images/menu/lasagna.png",
    },
    name: "LASAGNA",
    price: 10.99,
    productType: "special",
    slug: "lasagna",
  },
  {
    available: true,
    category: "specials",
    description:
      "Layered roasted aubergine bake, smoked provola mozzarella, Parmesan cheese and Italian tomato sauce.",
    dietaryTags: ["V", "GF", "VE"],
    id: "parmigiana-di-melanzane",
    name: "PARMIGIANA DI MELANZANE",
    price: 9.95,
    productType: "special",
    slug: "parmigiana-di-melanzane",
  },
  {
    available: true,
    category: "specials",
    description:
      "We believe in quality, so we have created a special blend of 100% Dorset reared minced beef for a perfect juicy 6oz burger, topped with blue cheese, bacon, caramelised red onions & gherkins finished with fresh salad & tomatoes in our homemade bread & accompanied with potato gratin.",
    dietaryTags: [],
    id: "the-italian-job",
    name: "THE ITALIAN JOB",
    price: 12.95,
    productType: "special",
    slug: "the-italian-job",
  },
  {
    available: true,
    category: "specials",
    description:
      "Chicken breast or thighs, roasted Isle of Wight cherry tomatoes, olives and buffalo mozzarella. Served with potato gratin.",
    dietaryTags: ["GF"],
    id: "pollo-al-forno",
    name: "POLLO AL FORNO",
    price: 11.95,
    productType: "special",
    slug: "pollo-al-forno",
  },
  {
    available: true,
    category: "sweet-tooth",
    description:
      "Espresso & liqueur-soaked finger biscuit & layers of mascarpone & cocoa.",
    dietaryTags: [],
    id: "tiramisu",
    name: "TIRAMISÙ",
    price: 5.99,
    productType: "dessert",
    slug: "tiramisu",
  },
  {
    available: true,
    category: "sweet-tooth",
    description:
      "A deliciously decadent Triple Chocolate Brownie recipe that was inspired by dietary requirements and brought to life for you.",
    dietaryTags: ["V", "GF", "VE"],
    id: "triple-chocolate-brownie",
    name: "TRIPLE CHOCOLATE BROWNIE",
    price: 5.99,
    productType: "dessert",
    slug: "triple-chocolate-brownie",
  },
  {
    available: true,
    category: "sweet-tooth",
    description:
      "Sweet, warm dough filled with Nutella, banana & fluffy marshmallow drizzled with salted caramel sauce.",
    dietaryTags: [],
    featured: true,
    id: "calzoneallanutella",
    image: {
      alt: "Calzone alla Nutella at Basilico",
      src: "/images/menu/calzoneallanutella.png",
    },
    name: "CALZONE ALLA NUTELLA",
    price: 5.99,
    productType: "dessert",
    slug: "calzone-alla-nutella",
  },
  {
    available: true,
    category: "side-salad",
    dietaryTags: ["V", "GF", "VE"],
    id: "mixed-sides",
    name: "MIXED SIDES",
    price: 4.5,
    productType: "salad",
    slug: "mixed-sides",
  },
  {
    available: true,
    category: "side-salad",
    dietaryTags: ["V", "GF", "VE"],
    id: "rocket-side",
    name: "ROCKET SIDE",
    price: 4.95,
    productType: "salad",
    slug: "rocket-side",
  },
];

export const menuItems: MenuItem[] = referenceMenuItems.map(
  ({ price, ...item }) => ({
    ...item,
    pricePence: priceToPennies(price),
  }),
);

const referenceCreateYourOwnBlock: ReferenceCreateYourOwnConfiguration = {
  basePrice: 7,
  extraToppingCopy: "EXTRA TOPPINGS AVAILABLE — £1.25 EACH",
  extraToppingPrice: 1.25,
  pricingCopy: "FROM £7 + NO TOPPINGS",
  title: "CREATE YOUR OWN",
  toppings: [
    "Anchovies",
    "Artichokes",
    "Asparagus",
    "Capers",
    "Caramelised Onions",
    "Pineapple",
    "Courgettes",
    "Fior di latte cheese",
    "Fresh Chilli",
    "Gluten Free Base",
    "Goat cheese",
    "Gorgonzola cheese",
    "Mixed olives",
    "Mushrooms",
    "Nduja",
    "Onions",
    "Parma ham",
    "Rainbow cherry tomatoes",
    "Red onion marmalade",
    "Ricotta cheese",
    "Roasted ham",
    "Spicy salami",
    "Tomato sauce",
    "Vegan cheese",
    "Isle of Wight Roasted Tomato Sauce",
    "Sweet Corn",
    "Peppers",
    "Spicy jalapeños",
  ],
};

const {
  basePrice,
  extraToppingPrice,
  ...createYourOwnReferenceContent
} = referenceCreateYourOwnBlock;

export const createYourOwnBlock: CreateYourOwnConfiguration = {
  ...createYourOwnReferenceContent,
  basePricePence: priceToPennies(basePrice),
  extraToppingPricePence: priceToPennies(extraToppingPrice),
  toppings: createYourOwnReferenceContent.toppings.map(toReferenceToppingOption),
};

function toReferenceToppingOption(
  topping: string,
  index: number,
): CreateYourOwnToppingOption {
  return {
    available: true,
    displayOrder: index + 1,
    id: index + 1,
    name: topping,
  };
}

export function getMenuItemsByCategory(categoryId: MenuCategoryId) {
  return menuItems.filter((item) => item.category === categoryId);
}

export function getMenuCategoryById(categoryId: MenuCategoryId) {
  return menuCategories.find((category) => category.id === categoryId);
}

export function getMenuItemBySlug(slug: string) {
  return menuItems.find((item) => item.slug === slug);
}

export function getRelatedMenuItems(item: MenuItem, limit = 3) {
  return menuItems
    .filter(
      (candidate) =>
        candidate.category === item.category && candidate.slug !== item.slug,
    )
    .slice(0, limit);
}
