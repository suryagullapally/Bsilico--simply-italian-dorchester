"use client";

import { useRouter } from "next/navigation";
import { useTransition } from "react";

export function RefreshButton() {
  const router = useRouter();
  const [pending, startTransition] = useTransition();

  return (
    <button
      className="button-ghost"
      disabled={pending}
      type="button"
      onClick={() => startTransition(() => router.refresh())}
    >
      {pending ? "Refreshing..." : "Refresh"}
    </button>
  );
}
