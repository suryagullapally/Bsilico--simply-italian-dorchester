import type {
  BackendOrderStatus,
  BackendPaymentStatus,
} from "@/types/backend-order";

export type BackendCreateCheckoutSessionRequest = {
  orderReference: string;
};

export type BackendCheckoutSessionResponse = {
  amountPence: number;
  checkoutSessionClientSecret: string;
  currency: string;
  orderReference: string;
  stripeCheckoutSessionId: string;
};

export type BackendPaymentAttemptStatus =
  | "CREATED"
  | "EXPIRED"
  | "FAILED"
  | "OPEN"
  | "PAID";

export type BackendCheckoutSessionStatusResponse = {
  checkoutSessionPaymentStatus: string | null;
  checkoutSessionStatus: string | null;
  orderReference: string;
  orderStatus: BackendOrderStatus;
  paymentAttemptStatus: BackendPaymentAttemptStatus;
  paymentStatus: BackendPaymentStatus;
  stripeCheckoutSessionId: string;
};
