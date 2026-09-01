const DEFAULT_SITE_URL = "http://localhost:3000";

export const SITE_URL = normalizeUrl(
  process.env.NEXT_PUBLIC_SITE_URL ?? DEFAULT_SITE_URL,
);

export const SITE_NAME = "Basilico - Simple Italian";
export const SITE_DESCRIPTION =
  "Basilico is an Italian restaurant serving sourdough pizza and Italian favourites in Dorchester, Dorset.";

export function siteUrl(path = "/") {
  const normalizedPath = path.startsWith("/") ? path : `/${path}`;
  return `${SITE_URL}${normalizedPath}`;
}

function normalizeUrl(url: string) {
  return url.replace(/\/+$/, "");
}
