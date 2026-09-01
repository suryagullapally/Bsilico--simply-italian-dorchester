"use client";

import Link from "next/link";
import { useCallback, useEffect, useState } from "react";
import { getAdminNotification, retryNotification } from "@/lib/api/admin-messages-api";
import { getAdminApiErrorMessage } from "@/lib/api/api-error";
import { formatDateTime } from "@/lib/date-time";
import { formatStatus } from "@/lib/status";
import type { NotificationResponse } from "@/types/admin";
import { ErrorPanel } from "@/components/ui/ErrorPanel";
import { LoadingPanel } from "@/components/ui/LoadingPanel";
import { StatusBadge } from "@/components/ui/StatusBadge";

export function MessageDetailPage({ id }: { id: string }) {
  const [notification, setNotification] = useState<NotificationResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [notFound, setNotFound] = useState(false);
  const [feedback, setFeedback] = useState("");
  const [error, setError] = useState("");

  const load = useCallback(async () => {
    setLoading(true);
    setError("");
    setNotFound(false);

    try {
      setNotification(await getAdminNotification(id));
    } catch (caught) {
      setNotification(null);
      setNotFound((caught as { status?: number }).status === 404);
      if ((caught as { status?: number }).status !== 404) {
        setError(getAdminApiErrorMessage(caught, "Could not load this message."));
      }
    } finally {
      setLoading(false);
    }
  }, [id]);

  useEffect(() => {
    const timeoutId = window.setTimeout(() => {
      void load();
    }, 0);

    return () => window.clearTimeout(timeoutId);
  }, [load]);

  async function handleRetry() {
    if (!notification) {
      return;
    }

    setError("");
    setFeedback("");

    try {
      const response = await retryNotification(notification.id);
      setNotification(response);
      setFeedback(response.status === "SENT" ? "Email retry sent." : "Retry recorded.");
    } catch (caught) {
      setError(getAdminApiErrorMessage(caught, "Could not retry this email."));
    }
  }

  if (loading) {
    return <LoadingPanel label="Loading message..." />;
  }

  if (notFound) {
    return <ErrorPanel title="Message not found." />;
  }

  if (!notification) {
    return <ErrorPanel title={error || "Could not load this message."} />;
  }

  return (
    <div>
      <Link className="button-ghost mb-5" href="/messages">
        ← Back to messages
      </Link>

      <div className="page-header">
        <div>
          <p className="eyebrow">Message detail</p>
          <h1 className="page-title">{notification.subject}</h1>
          <p className="mt-3 text-muted">
            {formatStatus(notification.notificationType)} · {formatDateTime(notification.createdAt)}
          </p>
        </div>
        <StatusBadge value={notification.status} />
      </div>

      <div className="split-layout">
        <div className="grid gap-4">
          <section className="panel">
            <div className="panel__body">
              <p className="eyebrow">Recipient</p>
              <dl className="detail-list mt-4">
                <Detail label="Name" value={notification.recipientName ?? "Customer"} />
                <Detail label="Email" value={notification.recipientEmail} />
                <Detail label="Channel" value={notification.channel} />
                <Detail label="Attempts" value={String(notification.attemptCount)} />
              </dl>
            </div>
          </section>

          <section className="panel">
            <div className="panel__body">
              <p className="eyebrow">Message body</p>
              <pre className="message-body mt-4">{notification.bodyText}</pre>
            </div>
          </section>

          {notification.lastError ? (
            <section className="panel">
              <div className="panel__body">
                <p className="eyebrow">Delivery error</p>
                <p className="mt-3 error">{notification.lastError}</p>
              </div>
            </section>
          ) : null}
        </div>

        <aside className="sticky-column">
          <section className="panel">
            <div className="panel__body">
              <p className="eyebrow">Related record</p>
              <div className="mt-4 grid gap-3">
                {notification.orderId ? (
                  <Link className="button-secondary" href={`/orders/${notification.orderId}`}>
                    Open order {notification.orderReference}
                  </Link>
                ) : null}
                {notification.bookingId ? (
                  <Link className="button-secondary" href={`/bookings/${notification.bookingId}`}>
                    Open booking {notification.bookingReference}
                  </Link>
                ) : null}
                {notification.status === "FAILED" ? (
                  <button className="button" type="button" onClick={handleRetry}>
                    Retry email
                  </button>
                ) : null}
              </div>
              <div aria-live="polite" className="mt-4">
                {feedback ? <p className="feedback">{feedback}</p> : null}
                {error ? <p className="error">{error}</p> : null}
              </div>
            </div>
          </section>
        </aside>
      </div>
    </div>
  );
}

function Detail({ label, value }: { label: string; value: string }) {
  return (
    <div>
      <dt>{label}</dt>
      <dd>{value}</dd>
    </div>
  );
}
