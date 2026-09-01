import { getApiUrl } from "@/lib/api/config";

type BackendErrorResponse = {
  error?: string;
  message?: string;
  status?: number;
};

export class ApiRequestError extends Error {
  readonly code: string;
  readonly status?: number;

  constructor(message: string, code = "REQUEST_FAILED", status?: number) {
    super(message);
    this.name = "ApiRequestError";
    this.code = code;
    this.status = status;
  }
}

export async function postApi<TRequest, TResponse>(
  path: string,
  body: TRequest,
) {
  let response: Response;

  try {
    response = await fetch(getApiUrl(path), {
      body: JSON.stringify(body),
      headers: {
        Accept: "application/json",
        "Content-Type": "application/json",
      },
      method: "POST",
    });
  } catch {
    throw new ApiRequestError(
      "We could not reach Basilico right now. Please try again.",
      "NETWORK_ERROR",
    );
  }

  if (!response.ok) {
    const error = await parseErrorResponse(response);
    throw new ApiRequestError(
      error.message,
      error.code,
      response.status,
    );
  }

  return (await response.json()) as TResponse;
}

export async function getApi<TResponse>(path: string) {
  let response: Response;

  try {
    response = await fetch(getApiUrl(path), {
      headers: {
        Accept: "application/json",
      },
      method: "GET",
    });
  } catch {
    throw new ApiRequestError(
      "We could not reach Basilico right now. Please try again.",
      "NETWORK_ERROR",
    );
  }

  if (!response.ok) {
    const error = await parseErrorResponse(response);
    throw new ApiRequestError(error.message, error.code, response.status);
  }

  return (await response.json()) as TResponse;
}

export function getApiErrorMessage(error: unknown, fallback: string) {
  if (error instanceof ApiRequestError) {
    return error.message;
  }

  return fallback;
}

async function parseErrorResponse(response: Response) {
  try {
    const value = (await response.json()) as BackendErrorResponse;

    return {
      code: value.error ?? "REQUEST_FAILED",
      message: value.message?.trim() || fallbackMessageForStatus(response.status),
    };
  } catch {
    return {
      code: "REQUEST_FAILED",
      message: fallbackMessageForStatus(response.status),
    };
  }
}

function fallbackMessageForStatus(status: number) {
  if (status === 409) {
    return "One of the items in your basket is no longer available. Please review your order.";
  }

  if (status === 400) {
    return "Some order details need checking before we can continue.";
  }

  if (status === 404) {
    return "One of the items in your basket is no longer available on the menu.";
  }

  return "We could not complete that request right now. Please try again.";
}
