"use client";

import { useRouter } from "next/navigation";
import { useState, useTransition } from "react";
import { refundOrder } from "@/lib/api/admin-orders-api";
import { getAdminApiErrorMessage } from "@/lib/api/api-error";
import { formatDateTime } from "@/lib/date-time";
import { formatGbpPennies } from "@/lib/format-price";
import { formatStatus } from "@/lib/status";
import type {
  AdminOrderResponse,
  PaymentRefundReason,
  PaymentRefundStatus,
} from "@/types/admin";
import { StatusBadge } from "@/components/ui/StatusBadge";

const refundReasons: Array<{ value: PaymentRefundReason; label: string }> = [
  { value: "CUSTOMER_REQUESTED", label: "Customer requested" },
  { value: "DUPLICATE", label: "Duplicate" },
  { value: "FRAUDULENT", label: "Fraudulent" },
  { value: "OTHER", label: "Other" },
];

export function RefundPaymentPanel({
  order,
  onOrderUpdated,
}: {
  order: AdminOrderResponse;
  onOrderUpdated?: (order: AdminOrderResponse) => void;
}) {
  const router = useRouter();
  const [pending, startTransition] = useTransition();
  const [modalOpen, setModalOpen] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [reason, setReason] =
    useState<PaymentRefundReason>("CUSTOMER_REQUESTED");
  const [note, setNote] = useState("");
  const [feedback, setFeedback] = useState("");
  const [error, setError] = useState("");

  async function handleRefund() {
    setError("");
    setFeedback("");

    if (reason === "OTHER" && note.trim().length === 0) {
      setError("Add a short note when the refund reason is Other.");
      return;
    }

    setSubmitting(true);
    try {
      const updated = await refundOrder(order.id, {
        reason,
        note: note.trim() || undefined,
      });
      onOrderUpdated?.(updated);
      setModalOpen(false);
      setFeedback(refundFeedback(updated.refundStatus));
      startTransition(() => router.refresh());
    } catch (caught) {
      setError(getAdminApiErrorMessage(caught, "Could not refund this payment."));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <section className="panel">
      <div className="panel__body">
        <div className="flex flex-wrap items-start justify-between gap-3">
          <div>
            <p className="eyebrow">Refund</p>
            <p className="mt-2 text-sm text-muted">
              Full Stripe refund to the customer&apos;s original payment method.
            </p>
          </div>
          {order.refundStatus ? <StatusBadge value={order.refundStatus} /> : null}
        </div>

        {order.refundStatus ? (
          <dl className="detail-list mt-4">
            <Detail
              label="Refund amount"
              value={
                order.refundAmountPence === null
                  ? "Not available"
                  : formatGbpPennies(order.refundAmountPence)
              }
            />
            <Detail label="Reason" value={formatStatus(order.refundReason ?? "OTHER")} />
            <Detail
              label="Requested"
              value={
                order.refundRequestedAt === null
                  ? "Not available"
                  : formatDateTime(order.refundRequestedAt)
              }
            />
            <Detail
              label="Completed"
              value={
                order.refundCompletedAt === null
                  ? "Not completed yet"
                  : formatDateTime(order.refundCompletedAt)
              }
            />
            {order.refundFailureReason ? (
              <Detail label="Failure" value={order.refundFailureReason} />
            ) : null}
          </dl>
        ) : (
          <p className="mt-4 text-muted">No refund has been requested for this order.</p>
        )}

        {order.refundEligible ? (
          <button
            className="button-danger mt-4 w-full"
            disabled={pending || submitting}
            type="button"
            onClick={() => {
              setError("");
              setFeedback("");
              setModalOpen(true);
            }}
          >
            Refund payment
          </button>
        ) : (
          <p className="mt-4 text-sm text-muted">
            Refund is unavailable unless the order is paid, has a Stripe
            PaymentIntent, and has no active or completed full refund.
          </p>
        )}

        <div aria-live="polite" className="mt-4">
          {feedback ? <p className="feedback">{feedback}</p> : null}
          {error ? <p className="error">{error}</p> : null}
        </div>
      </div>

      {modalOpen ? (
        <div
          aria-labelledby="refund-modal-title"
          aria-modal="true"
          className="fixed inset-0 z-50 grid place-items-center bg-black/70 p-4"
          role="dialog"
        >
          <div className="panel w-full max-w-lg">
            <div className="panel__body">
              <p className="eyebrow">Refund payment</p>
              <h2 className="mt-3 text-2xl font-black" id="refund-modal-title">
                Refund {formatGbpPennies(order.totalPence)}?
              </h2>
              <p className="mt-3 text-muted">
                This will return the full payment to the customer&apos;s
                original Stripe payment method.
              </p>

              <div className="mt-5 grid gap-4">
                <label className="field">
                  <span>Reason</span>
                  <select
                    value={reason}
                    onChange={(event) =>
                      setReason(event.target.value as PaymentRefundReason)
                    }
                  >
                    {refundReasons.map((option) => (
                      <option key={option.value} value={option.value}>
                        {option.label}
                      </option>
                    ))}
                  </select>
                </label>

                <label className="field">
                  <span>Note {reason === "OTHER" ? "" : "(optional)"}</span>
                  <textarea
                    rows={4}
                    value={note}
                    onChange={(event) => setNote(event.target.value)}
                    placeholder={
                      reason === "OTHER"
                        ? "Explain why this refund is being issued."
                        : "Add any internal context for this refund."
                    }
                  />
                </label>
              </div>

              <div aria-live="polite" className="mt-4">
                {error ? <p className="error">{error}</p> : null}
              </div>

              <div className="action-row mt-5">
                <button
                  className="button-ghost"
                  disabled={submitting}
                  type="button"
                  onClick={() => setModalOpen(false)}
                >
                  Cancel
                </button>
                <button
                  className="button-danger"
                  disabled={submitting}
                  type="button"
                  onClick={handleRefund}
                >
                  {submitting
                    ? "Refunding..."
                    : `Confirm refund ${formatGbpPennies(order.totalPence)}`}
                </button>
              </div>
            </div>
          </div>
        </div>
      ) : null}
    </section>
  );
}

function refundFeedback(status: PaymentRefundStatus | null) {
  if (status === "SUCCEEDED") {
    return "Payment refunded.";
  }

  if (status === "FAILED") {
    return "Refund failed. Check the refund section for details.";
  }

  if (status === "PENDING" || status === "REQUIRES_ACTION") {
    return "Refund request recorded. Stripe has not confirmed completion yet.";
  }

  return "Refund request recorded.";
}

function Detail({ label, value }: { label: string; value: string }) {
  return (
    <div>
      <dt>{label}</dt>
      <dd>{value}</dd>
    </div>
  );
}
