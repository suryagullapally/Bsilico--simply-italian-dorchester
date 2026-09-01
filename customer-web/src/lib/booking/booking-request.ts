import type { BookingState } from "@/lib/booking/booking-types";
import type { BackendCreateBookingRequest } from "@/types/backend-booking";

export function buildCreateBookingRequest(
  booking: BookingState,
): BackendCreateBookingRequest {
  return {
    date: booking.date,
    email: booking.customer.email,
    firstName: booking.customer.firstName,
    lastName: booking.customer.lastName,
    partySize: booking.partySize,
    phone: booking.customer.phone,
    specialRequests: booking.requests || undefined,
    time: booking.time,
  };
}
