import { routes } from "@/lib/routes";

export type NavigationLink = {
  href: string;
  label: string;
};

export type Dish = {
  description: string;
  href: string;
  imageSrc: string;
  name: string;
  price: string;
};

export type GalleryImage = {
  alt: string;
  src: string;
};

export const primaryNavigation: NavigationLink[] = [
  { href: routes.menu, label: "Menu" },
  { href: routes.order, label: "Order Online" },
  { href: routes.book, label: "Book a Table" },
  { href: routes.story, label: "Our Story" },
  { href: routes.visit, label: "Visit Us" },
];

export const signatureDishes: Dish[] = [
  {
    description:
      "Crushed San Marzano tomatoes, fior di latte cheese, parmesan DOP, extra virgin olive oil and Fresh basil.",
    href: "/menu/pizza-margherita",
    imageSrc: "/images/menu/margherita.webp",
    name: "PIZZA MARGHERITA | V GF VE",
    price: "£8.99",
  },
  {
    description:
      "Layers of baked pasta with creamy Bolognese sauce, egg, mozzarella cheese and Parmesan DOP.",
    href: "/menu/lasagna",
    imageSrc: "/images/menu/lasagna.webp",
    name: "LASAGNA",
    price: "£10.99",
  },
  {
    description: "Pizza bread with garlic oil & rosemary sprinkled with sea salt.",
    href: "/menu/schiacciatella",
    imageSrc: "/images/menu/starters/schiacciatella.webp",
    name: "SCHIACCIATELLA | V VE",
    price: "£4.45",
  },
  {
    description:
      "Sweet, warm dough filled with Nutella, banana & fluffy marshmallow drizzled with salted caramel sauce.",
    href: "/menu/calzone-alla-nutella",
    imageSrc: "/images/menu/sweet-tooth/calzone-alla-nutella.webp",
    name: "CALZONE ALLA NUTELLA",
    price: "£5.99",
  },
];

export const restaurantImages: GalleryImage[] = [
  {
    alt: "Basilico dining room with tables leading toward the restaurant counter",
    src: "/images/restaurant/interior-01.webp",
  },
  {
    alt: "Basilico dining room seating and warm table lighting",
    src: "/images/restaurant/interior-02.webp",
  },
  {
    alt: "Basilico wood-fired oven glowing with flame",
    src: "/images/restaurant/interior-03.webp",
  },
];
