import { adminGet, adminMutation } from "@/lib/api/api-error";
import type {
  NotificationResponse,
  NotificationStatus,
  NotificationType,
  PageResponse,
  SendManualEmailRequest,
} from "@/types/admin";

export type NotificationFilters = {
  status?: NotificationStatus;
  type?: NotificationType;
  orderId?: number;
  bookingId?: number;
  page?: number;
  size?: number;
};

export async function getAdminNotifications(filters: NotificationFilters = {}) {
  return adminGet<PageResponse<NotificationResponse>>(
    "/api/admin/messages",
    filters,
  );
}

export async function getAdminNotification(id: string | number) {
  return adminGet<NotificationResponse>(`/api/admin/messages/${id}`);
}

export async function sendManualEmail(request: SendManualEmailRequest) {
  return adminMutation<NotificationResponse>(
    "/api/admin/messages/email",
    "POST",
    request,
  );
}

export async function retryNotification(id: string | number) {
  return adminMutation<NotificationResponse>(
    `/api/admin/messages/${id}/retry`,
    "POST",
    {},
  );
}
