import { Suspense } from "react";
import { PaymentsRouteClient } from "@/components/routes/PaymentsRouteClient";
import { LoadingPanel } from "@/components/ui/LoadingPanel";

export default function PaymentsRoute() {
  return (
    <Suspense fallback={<LoadingPanel label="Loading payments..." />}>
      <PaymentsRouteClient />
    </Suspense>
  );
}
