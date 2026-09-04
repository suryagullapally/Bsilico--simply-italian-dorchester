import { adminGet, adminMutation } from "@/lib/api/api-error";
import type {
  AdminOrderResponse,
  AdminOrderSummaryResponse,
  FulfilmentType,
  OrderStatus,
  PageResponse,
  PaymentStatus,
  RefundOrderRequest,
} from "@/types/admin";

export type OrderFilters = {
  status?: OrderStatus;
  paymentStatus?: PaymentStatus;
  fulfilmentType?: FulfilmentType;
  page?: number;
  size?: number;
};

export async function getAdminOrders(filters: OrderFilters = {}) {
  return adminGet<PageResponse<AdminOrderSummaryResponse>>(
    "/api/admin/orders",
    filters,
  );
}

export async function getAdminOrder(id: string | number) {
  return adminGet<AdminOrderResponse>(`/api/admin/orders/${id}`);
}

export async function updateOrderStatus(id: string | number, status: OrderStatus) {
  return adminMutation<AdminOrderResponse>(
    `/api/admin/orders/${id}/status`,
    "PATCH",
    { status },
  );
}

export async function updateOrderPaymentStatus(
  id: string | number,
  paymentStatus: PaymentStatus,
) {
  return adminMutation<AdminOrderResponse>(
    `/api/admin/orders/${id}/payment-status`,
    "PATCH",
    { paymentStatus },
  );
}

export async function refundOrder(id: string | number, request: RefundOrderRequest) {
  return adminMutation<AdminOrderResponse>(
    `/api/admin/orders/${id}/refund`,
    "POST",
    request,
  );
}
