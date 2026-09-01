"use client";

import { useCallback, useEffect, useState } from "react";
import { DashboardPage } from "@/components/dashboard/DashboardPage";
import { LoadingPanel } from "@/components/ui/LoadingPanel";
import { getAdminBookings } from "@/lib/api/admin-bookings-api";
import { getAdminMenuItems } from "@/lib/api/admin-menu-api";
import { getAdminNotifications } from "@/lib/api/admin-messages-api";
import { getAdminOrders } from "@/lib/api/admin-orders-api";
import type {
  AdminBookingSummaryResponse,
  AdminOrderSummaryResponse,
  MenuItemResponse,
} from "@/types/admin";

type DashboardData = {
  bookings: AdminBookingSummaryResponse[];
  failedNotificationCount: number;
  menuItems: MenuItemResponse[];
  orders: AdminOrderSummaryResponse[];
};

export function DashboardRouteClient() {
  const [data, setData] = useState<DashboardData | null>(null);
  const [loading, setLoading] = useState(true);

  const load = useCallback(async () => {
    setLoading(true);

    try {
      const [orders, bookings, menuItems, failedNotifications] = await Promise.all([
        getAdminOrders({ size: 100 }),
        getAdminBookings({ size: 100 }),
        getAdminMenuItems(),
        getAdminNotifications({ status: "FAILED", size: 1 }),
      ]);

      setData({
        bookings: bookings.content,
        failedNotificationCount: failedNotifications.totalElements,
        menuItems,
        orders: orders.content,
      });
    } catch {
      setData(null);
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

  if (loading) {
    return <LoadingPanel label="Loading dashboard..." />;
  }

  return <DashboardPage {...(data ?? {})} />;
}
