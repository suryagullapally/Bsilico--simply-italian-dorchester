import { adminGet, adminMutation } from "@/lib/api/api-error";
import type {
  AdminBookingResponse,
  AdminBookingSummaryResponse,
  BookingStatus,
  PageResponse,
} from "@/types/admin";

export type BookingFilters = {
  date?: string;
  status?: BookingStatus;
  page?: number;
  size?: number;
};

export async function getAdminBookings(filters: BookingFilters = {}) {
  return adminGet<PageResponse<AdminBookingSummaryResponse>>(
    "/api/admin/bookings",
    filters,
  );
}

export async function getAdminBooking(id: string | number) {
  return adminGet<AdminBookingResponse>(`/api/admin/bookings/${id}`);
}

export async function updateBookingStatus(
  id: string | number,
  status: BookingStatus,
) {
  return adminMutation<AdminBookingResponse>(
    `/api/admin/bookings/${id}/status`,
    "PATCH",
    { status },
  );
}
