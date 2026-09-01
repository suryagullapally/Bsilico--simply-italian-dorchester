import Link from "next/link";
import { StatusBadge } from "@/components/ui/StatusBadge";
import { formatDate, formatTime } from "@/lib/date-time";
import type { AdminBookingSummaryResponse } from "@/types/admin";

export function BookingsTable({
  bookings,
}: {
  bookings: AdminBookingSummaryResponse[];
}) {
  if (bookings.length === 0) {
    return (
      <div className="panel">
        <div className="panel__body">
          <p className="text-muted">No bookings match these filters.</p>
        </div>
      </div>
    );
  }

  return (
    <div className="panel">
      <div className="table-wrap">
        <table className="data-table">
          <thead>
            <tr>
              <th>Reference</th>
              <th>Date</th>
              <th>Time</th>
              <th>Customer</th>
              <th>Party</th>
              <th>Status</th>
            </tr>
          </thead>
          <tbody>
            {bookings.map((booking) => (
              <tr key={booking.id}>
                <td>
                  <Link href={`/bookings/${booking.id}`}>
                    {booking.bookingReference}
                  </Link>
                </td>
                <td>{formatDate(booking.date)}</td>
                <td>{formatTime(booking.time)}</td>
                <td>{booking.customerName}</td>
                <td>{booking.partySize}</td>
                <td>
                  <StatusBadge value={booking.status} />
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
