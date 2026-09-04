import type {
  BookingStatus,
  FulfilmentType,
  OrderStatus,
  PaymentStatus,
} from "@/types/admin";

export const orderStatuses: OrderStatus[] = [
  "PENDING_PAYMENT",
  "NEW",
  "ACCEPTED",
  "PREPARING",
  "READY",
  "COMPLETED",
  "CANCELLED",
];

export const paymentStatuses: PaymentStatus[] = [
  "UNPAID",
  "PAID",
  "FAILED",
  "REFUNDED",
];

export const fulfilmentTypes: FulfilmentType[] = ["COLLECTION", "DELIVERY"];

export const bookingStatuses: BookingStatus[] = [
  "REQUESTED",
  "CONFIRMED",
  "DECLINED",
  "CANCELLED",
  "COMPLETED",
  "NO_SHOW",
];

export function formatStatus(value: string) {
  return value
    .split("_")
    .map((part) => part.charAt(0) + part.slice(1).toLowerCase())
    .join(" ");
}

export function statusTone(value: string) {
  if (value === "NEW" || value === "REQUESTED" || value === "READY" || value === "PENDING") {
    return "attention";
  }

  if (
    value === "PAID" ||
    value === "REFUNDED" ||
    value === "CONFIRMED" ||
    value === "COMPLETED" ||
    value === "SUCCEEDED" ||
    value === "SENT"
  ) {
    return "success";
  }

  if (
    value === "CANCELLED" ||
    value === "DECLINED" ||
    value === "FAILED" ||
    value === "CANCELED" ||
    value === "NO_SHOW"
  ) {
    return "danger";
  }

  if (
    value === "PREPARING" ||
    value === "ACCEPTED" ||
    value === "SENDING" ||
    value === "REQUIRES_ACTION"
  ) {
    return "working";
  }

  return "muted";
}
