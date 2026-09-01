"use client";

import { useRouter } from "next/navigation";
import { useState, useTransition } from "react";
import { updateOrderPaymentStatus, updateOrderStatus } from "@/lib/api/admin-orders-api";
import { getAdminApiErrorMessage } from "@/lib/api/api-error";
import { ENABLE_DEV_PAYMENT_CONTROL } from "@/lib/api/config";
import { formatStatus, paymentStatuses } from "@/lib/status";
import type { AdminOrderResponse, OrderStatus, PaymentStatus } from "@/types/admin";

type OrderStatusActionsProps = {
  orderId: number;
  currentStatus: OrderStatus;
  currentPaymentStatus: PaymentStatus;
  onOrderUpdated?: (order: AdminOrderResponse) => void;
};

export function OrderStatusActions({
  orderId,
  currentStatus,
  currentPaymentStatus,
  onOrderUpdated,
}: OrderStatusActionsProps) {
  const router = useRouter();
  const [pending, startTransition] = useTransition();
  const [feedback, setFeedback] = useState("");
  const [error, setError] = useState("");
  const [confirmCancel, setConfirmCancel] = useState(false);
  const [paymentStatus, setPaymentStatus] =
    useState<PaymentStatus>(currentPaymentStatus);

  async function setStatus(status: OrderStatus) {
    setError("");
    setFeedback("");

    try {
      const updated = await updateOrderStatus(orderId, status);
      setConfirmCancel(false);
      setFeedback(`Order marked as ${formatStatus(status).toLowerCase()}.`);
      onOrderUpdated?.(updated);
      startTransition(() => router.refresh());
    } catch (caught) {
      setError(getAdminApiErrorMessage(caught, "Could not update the order."));
    }
  }

  async function setPayment() {
    setError("");
    setFeedback("");

    try {
      const updated = await updateOrderPaymentStatus(orderId, paymentStatus);
      setFeedback(
        `Development payment status set to ${formatStatus(paymentStatus).toLowerCase()}.`,
      );
      onOrderUpdated?.(updated);
      startTransition(() => router.refresh());
    } catch (caught) {
      setError(getAdminApiErrorMessage(caught, "Could not update payment status."));
    }
  }

  const nextAction = getNextStatusAction(currentStatus);

  return (
    <div className="grid gap-4">
      <section className="panel">
        <div className="panel__body">
          <p className="eyebrow">Order actions</p>
          <div className="action-row mt-4">
            {nextAction ? (
              <button
                className="button-secondary"
                disabled={pending}
                type="button"
                onClick={() => setStatus(nextAction.status)}
              >
                {nextAction.label}
              </button>
            ) : null}
            {currentStatus !== "CANCELLED" && currentStatus !== "COMPLETED" ? (
              confirmCancel ? (
                <>
                  <button
                    className="button-danger"
                    disabled={pending}
                    type="button"
                    onClick={() => setStatus("CANCELLED")}
                  >
                    Confirm cancel
                  </button>
                  <button
                    className="button-ghost"
                    disabled={pending}
                    type="button"
                    onClick={() => setConfirmCancel(false)}
                  >
                    Keep order
                  </button>
                </>
              ) : (
                <button
                  className="button-ghost"
                  disabled={pending}
                  type="button"
                  onClick={() => setConfirmCancel(true)}
                >
                  Cancel order
                </button>
              )
            ) : null}
          </div>
        </div>
      </section>

      {ENABLE_DEV_PAYMENT_CONTROL ? (
        <section className="panel">
          <div className="panel__body">
            <p className="eyebrow">Development payment control</p>
            <p className="mt-2 text-sm text-muted">
              Stripe webhook updates are the real payment path. This manual
              control is enabled only for local development testing.
            </p>
            <div className="mt-4 flex flex-col gap-3 sm:flex-row">
              <label className="field flex-1">
                <span>Payment status</span>
                <select
                  value={paymentStatus}
                  onChange={(event) =>
                    setPaymentStatus(event.target.value as PaymentStatus)
                  }
                >
                  {paymentStatuses.map((status) => (
                    <option key={status} value={status}>
                      {formatStatus(status)}
                    </option>
                  ))}
                </select>
              </label>
              <div className="flex items-end">
                <button
                  className="button-secondary w-full"
                  disabled={pending}
                  type="button"
                  onClick={setPayment}
                >
                  Update payment
                </button>
              </div>
            </div>
          </div>
        </section>
      ) : null}

      <div aria-live="polite">
        {feedback ? <p className="feedback">{feedback}</p> : null}
        {error ? <p className="error">{error}</p> : null}
      </div>
    </div>
  );
}

function getNextStatusAction(status: OrderStatus) {
  switch (status) {
    case "PENDING_PAYMENT":
      return { label: "Mark new", status: "NEW" as const };
    case "NEW":
      return { label: "Accept order", status: "ACCEPTED" as const };
    case "ACCEPTED":
      return { label: "Start preparing", status: "PREPARING" as const };
    case "PREPARING":
      return { label: "Mark ready", status: "READY" as const };
    case "READY":
      return { label: "Mark completed", status: "COMPLETED" as const };
    case "COMPLETED":
    case "CANCELLED":
      return null;
  }
}
