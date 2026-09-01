import { Suspense } from "react";
import { BookingsRouteClient } from "@/components/routes/BookingsRouteClient";
import { LoadingPanel } from "@/components/ui/LoadingPanel";

export default function BookingsPage() {
  return (
    <Suspense fallback={<LoadingPanel label="Loading bookings..." />}>
      <BookingsRouteClient />
    </Suspense>
  );
}
