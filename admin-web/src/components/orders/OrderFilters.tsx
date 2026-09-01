import { fulfilmentTypes, formatStatus, orderStatuses, paymentStatuses } from "@/lib/status";
import type { FulfilmentType, OrderStatus, PaymentStatus } from "@/types/admin";

export function OrderFilters({
  status,
  paymentStatus,
  fulfilmentType,
}: {
  status?: OrderStatus;
  paymentStatus?: PaymentStatus;
  fulfilmentType?: FulfilmentType;
}) {
  return (
    <form action="/orders" className="panel">
      <div className="panel__body form-grid form-grid--two lg:grid-cols-4">
        <div className="field">
          <label htmlFor="status">Order status</label>
          <select defaultValue={status ?? ""} id="status" name="status">
            <option value="">All</option>
            {orderStatuses.map((value) => (
              <option key={value} value={value}>
                {formatStatus(value)}
              </option>
            ))}
          </select>
        </div>
        <div className="field">
          <label htmlFor="paymentStatus">Payment</label>
          <select
            defaultValue={paymentStatus ?? ""}
            id="paymentStatus"
            name="paymentStatus"
          >
            <option value="">All</option>
            {paymentStatuses.map((value) => (
              <option key={value} value={value}>
                {formatStatus(value)}
              </option>
            ))}
          </select>
        </div>
        <div className="field">
          <label htmlFor="fulfilmentType">Fulfilment</label>
          <select
            defaultValue={fulfilmentType ?? ""}
            id="fulfilmentType"
            name="fulfilmentType"
          >
            <option value="">All</option>
            {fulfilmentTypes.map((value) => (
              <option key={value} value={value}>
                {formatStatus(value)}
              </option>
            ))}
          </select>
        </div>
        <div className="flex items-end">
          <button className="button-secondary w-full" type="submit">
            Apply filters
          </button>
        </div>
      </div>
    </form>
  );
}
