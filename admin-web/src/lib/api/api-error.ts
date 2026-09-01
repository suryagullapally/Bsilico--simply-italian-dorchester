import { getApiUrl } from "@/lib/api/config";
import type { CsrfTokenResponse } from "@/types/admin";

type BackendErrorResponse = {
  error?: string;
  message?: string;
  status?: number;
};

export class AdminApiError extends Error {
  readonly status?: number;
  readonly code: string;

  constructor(message: string, code = "REQUEST_FAILED", status?: number) {
    super(message);
    this.name = "AdminApiError";
    this.code = code;
    this.status = status;
  }
}

type QueryValue = string | number | boolean | undefined | null;
let cachedCsrfToken: CsrfTokenResponse | null = null;

export async function adminGet<T>(
  path: string,
  query?: Record<string, QueryValue>,
) {
  let response: Response;

  try {
    response = await fetch(buildUrl(path, query), {
      cache: "no-store",
      credentials: "include",
      headers: {
        Accept: "application/json",
      },
    });
  } catch {
    throw new AdminApiError("Could not reach the backend. Try again.", "NETWORK_ERROR");
  }

  return parseResponse<T>(response);
}

export async function adminMutation<T>(
  path: string,
  method: "POST" | "PUT" | "PATCH",
  body: unknown,
) {
  return sendAdminMutation<T>(path, method, body, true);
}

async function sendAdminMutation<T>(
  path: string,
  method: "POST" | "PUT" | "PATCH",
  body: unknown,
  retryOnForbidden: boolean,
) {
  let response: Response;
  const csrfToken = await getAdminCsrfToken();

  try {
    response = await fetch(getApiUrl(path), {
      body: body === undefined ? undefined : JSON.stringify(body),
      credentials: "include",
      headers: {
        Accept: "application/json",
        "Content-Type": "application/json",
        [csrfToken.headerName]: csrfToken.token,
      },
      method,
    });
  } catch {
    throw new AdminApiError("Could not reach the backend. Try again.", "NETWORK_ERROR");
  }

  if (response.status === 403) {
    clearAdminCsrfToken();
    if (retryOnForbidden) {
      return sendAdminMutation<T>(path, method, body, false);
    }
  }

  return parseResponse<T>(response);
}

export async function getAdminCsrfToken(forceRefresh = false) {
  if (cachedCsrfToken && !forceRefresh) {
    return cachedCsrfToken;
  }

  let response: Response;

  try {
    response = await fetch(getApiUrl("/api/admin/auth/csrf"), {
      cache: "no-store",
      credentials: "include",
      headers: {
        Accept: "application/json",
      },
    });
  } catch {
    throw new AdminApiError("Could not reach the backend. Try again.", "NETWORK_ERROR");
  }

  cachedCsrfToken = await parseResponse<CsrfTokenResponse>(response);
  return cachedCsrfToken;
}

export function clearAdminCsrfToken() {
  cachedCsrfToken = null;
}

export function getAdminApiErrorMessage(error: unknown, fallback: string) {
  if (error instanceof AdminApiError) {
    return error.message;
  }

  return fallback;
}

function buildUrl(path: string, query?: Record<string, QueryValue>) {
  const url = new URL(getApiUrl(path));

  if (query) {
    for (const [key, value] of Object.entries(query)) {
      if (value !== undefined && value !== null && value !== "") {
        url.searchParams.set(key, String(value));
      }
    }
  }

  return url;
}

async function parseResponse<T>(response: Response) {
  if (!response.ok) {
    throw await createError(response);
  }

  if (response.status === 204) {
    return undefined as T;
  }

  return (await response.json()) as T;
}

async function createError(response: Response) {
  try {
    const value = (await response.json()) as BackendErrorResponse;

    return new AdminApiError(
      value.message?.trim() || fallbackMessage(response.status),
      value.error ?? "REQUEST_FAILED",
      response.status,
    );
  } catch {
    return new AdminApiError(
      fallbackMessage(response.status),
      "REQUEST_FAILED",
      response.status,
    );
  }
}

function fallbackMessage(status: number) {
  if (status === 400) {
    return "Some details need checking before the backend can save this.";
  }

  if (status === 404) {
    return "That record could not be found.";
  }

  if (status === 409) {
    return "The backend rejected this change because it conflicts with current data.";
  }

  if (status === 401) {
    return "Sign in to continue.";
  }

  if (status === 403) {
    return "The backend rejected this request. Refresh and try again.";
  }

  return "The backend could not complete that request.";
}
