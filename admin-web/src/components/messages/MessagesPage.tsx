"use client";

import Link from "next/link";
import { useCallback, useEffect, useMemo, useState } from "react";
import {
  getAdminNotifications,
  retryNotification,
} from "@/lib/api/admin-messages-api";
import { getAdminApiErrorMessage } from "@/lib/api/api-error";
import { formatDateTime } from "@/lib/date-time";
import { formatStatus } from "@/lib/status";
import type {
  NotificationResponse,
  NotificationStatus,
  NotificationType,
  PageResponse,
} from "@/types/admin";
import { ErrorPanel } from "@/components/ui/ErrorPanel";
import { LoadingPanel } from "@/components/ui/LoadingPanel";
import { PageHeader } from "@/components/ui/PageHeader";
import { StatusBadge } from "@/components/ui/StatusBadge";

const notificationStatuses: NotificationStatus[] = [
  "PENDING",
  "SENDING",
  "SENT",
  "FAILED",
];

const notificationTypes: NotificationType[] = [
  "ORDER_RECEIVED",
  "ORDER_ACCEPTED",
  "ORDER_READY",
  "ORDER_CANCELLED",
  "ORDER_REFUNDED",
  "BOOKING_REQUEST_RECEIVED",
  "BOOKING_CONFIRMED",
  "BOOKING_DECLINED",
  "BOOKING_CANCELLED",
  "RESTAURANT_NEW_ORDER",
  "RESTAURANT_NEW_BOOKING",
  "MANUAL_MESSAGE",
];

export function MessagesPage() {
  const [data, setData] = useState<PageResponse<NotificationResponse> | null>(null);
  const [statusFilter, setStatusFilter] = useState<NotificationStatus | "">("");
  const [typeFilter, setTypeFilter] = useState<NotificationType | "">("");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [feedback, setFeedback] = useState("");

  const load = useCallback(async () => {
    setLoading(true);
    setError("");

    try {
      setData(
        await getAdminNotifications({
          size: 50,
          status: statusFilter || undefined,
          type: typeFilter || undefined,
        }),
      );
    } catch (caught) {
      setData(null);
      setError(getAdminApiErrorMessage(caught, "Could not load messages."));
    } finally {
      setLoading(false);
    }
  }, [statusFilter, typeFilter]);

  useEffect(() => {
    const timeoutId = window.setTimeout(() => {
      void load();
    }, 0);

    return () => window.clearTimeout(timeoutId);
  }, [load]);

  const notifications = useMemo(() => data?.content ?? [], [data]);
  const summary = useMemo(
    () => ({
      failed: notifications.filter((notification) => notification.status === "FAILED").length,
      pending: notifications.filter((notification) => notification.status === "PENDING").length,
      sent: notifications.filter((notification) => notification.status === "SENT").length,
    }),
    [notifications],
  );

  async function handleRetry(id: number) {
    setError("");
    setFeedback("");

    try {
      const response = await retryNotification(id);
      setFeedback(response.status === "SENT" ? "Email retry sent." : "Retry recorded.");
      await load();
    } catch (caught) {
      setError(getAdminApiErrorMessage(caught, "Could not retry this email."));
    }
  }

  return (
    <div>
      <PageHeader eyebrow="Customer communication" title="Messages.">
        <p>
          Transactional customer emails and manual messages sent from orders and
          bookings.
        </p>
      </PageHeader>

      <section className="metric-grid" aria-label="Message status summary">
        <div className="panel metric-card">
          <p className="metric-card__value">{summary.pending}</p>
          <p className="metric-card__label">Pending</p>
        </div>
        <div className="panel metric-card">
          <p className="metric-card__value">{summary.sent}</p>
          <p className="metric-card__label">Sent</p>
        </div>
        <div className="panel metric-card">
          <p className="metric-card__value">{summary.failed}</p>
          <p className="metric-card__label">Failed</p>
        </div>
      </section>

      <section className="panel mt-4">
        <div className="panel__body form-grid form-grid--two lg:grid-cols-3">
          <label className="field">
            <span>Status</span>
            <select
              value={statusFilter}
              onChange={(event) => setStatusFilter(event.target.value as NotificationStatus | "")}
            >
              <option value="">All</option>
              {notificationStatuses.map((status) => (
                <option key={status} value={status}>
                  {formatStatus(status)}
                </option>
              ))}
            </select>
          </label>
          <label className="field">
            <span>Type</span>
            <select
              value={typeFilter}
              onChange={(event) => setTypeFilter(event.target.value as NotificationType | "")}
            >
              <option value="">All</option>
              {notificationTypes.map((type) => (
                <option key={type} value={type}>
                  {formatStatus(type)}
                </option>
              ))}
            </select>
          </label>
          <div className="flex items-end">
            <button className="button-secondary w-full" type="button" onClick={load}>
              Refresh
            </button>
          </div>
        </div>
      </section>

      <div aria-live="polite" className="mt-4">
        {feedback ? <p className="feedback">{feedback}</p> : null}
        {error ? <p className="error">{error}</p> : null}
      </div>

      {loading ? <LoadingPanel label="Loading messages..." /> : null}
      {!loading && error && !data ? <ErrorPanel title="Could not load messages." /> : null}

      {!loading && data ? (
        <section className="panel mt-4">
          <div className="table-wrap">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Date</th>
                  <th>Customer</th>
                  <th>Type</th>
                  <th>Related</th>
                  <th>Recipient</th>
                  <th>Status</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {notifications.map((notification) => (
                  <tr key={notification.id}>
                    <td>{formatDateTime(notification.createdAt)}</td>
                    <td>{notification.recipientName ?? "Customer"}</td>
                    <td>{formatStatus(notification.notificationType)}</td>
                    <td>
                      {notification.orderId ? (
                        <Link href={`/orders/${notification.orderId}`}>
                          {notification.orderReference}
                        </Link>
                      ) : null}
                      {notification.bookingId ? (
                        <Link href={`/bookings/${notification.bookingId}`}>
                          {notification.bookingReference}
                        </Link>
                      ) : null}
                    </td>
                    <td>{notification.recipientEmail}</td>
                    <td>
                      <StatusBadge value={notification.status} />
                    </td>
                    <td>
                      <div className="action-row">
                        <Link className="button-ghost" href={`/messages/${notification.id}`}>
                          View
                        </Link>
                        {notification.status === "FAILED" ? (
                          <button
                            className="button-secondary"
                            type="button"
                            onClick={() => handleRetry(notification.id)}
                          >
                            Retry
                          </button>
                        ) : null}
                      </div>
                    </td>
                  </tr>
                ))}
                {notifications.length === 0 ? (
                  <tr>
                    <td colSpan={7}>No messages match these filters.</td>
                  </tr>
                ) : null}
              </tbody>
            </table>
          </div>
        </section>
      ) : null}
    </div>
  );
}
