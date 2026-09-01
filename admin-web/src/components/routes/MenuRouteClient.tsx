"use client";

import Link from "next/link";
import { useCallback, useEffect, useState } from "react";
import { MenuManagementPage } from "@/components/menu/MenuManagementPage";
import { ErrorPanel } from "@/components/ui/ErrorPanel";
import { LoadingPanel } from "@/components/ui/LoadingPanel";
import { PageHeader } from "@/components/ui/PageHeader";
import { getAdminMenuItems, getMenuCategories } from "@/lib/api/admin-menu-api";
import type { MenuCategoryResponse, MenuItemResponse } from "@/types/admin";

export function MenuRouteClient() {
  const [items, setItems] = useState<MenuItemResponse[] | null>(null);
  const [categories, setCategories] = useState<MenuCategoryResponse[] | null>(null);
  const [loading, setLoading] = useState(true);

  const load = useCallback(async () => {
    setLoading(true);

    try {
      const [menuItems, menuCategories] = await Promise.all([
        getAdminMenuItems(),
        getMenuCategories(),
      ]);
      setItems(menuItems);
      setCategories(menuCategories);
    } catch {
      setItems(null);
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

  function updateItem(updated: MenuItemResponse) {
    setItems((current) =>
      current?.map((item) => (item.id === updated.id ? updated : item)) ?? current,
    );
  }

  return (
    <div className="page-stack">
      <PageHeader eyebrow="Menu" title="Menu management.">
        <p>
          Quickly mark items sold out, feature dishes, archive old records or
          edit menu details.
        </p>
      </PageHeader>
      <div>
        <Link className="button" href="/menu/new">
          Add menu item
        </Link>
      </div>
      <p className="notice">
        Archived items are removed from the public menu but retained for
        historical records.
      </p>
      {loading ? <LoadingPanel label="Loading menu..." /> : null}
      {!loading && (!items || !categories) ? (
        <ErrorPanel title="Could not load menu items." />
      ) : null}
      {!loading && items && categories ? (
        <MenuManagementPage
          categories={categories}
          items={items}
          onItemUpdated={updateItem}
        />
      ) : null}
    </div>
  );
}
