"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import { useSearchParams } from "next/navigation";
import { PaymentsPage } from "@/components/payments/PaymentsPage";
import { ErrorPanel } from "@/components/ui/ErrorPanel";
import { LoadingPanel } from "@/components/ui/LoadingPanel";
import { PageHeader } from "@/components/ui/PageHeader";
import { getAdminOrders } from "@/lib/api/admin-orders-api";
import type { AdminOrderSummaryResponse, PaymentStatus } from "@/types/admin";

type PaymentData = {
  allOrders: AdminOrderSummaryResponse[];
  filteredOrders: AdminOrderSummaryResponse[];
};

export function PaymentsRouteClient() {
  const searchParams = useSearchParams();
  const paymentStatus = searchParams.get("paymentStatus") as PaymentStatus | null;
  const [data, setData] = useState<PaymentData | null>(null);
  const [loading, setLoading] = useState(true);
  const filters = useMemo(
    () => ({ paymentStatus: paymentStatus ?? undefined }),
    [paymentStatus],
  );

  const load = useCallback(async () => {
    setLoading(true);

    try {
      const [filteredOrders, allOrders] = await Promise.all([
        getAdminOrders({ ...filters, size: 100 }),
        getAdminOrders({ size: 100 }),
      ]);
      setData({
        allOrders: allOrders.content,
        filteredOrders: filteredOrders.content,
      });
    } catch {
      setData(null);
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
      <PageHeader eyebrow="Payments" title="Payment status.">
        <p>Operational view of order payment states until Stripe is connected.</p>
      </PageHeader>
      {loading ? <LoadingPanel label="Loading payment status..." /> : null}
      {!loading && !data ? (
        <ErrorPanel title="Could not load payment status data." />
      ) : null}
      {!loading && data ? (
        <PaymentsPage
          allOrders={data.allOrders}
          orders={data.filteredOrders}
          paymentStatus={filters.paymentStatus}
        />
      ) : null}
    </div>
  );
}
