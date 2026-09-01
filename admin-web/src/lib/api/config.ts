const DEFAULT_API_BASE_URL = "http://localhost:8080";
const DEFAULT_CUSTOMER_WEB_URL = "http://localhost:3001";

export const API_BASE_URL = normalizeUrl(
  process.env.NEXT_PUBLIC_API_BASE_URL ?? DEFAULT_API_BASE_URL,
);

export const CUSTOMER_WEB_URL = normalizeUrl(
  process.env.NEXT_PUBLIC_CUSTOMER_WEB_URL ?? DEFAULT_CUSTOMER_WEB_URL,
);

export const ENABLE_DEV_PAYMENT_CONTROL =
  process.env.NEXT_PUBLIC_ENABLE_DEV_PAYMENT_CONTROL === "true";

export function getApiUrl(path: string) {
  const normalizedPath = path.startsWith("/") ? path : `/${path}`;

  return `${API_BASE_URL}${normalizedPath}`;
}

function normalizeUrl(url: string) {
  return url.replace(/\/+$/, "");
}
