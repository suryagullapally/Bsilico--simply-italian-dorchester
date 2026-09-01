"use client";

import { useCallback, useEffect, useState } from "react";
import { MenuItemForm } from "@/components/menu/MenuItemForm";
import { ErrorPanel } from "@/components/ui/ErrorPanel";
import { LoadingPanel } from "@/components/ui/LoadingPanel";
import { PageHeader } from "@/components/ui/PageHeader";
import { getAdminMenuItem, getMenuCategories } from "@/lib/api/admin-menu-api";
import type { MenuCategoryResponse, MenuItemResponse } from "@/types/admin";

export function MenuEditRouteClient({ id }: { id: string }) {
  const [item, setItem] = useState<MenuItemResponse | null>(null);
  const [categories, setCategories] = useState<MenuCategoryResponse[] | null>(null);
  const [loading, setLoading] = useState(true);
  const [notFound, setNotFound] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    setNotFound(false);

    try {
      const [menuItem, menuCategories] = await Promise.all([
        getAdminMenuItem(id),
        getMenuCategories(),
      ]);
      setItem(menuItem);
      setCategories(menuCategories);
    } catch (error) {
      setItem(null);
      setCategories(null);
      setNotFound((error as { status?: number }).status === 404);
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

  return (
    <div className="page-stack">
      <PageHeader eyebrow="Menu" title={item ? item.name : "Edit menu item."} />
      {loading ? <LoadingPanel label="Loading menu item..." /> : null}
      {!loading && notFound ? <ErrorPanel title="Menu item not found." /> : null}
      {!loading && !notFound && (!item || !categories) ? (
        <ErrorPanel title="Could not load this menu item." />
      ) : null}
      {!loading && item && categories ? (
        <MenuItemForm categories={categories} item={item} />
      ) : null}
    </div>
  );
}
