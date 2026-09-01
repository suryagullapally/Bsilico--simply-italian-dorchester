"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import { useSearchParams } from "next/navigation";
import { BookingFilters } from "@/components/bookings/BookingFilters";
import { BookingsTable } from "@/components/bookings/BookingsTable";
import { ErrorPanel } from "@/components/ui/ErrorPanel";
import { LoadingPanel } from "@/components/ui/LoadingPanel";
import { PageHeader } from "@/components/ui/PageHeader";
import { getAdminBookings } from "@/lib/api/admin-bookings-api";
import type { AdminBookingSummaryResponse, BookingStatus } from "@/types/admin";

export function BookingsRouteClient() {
  const searchParams = useSearchParams();
  const date = searchParams.get("date") ?? undefined;
  const status = searchParams.get("status") as BookingStatus | null;
  const [bookings, setBookings] = useState<AdminBookingSummaryResponse[] | null>(null);
  const [loading, setLoading] = useState(true);
  const filters = useMemo(
    () => ({
      date,
      status: status ?? undefined,
    }),
    [date, status],
  );

  const load = useCallback(async () => {
    setLoading(true);

    try {
      const response = await getAdminBookings({ ...filters, size: 100 });
      setBookings(response.content);
    } catch {
      setBookings(null);
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
      <PageHeader eyebrow="Bookings" title="Table requests.">
        <p>Confirm, decline or review upcoming Basilico table requests.</p>
      </PageHeader>
      <BookingFilters date={filters.date} status={filters.status} />
      {loading ? <LoadingPanel label="Loading bookings..." /> : null}
      {!loading && !bookings ? <ErrorPanel title="Could not load bookings." /> : null}
      {!loading && bookings ? <BookingsTable bookings={bookings} /> : null}
    </div>
  );
}
