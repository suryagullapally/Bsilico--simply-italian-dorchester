"use client";

import { useRouter } from "next/navigation";
import { useState, useTransition } from "react";
import {
  updateMenuItemActive,
  updateMenuItemAvailability,
  updateMenuItemFeatured,
} from "@/lib/api/admin-menu-api";
import { getAdminApiErrorMessage } from "@/lib/api/api-error";
import type { MenuItemResponse } from "@/types/admin";

export function MenuQuickActions({
  item,
  onItemUpdated,
}: {
  item: MenuItemResponse;
  onItemUpdated?: (item: MenuItemResponse) => void;
}) {
  const router = useRouter();
  const [pending, startTransition] = useTransition();
  const [current, setCurrent] = useState(item);
  const [confirmArchive, setConfirmArchive] = useState(false);
  const [feedback, setFeedback] = useState("");
  const [error, setError] = useState("");

  async function mutate(
    action: () => Promise<MenuItemResponse>,
    message: (updated: MenuItemResponse) => string,
  ) {
    setError("");
    setFeedback("");

    try {
      const updated = await action();
      setCurrent(updated);
      onItemUpdated?.(updated);
      setConfirmArchive(false);
      setFeedback(message(updated));
      startTransition(() => router.refresh());
    } catch (caught) {
      setError(getAdminApiErrorMessage(caught, "Could not update menu item."));
    }
  }

  return (
    <div>
      <div className="action-row">
        <button
          className={current.available ? "button-secondary" : "button"}
          disabled={pending}
          type="button"
          onClick={() =>
            mutate(
              () =>
                updateMenuItemAvailability(current.id, {
                  available: !current.available,
                }),
              (updated) =>
                updated.available
                  ? `${updated.name} marked available.`
                  : `${updated.name} marked sold out.`,
            )
          }
        >
          {current.available ? "Mark sold out" : "Mark available"}
        </button>
        <button
          className="button-ghost"
          disabled={pending}
          type="button"
          onClick={() =>
            mutate(
              () =>
                updateMenuItemFeatured(current.id, {
                  featured: !current.featured,
                }),
              (updated) =>
                updated.featured
                  ? `${updated.name} marked featured.`
                  : `${updated.name} removed from featured.`,
            )
          }
        >
          {current.featured ? "Unfeature" : "Feature"}
        </button>
        {current.active ? (
          confirmArchive ? (
            <>
              <button
                className="button-danger"
                disabled={pending}
                type="button"
                onClick={() =>
                  mutate(
                    () => updateMenuItemActive(current.id, { active: false }),
                    (updated) =>
                      `${updated.name} archived. It is retained for records.`,
                  )
                }
              >
                Confirm archive
              </button>
              <button
                className="button-ghost"
                disabled={pending}
                type="button"
                onClick={() => setConfirmArchive(false)}
              >
                Keep
              </button>
            </>
          ) : (
            <button
              className="button-ghost"
              disabled={pending}
              type="button"
              onClick={() => setConfirmArchive(true)}
            >
              Archive
            </button>
          )
        ) : (
          <button
            className="button-secondary"
            disabled={pending}
            type="button"
            onClick={() =>
              mutate(
                () => updateMenuItemActive(current.id, { active: true }),
                (updated) => `${updated.name} restored to the public menu.`,
              )
            }
          >
            Restore
          </button>
        )}
      </div>
      <div aria-live="polite" className="mt-3">
        {feedback ? <p className="feedback">{feedback}</p> : null}
        {error ? <p className="error">{error}</p> : null}
      </div>
    </div>
  );
}
