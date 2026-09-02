const DEFAULT_SITE_URL = "https://basilicodorchester.co.uk";

export const SITE_URL = normalizeUrl(
  process.env.NEXT_PUBLIC_SITE_URL ?? DEFAULT_SITE_URL,
);

export const SITE_NAME = "Basilico - Simple Italian";
export const SITE_DESCRIPTION =
  "Authentic Italian food and sourdough pizza at Basilico in Dorchester, Dorset. Dine in, order takeaway or delivery, or book a table online.";

export const BASILICO_ADDRESS = {
  country: "GB",
  locality: "Dorchester",
  postalCode: "DT1 1TT",
  region: "Dorset",
  street: "41 Trinity Street",
} as const;

export const BASILICO_PHONE_DISPLAY = "07424 642900";
export const BASILICO_PHONE_E164 = "+44 7424 642900";
export const BASILICO_PHONE_HREF = "tel:+447424642900";
export const BASILICO_LOGO_PATH = "/brand/basilico-logo.webp";
export const BASILICO_OG_IMAGE_PATH = "/images/home/basilico-hero.webp";
export const BASILICO_OG_IMAGE_ALT =
  "Basilico Italian restaurant and pizza in Dorchester";

export function siteUrl(path = "/") {
  const normalizedPath = path.startsWith("/") ? path : `/${path}`;
  return `${SITE_URL}${normalizedPath}`;
}

function normalizeUrl(url: string) {
  return url.replace(/\/+$/, "");
}
