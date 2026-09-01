import { postApi } from "@/lib/api/api-error";
import type {
  BackendCreateOrderRequest,
  BackendOrderResponse,
} from "@/types/backend-order";

export function createOrder(request: BackendCreateOrderRequest) {
  return postApi<BackendCreateOrderRequest, BackendOrderResponse>(
    "/api/orders",
    request,
  );
}
