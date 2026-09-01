"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import { useSearchParams } from "next/navigation";
import { OrderFilters } from "@/components/orders/OrderFilters";
import { OrdersTable } from "@/components/orders/OrdersTable";
import { ErrorPanel } from "@/components/ui/ErrorPanel";
import { LoadingPanel } from "@/components/ui/LoadingPanel";
import { PageHeader } from "@/components/ui/PageHeader";
import { getAdminOrders } from "@/lib/api/admin-orders-api";
import type {
  AdminOrderSummaryResponse,
  FulfilmentType,
  OrderStatus,
  PaymentStatus,
} from "@/types/admin";

export function OrdersRouteClient() {
  const searchParams = useSearchParams();
  const status = searchParams.get("status") as OrderStatus | null;
  const paymentStatus = searchParams.get("paymentStatus") as PaymentStatus | null;
  const fulfilmentType = searchParams.get("fulfilmentType") as FulfilmentType | null;
  const [orders, setOrders] = useState<AdminOrderSummaryResponse[] | null>(null);
  const [loading, setLoading] = useState(true);
  const filters = useMemo(
    () => ({
      fulfilmentType: fulfilmentType ?? undefined,
      paymentStatus: paymentStatus ?? undefined,
      status: status ?? undefined,
    }),
    [fulfilmentType, paymentStatus, status],
  );

  const load = useCallback(async () => {
    setLoading(true);

    try {
      const response = await getAdminOrders({ ...filters, size: 100 });
      setOrders(response.content);
    } catch {
      setOrders(null);
    } finally {
      setLoading(false);
    }
  }, [filters]);

  useEffect(() => {
    const timeoutId = window.setTimeout(() => {
      void load();
    }, 0);

    return () => window.clearTimeout(timeoutId);
  }, [load]);

  return (
    <div className="page-stack">
      <PageHeader eyebrow="Orders" title="Restaurant orders.">
        <p>Review new orders, payment status, fulfilment and kitchen state.</p>
      </PageHeader>
      <OrderFilters
        fulfilmentType={filters.fulfilmentType}
        paymentStatus={filters.paymentStatus}
        status={filters.status}
      />
      {loading ? <LoadingPanel label="Loading orders..." /> : null}
      {!loading && !orders ? <ErrorPanel title="Could not load orders." /> : null}
      {!loading && orders ? <OrdersTable orders={orders} /> : null}
    </div>
  );
}
