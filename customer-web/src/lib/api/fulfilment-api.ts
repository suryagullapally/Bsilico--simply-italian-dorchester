import { getApi, postApi } from "@/lib/api/api-error";
import type {
  BackendCheckDeliveryRequest,
  BackendDeliveryEligibilityResponse,
  BackendDeliveryQuoteRequest,
  BackendDeliveryQuoteResponse,
  BackendFulfilmentOptionsResponse,
} from "@/types/backend-fulfilment";

export function getFulfilmentOptions() {
  return getApi<BackendFulfilmentOptionsResponse>("/api/fulfilment/options");
}

export function checkDeliveryEligibility(request: BackendCheckDeliveryRequest) {
  return postApi<
    BackendCheckDeliveryRequest,
    BackendDeliveryEligibilityResponse
  >("/api/fulfilment/check-delivery", request);
}

export function getDeliveryQuote(request: BackendDeliveryQuoteRequest) {
  return postApi<BackendDeliveryQuoteRequest, BackendDeliveryQuoteResponse>(
    "/api/fulfilment/delivery-quote",
    request,
  );
}
