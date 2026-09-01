import Link from "next/link";
import { BookingStatusActions } from "@/components/bookings/BookingStatusActions";
import { CommunicationPanel } from "@/components/messages/CommunicationPanel";
import { StatusBadge } from "@/components/ui/StatusBadge";
import { formatDate, formatDateTime, formatTime } from "@/lib/date-time";
import type { AdminBookingResponse } from "@/types/admin";

export function BookingDetailPage({
  booking,
  onBookingUpdated,
}: {
  booking: AdminBookingResponse;
  onBookingUpdated?: (booking: AdminBookingResponse) => void;
}) {
  return (
    <div>
      <Link className="button-ghost mb-5" href="/bookings">
        ← Back to bookings
      </Link>

      <div className="page-header">
        <div>
          <p className="eyebrow">Booking detail</p>
          <h1 className="page-title">{booking.bookingReference}</h1>
          <p className="mt-3 text-muted">Requested {formatDateTime(booking.createdAt)}</p>
        </div>
        <StatusBadge value={booking.status} />
      </div>

      <div className="split-layout">
        <div className="grid gap-4">
          <section className="panel panel--cream">
            <div className="panel__body">
              <p className="eyebrow">Table request</p>
              <div className="mt-4 grid gap-4 sm:grid-cols-3">
                <Detail label="Date" value={formatDate(booking.date)} />
                <Detail label="Time" value={formatTime(booking.time)} />
                <Detail
                  label="Party size"
                  value={`${booking.partySize} ${booking.partySize === 1 ? "guest" : "guests"}`}
                />
              </div>
            </div>
          </section>

          <section className="panel">
            <div className="panel__body">
              <p className="eyebrow">Customer</p>
              <div className="mt-4 grid gap-4 sm:grid-cols-2">
                <Detail label="Name" value={`${booking.firstName} ${booking.lastName}`} />
                <Detail label="Phone" value={booking.phone} />
                <Detail label="Email" value={booking.email} />
                <Detail label="Status" value={booking.status} />
              </div>
            </div>
          </section>

          <section className="panel">
            <div className="panel__body">
              <p className="eyebrow">Special requests</p>
              <p className="mt-3 text-muted">
                {booking.specialRequests || "No special requests."}
              </p>
            </div>
          </section>

          <CommunicationPanel
            context="booking"
            contextId={booking.id}
            recipientEmail={booking.email}
            recipientName={`${booking.firstName} ${booking.lastName}`}
          />
        </div>

        <aside className="sticky-column">
          <BookingStatusActions
            bookingId={booking.id}
            currentStatus={booking.status}
            onBookingUpdated={onBookingUpdated}
          />
        </aside>
      </div>
    </div>
  );
}

function Detail({ label, value }: { label: string; value: string }) {
  return (
    <div>
      <p className="field-label">{label}</p>
      <p className="mt-1 text-muted">{value}</p>
    </div>
  );
}
