const DEFAULT_API_BASE_URL = "http://localhost:8080";

export const API_BASE_URL = normalizeApiBaseUrl(
  process.env.NEXT_PUBLIC_API_BASE_URL ?? DEFAULT_API_BASE_URL,
);

export function getApiUrl(path: string) {
  const normalizedPath = path.startsWith("/") ? path : `/${path}`;

  return `${API_BASE_URL}${normalizedPath}`;
}

function normalizeApiBaseUrl(url: string) {
  return url.replace(/\/+$/, "");
}
