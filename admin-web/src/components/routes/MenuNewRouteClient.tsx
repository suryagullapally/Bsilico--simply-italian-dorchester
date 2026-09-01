"use client";

import { useCallback, useEffect, useState } from "react";
import { MenuItemForm } from "@/components/menu/MenuItemForm";
import { ErrorPanel } from "@/components/ui/ErrorPanel";
import { LoadingPanel } from "@/components/ui/LoadingPanel";
import { PageHeader } from "@/components/ui/PageHeader";
import { getMenuCategories } from "@/lib/api/admin-menu-api";
import type { MenuCategoryResponse } from "@/types/admin";

export function MenuNewRouteClient() {
  const [categories, setCategories] = useState<MenuCategoryResponse[] | null>(null);
  const [loading, setLoading] = useState(true);

  const load = useCallback(async () => {
    setLoading(true);

    try {
      setCategories(await getMenuCategories());
    } catch {
      setCategories(null);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    const timeoutId = window.setTimeout(() => {
      void load();
    }, 0);

    return () => window.clearTimeout(timeoutId);
  }, [load]);

  return (
    <div className="page-stack">
      <PageHeader eyebrow="Menu" title="Add menu item." />
      {loading ? <LoadingPanel label="Loading categories..." /> : null}
      {!loading && !categories ? (
        <ErrorPanel title="Could not load menu categories." />
      ) : null}
      {!loading && categories ? <MenuItemForm categories={categories} /> : null}
    </div>
  );
}
