"use client";

import { useCallback, useEffect, useState } from "react";
import { OrderDetailPage } from "@/components/orders/OrderDetailPage";
import { ErrorPanel } from "@/components/ui/ErrorPanel";
import { LoadingPanel } from "@/components/ui/LoadingPanel";
import { getAdminOrder } from "@/lib/api/admin-orders-api";
import type { AdminOrderResponse } from "@/types/admin";

export function OrderDetailRouteClient({ id }: { id: string }) {
  const [order, setOrder] = useState<AdminOrderResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [notFound, setNotFound] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    setNotFound(false);

    try {
      setOrder(await getAdminOrder(id));
    } catch (error) {
      setOrder(null);
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

  if (loading) {
    return <LoadingPanel label="Loading order..." />;
  }

  if (notFound) {
    return <ErrorPanel title="Order not found." />;
  }

  if (!order) {
    return <ErrorPanel title="Could not load this order." />;
  }

  return <OrderDetailPage order={order} onOrderUpdated={setOrder} />;
}
