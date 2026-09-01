import Link from "next/link";
import { CommunicationPanel } from "@/components/messages/CommunicationPanel";
import { OrderStatusActions } from "@/components/orders/OrderStatusActions";
import { StatusBadge } from "@/components/ui/StatusBadge";
import { formatDate, formatDateTime, formatTime } from "@/lib/date-time";
import { formatGbpPennies } from "@/lib/format-price";
import type { AdminOrderResponse } from "@/types/admin";

export function OrderDetailPage({
  order,
  onOrderUpdated,
}: {
  order: AdminOrderResponse;
  onOrderUpdated?: (order: AdminOrderResponse) => void;
}) {
  return (
    <div>
      <Link className="button-ghost mb-5" href="/orders">
        ← Back to orders
      </Link>

      <div className="page-header">
        <div>
          <p className="eyebrow">Order detail</p>
          <h1 className="page-title">{order.orderReference}</h1>
          <p className="mt-3 text-muted">Created {formatDateTime(order.createdAt)}</p>
        </div>
      </div>

      <div className="split-layout">
        <div className="grid gap-4">
          <section className="panel">
            <div className="panel__body">
              <div className="flex flex-wrap items-start justify-between gap-3">
                <div>
                  <p className="eyebrow">Order state</p>
                  <div className="mt-3 flex flex-wrap gap-2">
                    <StatusBadge value={order.status} />
                    <StatusBadge value={order.paymentStatus} />
                    <StatusBadge value={order.fulfilmentType} />
                  </div>
                </div>
                <p className="text-3xl font-black">
                  {formatGbpPennies(order.totalPence)}
                </p>
              </div>
            </div>
          </section>

          <section className="panel">
            <div className="panel__body">
              <p className="eyebrow">Order total</p>
              <dl className="detail-list mt-4">
                <Detail
                  label="Food subtotal"
                  value={formatGbpPennies(order.subtotalPence)}
                />
                <Detail
                  label="Delivery"
                  value={
                    order.fulfilmentType === "DELIVERY"
                      ? formatGbpPennies(order.deliveryFeePence)
                      : "Collection"
                  }
                />
                {order.fulfilmentType === "DELIVERY" ? (
                  <>
                    <Detail
                      label="Distance"
                      value={formatOptionalMiles(order.deliveryDistanceMiles)}
                    />
                    <Detail
                      label="Preparation"
                      value={formatOptionalMinutes(order.deliveryPreparationMinutes)}
                    />
                    <Detail
                      label="Estimated drive"
                      value={formatOptionalMinutes(order.deliveryTravelMinutes)}
                    />
                    <Detail
                      label="Estimated delivery"
                      value={
                        order.estimatedDeliveryMinutes === null
                          ? "Not available"
                          : `Approx. ${order.estimatedDeliveryMinutes} mins`
                      }
                    />
                  </>
                ) : null}
                <Detail label="Total" value={formatGbpPennies(order.totalPence)} />
              </dl>
            </div>
          </section>

          <section className="panel">
            <div className="panel__body">
              <p className="eyebrow">Items</p>
              <div className="mt-4 grid gap-4">
                {order.items.map((item, index) => (
                  <article className="border-b border-[var(--border-subtle)] pb-4 last:border-0 last:pb-0" key={`${item.productName}-${index}`}>
                    <div className="grid gap-2 sm:grid-cols-[minmax(0,1fr)_auto]">
                      <div>
                        <h2 className="text-xl font-black">{item.productName}</h2>
                        <p className="text-sm text-muted">
                          {item.quantity} × {formatGbpPennies(item.unitPricePence)}
                        </p>
                        {item.toppings.length > 0 ? (
                          <ul className="mt-3 grid gap-1 text-sm text-muted">
                            {item.toppings.map((topping) => (
                              <li key={topping.name}>
                                + {topping.name} · {formatGbpPennies(topping.pricePence)}
                              </li>
                            ))}
                          </ul>
                        ) : null}
                      </div>
                      <p className="text-lg font-black">
                        {formatGbpPennies(item.lineTotalPence)}
                      </p>
                    </div>
                  </article>
                ))}
              </div>
            </div>
          </section>

          <section className="panel">
            <div className="panel__body">
              <p className="eyebrow">Stripe payment</p>
              {order.paymentAttempts.length > 0 ? (
                <div className="mt-4 grid gap-3">
                  {order.paymentAttempts.map((attempt) => (
                    <article
                      className="border-b border-[var(--border-subtle)] pb-3 last:border-0 last:pb-0"
                      key={attempt.id}
                    >
                      <div className="grid gap-2 sm:grid-cols-[minmax(0,1fr)_auto]">
                        <div>
                          <div className="flex flex-wrap gap-2">
                            <StatusBadge value={attempt.provider} />
                            <StatusBadge value={attempt.status} />
                          </div>
                          <p className="mt-2 text-sm text-muted">
                            Session {attempt.stripeCheckoutSessionId ?? "not created"}
                          </p>
                          {attempt.stripePaymentIntentId ? (
                            <p className="mt-1 text-sm text-muted">
                              Payment intent {attempt.stripePaymentIntentId}
                            </p>
                          ) : null}
                        </div>
                        <p className="text-lg font-black">
                          {formatGbpPennies(attempt.amountPence)}
                        </p>
                      </div>
                    </article>
                  ))}
                </div>
              ) : (
                <p className="mt-3 text-muted">
                  No Stripe payment session has been created for this order yet.
                </p>
              )}
            </div>
          </section>

          <section className="panel">
            <div className="panel__body">
              <p className="eyebrow">Customer</p>
              <div className="mt-4 grid gap-3 sm:grid-cols-2">
                <Detail label="Name" value={`${order.customer.firstName} ${order.customer.lastName}`} />
                <Detail label="Phone" value={order.customer.phone} />
                <Detail label="Email" value={order.customer.email} />
                <Detail label="Fulfilment" value={order.fulfilmentType} />
              </div>
              {order.deliveryAddress ? (
                <div className="mt-5">
                  <p className="field-label">Delivery address</p>
                  <p className="mt-2 text-muted">
                    {order.deliveryAddress.line1}
                    {order.deliveryAddress.line2 ? (
                      <>
                        <br />
                        {order.deliveryAddress.line2}
                      </>
                    ) : null}
                    <br />
                    {order.deliveryAddress.city}
                    <br />
                    {order.deliveryAddress.postcode}
                  </p>
                </div>
              ) : null}
            </div>
          </section>

          <section className="panel">
            <div className="panel__body">
              <p className="eyebrow">Timing and notes</p>
              <div className="mt-4 grid gap-3 sm:grid-cols-2">
                <Detail
                  label="Requested time"
                  value={
                    order.timing.type === "ASAP"
                      ? "ASAP"
                      : `${formatDate(order.timing.requestedDate ?? "")} · ${formatTime(order.timing.requestedTime)}`
                  }
                />
                <Detail label="Notes" value={order.notes || "No notes"} />
              </div>
            </div>
          </section>

          <CommunicationPanel
            context="order"
            contextId={order.id}
            recipientEmail={order.customer.email}
            recipientName={`${order.customer.firstName} ${order.customer.lastName}`}
          />
        </div>

        <aside className="sticky-column">
          <OrderStatusActions
            currentPaymentStatus={order.paymentStatus}
            currentStatus={order.status}
            orderId={order.id}
            onOrderUpdated={onOrderUpdated}
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

function formatOptionalMiles(value: number | null) {
  return value === null ? "Not available" : `${value.toFixed(1)} miles`;
}

function formatOptionalMinutes(value: number | null) {
  return value === null ? "Not available" : `${value} mins`;
}
