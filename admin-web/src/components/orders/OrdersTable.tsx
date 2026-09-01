import Link from "next/link";
import { StatusBadge } from "@/components/ui/StatusBadge";
import { formatDateTime } from "@/lib/date-time";
import { formatGbpPennies } from "@/lib/format-price";
import type { AdminOrderSummaryResponse } from "@/types/admin";

export function OrdersTable({ orders }: { orders: AdminOrderSummaryResponse[] }) {
  if (orders.length === 0) {
    return (
      <div className="panel">
        <div className="panel__body">
          <p className="text-muted">No orders match these filters.</p>
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
              <th>Created</th>
              <th>Fulfilment</th>
              <th>Customer</th>
              <th>Total</th>
              <th>Payment</th>
              <th>Status</th>
            </tr>
          </thead>
          <tbody>
            {orders.map((order) => (
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
          </tbody>
        </table>
      </div>
    </div>
  );
}
