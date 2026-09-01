import { OrderDetailRouteClient } from "@/components/routes/OrderDetailRouteClient";

type OrderDetailRouteProps = {
  params: Promise<{ id: string }>;
};

export default async function OrderDetailRoute({ params }: OrderDetailRouteProps) {
  const { id } = await params;
  return <OrderDetailRouteClient id={id} />;
}
