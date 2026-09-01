"use client";

import { useCallback, useEffect, useState } from "react";
import { BookingDetailPage } from "@/components/bookings/BookingDetailPage";
import { ErrorPanel } from "@/components/ui/ErrorPanel";
import { LoadingPanel } from "@/components/ui/LoadingPanel";
import { getAdminBooking } from "@/lib/api/admin-bookings-api";
import type { AdminBookingResponse } from "@/types/admin";

export function BookingDetailRouteClient({ id }: { id: string }) {
  const [booking, setBooking] = useState<AdminBookingResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [notFound, setNotFound] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    setNotFound(false);

    try {
      setBooking(await getAdminBooking(id));
    } catch (error) {
      setBooking(null);
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
    return <LoadingPanel label="Loading booking..." />;
  }

  if (notFound) {
    return <ErrorPanel title="Booking not found." />;
  }

  if (!booking) {
    return <ErrorPanel title="Could not load this booking." />;
  }

  return <BookingDetailPage booking={booking} onBookingUpdated={setBooking} />;
}
