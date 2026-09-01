"use client";

import { useRouter } from "next/navigation";
import { useState, useTransition } from "react";
import { updateBookingStatus } from "@/lib/api/admin-bookings-api";
import { getAdminApiErrorMessage } from "@/lib/api/api-error";
import { formatStatus } from "@/lib/status";
import type { AdminBookingResponse, BookingStatus } from "@/types/admin";

export function BookingStatusActions({
  bookingId,
  currentStatus,
  onBookingUpdated,
}: {
  bookingId: number;
  currentStatus: BookingStatus;
  onBookingUpdated?: (booking: AdminBookingResponse) => void;
}) {
  const router = useRouter();
  const [pending, startTransition] = useTransition();
  const [feedback, setFeedback] = useState("");
  const [error, setError] = useState("");
  const actions = getActions(currentStatus);

  async function setStatus(status: BookingStatus) {
    setError("");
    setFeedback("");

    try {
      const updated = await updateBookingStatus(bookingId, status);
      setFeedback(`Booking marked as ${formatStatus(status).toLowerCase()}.`);
      onBookingUpdated?.(updated);
      startTransition(() => router.refresh());
    } catch (caught) {
      setError(getAdminApiErrorMessage(caught, "Could not update the booking."));
    }
  }

  return (
    <section className="panel">
      <div className="panel__body">
        <p className="eyebrow">Booking actions</p>
        <div className="action-row mt-4">
          {actions.map((action) => (
            <button
              className={action.tone === "danger" ? "button-danger" : "button-secondary"}
              disabled={pending}
              key={action.status}
              type="button"
              onClick={() => setStatus(action.status)}
            >
              {action.label}
            </button>
          ))}
          {actions.length === 0 ? (
            <p className="text-muted">No immediate action is suggested for this status.</p>
          ) : null}
        </div>
        <p className="mt-4 text-sm text-muted">
          Confirmation, decline and cancellation changes queue customer emails.
          SMS is not connected yet.
        </p>
        <div aria-live="polite" className="mt-4">
          {feedback ? <p className="feedback">{feedback}</p> : null}
          {error ? <p className="error">{error}</p> : null}
        </div>
      </div>
    </section>
  );
}

function getActions(status: BookingStatus) {
  switch (status) {
    case "REQUESTED":
      return [
        { label: "Confirm table", status: "CONFIRMED" as const },
        { label: "Decline", status: "DECLINED" as const, tone: "danger" as const },
      ];
    case "CONFIRMED":
      return [
        { label: "Mark completed", status: "COMPLETED" as const },
        { label: "Mark no show", status: "NO_SHOW" as const, tone: "danger" as const },
        { label: "Cancel booking", status: "CANCELLED" as const, tone: "danger" as const },
      ];
    case "DECLINED":
    case "CANCELLED":
    case "COMPLETED":
    case "NO_SHOW":
      return [];
  }
}
