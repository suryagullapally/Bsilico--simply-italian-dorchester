import { getApi, postApi } from "@/lib/api/api-error";
import type {
  BackendCheckoutSessionResponse,
  BackendCheckoutSessionStatusResponse,
  BackendCreateCheckoutSessionRequest,
} from "@/types/backend-payment";

export function createCheckoutSession(orderReference: string) {
  return postApi<
    BackendCreateCheckoutSessionRequest,
    BackendCheckoutSessionResponse
  >("/api/payments/checkout-session", { orderReference });
}

export function getCheckoutSessionStatus(sessionId: string) {
  return getApi<BackendCheckoutSessionStatusResponse>(
    `/api/payments/checkout-session/${encodeURIComponent(sessionId)}/status`,
  );
}
