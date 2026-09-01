import { postApi } from "@/lib/api/api-error";
import type {
  BackendBookingResponse,
  BackendCreateBookingRequest,
} from "@/types/backend-booking";

export function createBooking(request: BackendCreateBookingRequest) {
  return postApi<BackendCreateBookingRequest, BackendBookingResponse>(
    "/api/bookings",
    request,
  );
}
