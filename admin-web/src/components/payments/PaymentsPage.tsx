import Link from "next/link";
import { StatusBadge } from "@/components/ui/StatusBadge";
import { formatDateTime } from "@/lib/date-time";
import { formatGbpPennies } from "@/lib/format-price";
import { formatStatus, paymentStatuses } from "@/lib/status";
import type { AdminOrderSummaryResponse, PaymentStatus } from "@/types/admin";

export function PaymentsPage({
  orders,
  allOrders,
  paymentStatus,
}: {
  orders: AdminOrderSummaryResponse[];
  allOrders: AdminOrderSummaryResponse[];
  paymentStatus?: PaymentStatus;
}) {
  const paidOrders = allOrders.filter((order) => order.paymentStatus === "PAID");

  return (
    <div className="page-stack">
      <div className="notice">
        Stripe test-mode payments update order payment status through verified
        webhooks. Manual payment controls are hidden unless local development
        explicitly enables them.
      </div>

      <section className="metric-grid" aria-label="Payment summary">
        <Metric label="Paid orders" value={paidOrders.length.toString()} />
        <Metric
          label="Paid value"
          value={formatGbpPennies(
            paidOrders.reduce((total, order) => total + order.totalPence, 0),
          )}
        />
        <Metric
          label="Unpaid count"
          value={countPaymentStatus(allOrders, "UNPAID").toString()}
        />
        <Metric
          label="Failed count"
          value={countPaymentStatus(allOrders, "FAILED").toString()}
        />
        <Metric
          label="Refunded count"
          value={countPaymentStatus(allOrders, "REFUNDED").toString()}
        />
      </section>

      <form action="/payments" className="panel">
        <div className="panel__body form-grid form-grid--two lg:grid-cols-3">
          <div className="field">
            <label htmlFor="paymentStatus">Payment status</label>
            <select
              defaultValue={paymentStatus ?? ""}
              id="paymentStatus"
              name="paymentStatus"
            >
              <option value="">All</option>
              {paymentStatuses.map((status) => (
                <option key={status} value={status}>
                  {formatStatus(status)}
                </option>
              ))}
            </select>
          </div>
          <div className="flex items-end">
            <button className="button-secondary w-full" type="submit">
              Apply filter
            </button>
          </div>
        </div>
      </form>

      <div className="panel">
        <div className="table-wrap">
          <table className="data-table">
            <thead>
              <tr>
                <th>Order</th>
                <th>Customer</th>
                <th>Fulfilment</th>
                <th>Amount</th>
                <th>Payment</th>
                <th>Order status</th>
                <th>Created</th>
              </tr>
            </thead>
            <tbody>
              {orders.map((order) => (
                <tr key={order.id}>
                  <td>
                    <Link href={`/orders/${order.id}`}>{order.orderReference}</Link>
                  </td>
                  <td>{order.customerName}</td>
                  <td>{order.fulfilmentType}</td>
                  <td>{formatGbpPennies(order.totalPence)}</td>
                  <td>
                    <StatusBadge value={order.paymentStatus} />
                  </td>
                  <td>
                    <StatusBadge value={order.status} />
                  </td>
                  <td>{formatDateTime(order.createdAt)}</td>
                </tr>
              ))}
              {orders.length === 0 ? (
                <tr>
                  <td colSpan={7}>No orders match this payment filter.</td>
                </tr>
              ) : null}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}

function Metric({ label, value }: { label: string; value: string }) {
  return (
    <div className="panel metric-card">
      <p className="metric-card__value">{value}</p>
      <p className="metric-card__label">{label}</p>
    </div>
  );
}

function countPaymentStatus(
  orders: AdminOrderSummaryResponse[],
  status: PaymentStatus,
) {
  return orders.filter((order) => order.paymentStatus === status).length;
}
