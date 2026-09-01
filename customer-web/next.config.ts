import type { NextConfig } from "next";

const legacyImageRedirects = [
  ["/brand/basilico-logo.png", "/brand/basilico-logo.webp"],
  ["/images/home/basilico-hero.png", "/images/home/basilico-hero.webp"],
  ["/images/home/order-basilico.png", "/images/home/order-basilico.webp"],
  ["/images/location/basilico-map.png", "/images/location/basilico-map.webp"],
  ["/images/restaurant/interior-01.jpg", "/images/restaurant/interior-01.webp"],
  ["/images/restaurant/interior-02.jpg", "/images/restaurant/interior-02.webp"],
  ["/images/restaurant/interior-03.jpg", "/images/restaurant/interior-03.webp"],
  ["/images/menu/margherita.png", "/images/menu/margherita.webp"],
  ["/images/menu/lasagna.png", "/images/menu/lasagna.webp"],
  [
    "/images/menu/schiacciatella.png",
    "/images/menu/starters/schiacciatella.webp",
  ],
  [
    "/images/menu/calzoneallanutella.png",
    "/images/menu/sweet-tooth/calzone-alla-nutella.webp",
  ],
  [
    "/images/menu/pizzas/a-bella-figliola.png",
    "/images/menu/pizzas/a-bella-figliola.webp",
  ],
  [
    "/images/menu/pizzas/alicella-sagliuta.png",
    "/images/menu/pizzas/alicella-sagliuta.webp",
  ],
  [
    "/images/menu/pizzas/lazzarella.png",
    "/images/menu/pizzas/lazzarella.webp",
  ],
  [
    "/images/menu/pizzas/lerrore-the-mistake.png",
    "/images/menu/pizzas/lerrore-the-mistake.webp",
  ],
  [
    "/images/menu/pizzas/margherita.png",
    "/images/menu/margherita.webp",
  ],
  [
    "/images/menu/pizzas/o-core-e-napule.png",
    "/images/menu/pizzas/o-core-e-napule.webp",
  ],
  [
    "/images/menu/pizzas/pepperoni-doppio-formaggio.png",
    "/images/menu/pizzas/pepperoni-doppio-formaggio.webp",
  ],
  [
    "/images/menu/pizzas/piccante-formaggiata.png",
    "/images/menu/pizzas/piccante-formaggiata.webp",
  ],
  [
    "/images/menu/pizzas/pizza-bufala.png",
    "/images/menu/pizzas/pizza-bufala.webp",
  ],
  [
    "/images/menu/pizzas/pizza-cacio-e-pepe.png",
    "/images/menu/pizzas/pizza-cacio-e-pepe.webp",
  ],
  [
    "/images/menu/pizzas/pizza-do-putecaro.png",
    "/images/menu/pizzas/pizza-do-putecaro.webp",
  ],
  [
    "/images/menu/pizzas/pizza-e-fantasia.png",
    "/images/menu/pizzas/pizza-e-fantasia.webp",
  ],
  [
    "/images/menu/pizzas/pollo-funghi-dolce.png",
    "/images/menu/pizzas/pollo-funghi-dolce.webp",
  ],
  [
    "/images/menu/pizzas/tofu-mediterraneo.png",
    "/images/menu/pizzas/tofu-mediterraneo.webp",
  ],
  [
    "/images/menu/pizzas/verdure-fresche.png",
    "/images/menu/pizzas/verdure-fresche.webp",
  ],
  [
    "/images/menu/side-salads/mixed-salad.png",
    "/images/menu/side-salads/mixed-salad.webp",
  ],
  [
    "/images/menu/side-salads/rocket-side.png",
    "/images/menu/side-salads/rocket-side.webp",
  ],
  [
    "/images/menu/starters/bruschetta.png",
    "/images/menu/starters/bruschetta.webp",
  ],
  [
    "/images/menu/starters/olive-di-nocellara.png",
    "/images/menu/starters/olive-di-nocellara.webp",
  ],
  [
    "/images/menu/starters/oppane-the-bread.png",
    "/images/menu/starters/oppane-the-bread.webp",
  ],
  [
    "/images/menu/starters/piadina-e-crema-di-pomodoro.png",
    "/images/menu/starters/piadina-e-crema-di-pomodoro.webp",
  ],
  [
    "/images/menu/starters/schiacciata-al-formaggio.png",
    "/images/menu/starters/schiacciata-al-formaggio.webp",
  ],
  [
    "/images/menu/starters/schiacciatella.png",
    "/images/menu/starters/schiacciatella.webp",
  ],
  [
    "/images/menu/starters/sfizio-al-pomodoro.png",
    "/images/menu/starters/sfizio-al-pomodoro.webp",
  ],
  [
    "/images/menu/starters/tagliere-misto.png",
    "/images/menu/starters/tagliere-misto.webp",
  ],
  [
    "/images/menu/sweet-tooth/calzone-alla-nutella.png",
    "/images/menu/sweet-tooth/calzone-alla-nutella.webp",
  ],
  [
    "/images/menu/sweet-tooth/tiramisu.png",
    "/images/menu/sweet-tooth/tiramisu.webp",
  ],
  [
    "/images/menu/sweet-tooth/triple-chocolate-brownie.png",
    "/images/menu/sweet-tooth/triple-chocolate-brownie.webp",
  ],
] satisfies Array<[string, string]>;

const nextConfig: NextConfig = {
  async headers() {
    return [
      {
        source: "/(.*)",
        headers: [
          {
            key: "X-Content-Type-Options",
            value: "nosniff",
          },
          {
            key: "Referrer-Policy",
            value: "strict-origin-when-cross-origin",
          },
          {
            key: "X-Frame-Options",
            value: "DENY",
          },
          {
            key: "Permissions-Policy",
            value: "camera=(), microphone=(), geolocation=(self)",
          },
        ],
      },
    ];
  },
  async redirects() {
    return legacyImageRedirects.map(([source, destination]) => ({
      source,
      destination,
      permanent: true,
    }));
  },
};

export default nextConfig;
