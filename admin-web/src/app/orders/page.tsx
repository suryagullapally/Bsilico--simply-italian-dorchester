import { Suspense } from "react";
import { OrdersRouteClient } from "@/components/routes/OrdersRouteClient";
import { LoadingPanel } from "@/components/ui/LoadingPanel";

export default function OrdersPage() {
  return (
    <Suspense fallback={<LoadingPanel label="Loading orders..." />}>
      <OrdersRouteClient />
    </Suspense>
  );
}
