import Link from "next/link";
import { ErrorPanel } from "@/components/ui/ErrorPanel";
import { PageHeader } from "@/components/ui/PageHeader";
import { StatusBadge } from "@/components/ui/StatusBadge";
import { formatDate, formatDateTime, formatTime, getLondonDateKey, todayLondonKey } from "@/lib/date-time";
import { formatGbpPennies } from "@/lib/format-price";
import type {
  AdminBookingSummaryResponse,
  AdminOrderSummaryResponse,
  MenuItemResponse,
} from "@/types/admin";

type DashboardPageProps = {
  orders?: AdminOrderSummaryResponse[];
  bookings?: AdminBookingSummaryResponse[];
  menuItems?: MenuItemResponse[];
  failedNotificationCount?: number;
};

export function DashboardPage({
  orders,
  bookings,
  menuItems,
  failedNotificationCount = 0,
}: DashboardPageProps) {
  if (!orders || !bookings || !menuItems) {
    return (
      <>
        <PageHeader eyebrow="Operations" title="Dashboard." />
        <ErrorPanel title="Could not load dashboard data." />
      </>
    );
  }

  const today = todayLondonKey();
  const todayOrders = orders.filter(
    (order) => getLondonDateKey(order.createdAt) === today,
  );
  const todayBookings = bookings.filter((booking) => booking.date === today);
  const paidOrders = orders.filter((order) => order.paymentStatus === "PAID");
  const requestedBookings = bookings.filter(
    (booking) => booking.status === "REQUESTED",
  );

  const metrics = [
    { label: "Today's orders", value: todayOrders.length },
    {
      label: "Needs attention",
      value: orders.filter(
        (order) => order.status === "NEW" || order.status === "PENDING_PAYMENT",
      ).length,
    },
    {
      label: "Preparing",
      value: orders.filter((order) => order.status === "PREPARING").length,
    },
    { label: "Ready", value: orders.filter((order) => order.status === "READY").length },
    { label: "Today's bookings", value: todayBookings.length },
    { label: "Booking requests", value: requestedBookings.length },
  ];

  return (
    <div>
      <PageHeader eyebrow="Operations" title="Dashboard.">
        <p>
          Quick view of orders, bookings, payment state and menu availability for
          Basilico.
        </p>
      </PageHeader>

      <section className="metric-grid" aria-label="Operational summary">
        {metrics.map((metric) => (
          <div className="panel metric-card" key={metric.label}>
            <p className="metric-card__value">{metric.value}</p>
            <p className="metric-card__label">{metric.label}</p>
          </div>
        ))}
      </section>

      <div className="mt-4 grid gap-4 xl:grid-cols-[1.4fr_0.8fr]">
        <section className="panel">
          <div className="panel__body">
            <div className="flex items-end justify-between gap-4">
              <div>
                <p className="eyebrow">Recent orders</p>
                <h2 className="section-title mt-1">What just arrived?</h2>
              </div>
              <Link className="button-ghost" href="/orders">
                View all
              </Link>
            </div>
            <div className="table-wrap mt-4">
              <table className="data-table">
                <thead>
                  <tr>
                    <th>Reference</th>
                    <th>Time</th>
                    <th>Fulfilment</th>
                    <th>Customer</th>
                    <th>Amount</th>
                    <th>Payment</th>
                    <th>Status</th>
                  </tr>
                </thead>
                <tbody>
                  {orders.slice(0, 8).map((order) => (
                    <tr key={order.id}>
                      <td>
                        <Link href={`/orders/${order.id}`}>{order.orderReference}</Link>
                      </td>
                      <td>{formatDateTime(order.createdAt)}</td>
                      <td>{order.fulfilmentType}</td>
                      <td>{order.customerName}</td>
                      <td>{formatGbpPennies(order.totalPence)}</td>
                      <td>
                        <StatusBadge value={order.paymentStatus} />
                      </td>
                      <td>
                        <StatusBadge value={order.status} />
                      </td>
                    </tr>
                  ))}
                  {orders.length === 0 ? (
                    <tr>
                      <td colSpan={7}>No orders yet.</td>
                    </tr>
                  ) : null}
                </tbody>
              </table>
            </div>
          </div>
        </section>

        <div className="grid gap-4">
          <section className="panel panel--cream">
            <div className="panel__body">
              <p className="eyebrow">Paid order value</p>
              <p className="metric-card__value mt-2">
                {formatGbpPennies(
                  paidOrders.reduce((total, order) => total + order.totalPence, 0),
                )}
              </p>
              <p className="mt-3 text-sm">
                Only orders marked PAID are counted here. UNPAID orders are not
                treated as sales.
              </p>
            </div>
          </section>

          {failedNotificationCount > 0 ? (
            <section className="panel">
              <div className="panel__body">
                <p className="eyebrow">Messages</p>
                <h2 className="section-title mt-1">Email needs attention.</h2>
                <p className="metric-card__value mt-2">{failedNotificationCount}</p>
                <p className="mt-3 text-sm text-muted">
                  Failed customer emails are recorded for review and retry.
                </p>
                <Link className="button-secondary mt-4" href="/messages">
                  Review messages
                </Link>
              </div>
            </section>
          ) : null}

          <section className="panel">
            <div className="panel__body">
              <div className="flex items-end justify-between gap-4">
                <div>
                  <p className="eyebrow">Bookings</p>
                  <h2 className="section-title mt-1">Requests next.</h2>
                </div>
                <Link className="button-ghost" href="/bookings">
                  View all
                </Link>
              </div>
              <div className="card-list mt-4">
                {bookings.slice(0, 6).map((booking) => (
                  <Link
                    className="panel mobile-card no-underline"
                    href={`/bookings/${booking.id}`}
                    key={booking.id}
                  >
                    <div className="flex items-start justify-between gap-3">
                      <div>
                        <p className="font-black">{booking.customerName}</p>
                        <p className="text-sm text-muted">
                          {formatDate(booking.date)} · {formatTime(booking.time)}
                        </p>
                        <p className="text-sm text-muted">{booking.partySize} guests</p>
                      </div>
                      <StatusBadge value={booking.status} />
                    </div>
                  </Link>
                ))}
                {bookings.length === 0 ? <p className="text-muted">No bookings yet.</p> : null}
              </div>
            </div>
          </section>

          <section className="panel">
            <div className="panel__body">
              <p className="eyebrow">Menu watch</p>
              <h2 className="section-title mt-1">Sold out now.</h2>
              <div className="mt-4 grid gap-2">
                {menuItems
                  .filter((item) => item.active && !item.available)
                  .slice(0, 6)
                  .map((item) => (
                    <Link className="text-sm font-extrabold" href={`/menu/${item.id}/edit`} key={item.id}>
                      {item.name}
                    </Link>
                  ))}
                {menuItems.filter((item) => item.active && !item.available).length === 0 ? (
                  <p className="text-muted">Nothing is currently marked sold out.</p>
                ) : null}
              </div>
            </div>
          </section>
        </div>
      </div>
    </div>
  );
}
