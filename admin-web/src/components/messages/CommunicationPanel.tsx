"use client";

import Link from "next/link";
import { useCallback, useEffect, useMemo, useState, type FormEvent } from "react";
import {
  getAdminNotifications,
  retryNotification,
  sendManualEmail,
} from "@/lib/api/admin-messages-api";
import { getAdminApiErrorMessage } from "@/lib/api/api-error";
import { formatDateTime } from "@/lib/date-time";
import { formatStatus } from "@/lib/status";
import type { NotificationResponse } from "@/types/admin";
import { StatusBadge } from "@/components/ui/StatusBadge";

type CommunicationPanelProps = {
  context: "order" | "booking";
  contextId: number;
  recipientEmail: string;
  recipientName: string;
};

export function CommunicationPanel({
  context,
  contextId,
  recipientEmail,
  recipientName,
}: CommunicationPanelProps) {
  const [notifications, setNotifications] = useState<NotificationResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const [subject, setSubject] = useState("");
  const [message, setMessage] = useState("");
  const [sending, setSending] = useState(false);
  const [feedback, setFeedback] = useState("");
  const [error, setError] = useState("");

  const load = useCallback(async () => {
    setLoading(true);

    try {
      const response = await getAdminNotifications({
        bookingId: context === "booking" ? contextId : undefined,
        orderId: context === "order" ? contextId : undefined,
        size: 8,
      });
      setNotifications(response.content);
    } catch {
      setNotifications([]);
    } finally {
      setLoading(false);
    }
  }, [context, contextId]);

  useEffect(() => {
    const timeoutId = window.setTimeout(() => {
      void load();
    }, 0);

    return () => window.clearTimeout(timeoutId);
  }, [load]);

  const defaultSubject = useMemo(
    () =>
      context === "order"
        ? "A message about your Basilico order"
        : "A message about your Basilico booking",
    [context],
  );

  async function handleSend(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError("");
    setFeedback("");

    const trimmedSubject = subject.trim() || defaultSubject;
    const trimmedMessage = message.trim();

    if (!trimmedSubject || !trimmedMessage) {
      setError("Add a subject and message before sending.");
      return;
    }

    setSending(true);

    try {
      const response = await sendManualEmail({
        bookingId: context === "booking" ? contextId : undefined,
        orderId: context === "order" ? contextId : undefined,
        subject: trimmedSubject,
        message: trimmedMessage,
      });

      setSubject("");
      setMessage("");
      setShowForm(false);
      setFeedback(
        response.status === "SENT"
          ? "Email sent to the customer."
          : "Email was recorded but could not be sent. Check Messages for details.",
      );
      await load();
    } catch (caught) {
      setError(getAdminApiErrorMessage(caught, "Email could not be sent."));
    } finally {
      setSending(false);
    }
  }

  async function handleRetry(notificationId: number) {
    setError("");
    setFeedback("");

    try {
      const response = await retryNotification(notificationId);
      setFeedback(response.status === "SENT" ? "Email retry sent." : "Retry recorded.");
      await load();
    } catch (caught) {
      setError(getAdminApiErrorMessage(caught, "Could not retry this email."));
    }
  }

  return (
    <section className="panel">
      <div className="panel__body">
        <div className="flex flex-wrap items-start justify-between gap-3">
          <div>
            <p className="eyebrow">Communication</p>
            <h2 className="section-title mt-1">Customer email.</h2>
            <p className="mt-2 text-sm text-muted">
              Sending to {recipientName} at {recipientEmail}
            </p>
          </div>
          <button
            className="button-secondary"
            type="button"
            onClick={() => {
              setShowForm((current) => !current);
              setError("");
              setFeedback("");
            }}
          >
            {showForm ? "Close" : "Send message"}
          </button>
        </div>

        {showForm ? (
          <form className="form-grid mt-5" onSubmit={handleSend}>
            <label className="field">
              <span>Subject</span>
              <input
                maxLength={180}
                value={subject}
                placeholder={defaultSubject}
                onChange={(event) => setSubject(event.target.value)}
              />
            </label>
            <label className="field">
              <span>Message</span>
              <textarea
                value={message}
                placeholder="Write a clear message for the customer."
                onChange={(event) => setMessage(event.target.value)}
              />
            </label>
            <div className="action-row">
              <button className="button" disabled={sending} type="submit">
                {sending ? "Sending..." : "Send email"}
              </button>
              <button
                className="button-ghost"
                disabled={sending}
                type="button"
                onClick={() => setShowForm(false)}
              >
                Cancel
              </button>
            </div>
          </form>
        ) : null}

        <div aria-live="polite" className="mt-4">
          {feedback ? <p className="feedback">{feedback}</p> : null}
          {error ? <p className="error">{error}</p> : null}
        </div>

        <div className="communication-list mt-5">
          {loading ? <p className="text-muted">Loading message history...</p> : null}
          {!loading && notifications.length === 0 ? (
            <p className="text-muted">No messages recorded for this {context} yet.</p>
          ) : null}
          {notifications.map((notification) => (
            <article className="communication-item" key={notification.id}>
              <div className="flex flex-wrap items-start justify-between gap-3">
                <div>
                  <p className="font-black">{notification.subject}</p>
                  <p className="mt-1 text-sm text-muted">
                    {formatStatus(notification.notificationType)} · {formatDateTime(notification.createdAt)}
                  </p>
                </div>
                <StatusBadge value={notification.status} />
              </div>
              {notification.lastError ? (
                <p className="mt-2 text-sm error">{notification.lastError}</p>
              ) : null}
              <div className="action-row mt-3">
                <Link className="button-ghost" href={`/messages/${notification.id}`}>
                  View
                </Link>
                {notification.status === "FAILED" ? (
                  <button
                    className="button-secondary"
                    type="button"
                    onClick={() => handleRetry(notification.id)}
                  >
                    Retry email
                  </button>
                ) : null}
              </div>
            </article>
          ))}
        </div>
      </div>
    </section>
  );
}
