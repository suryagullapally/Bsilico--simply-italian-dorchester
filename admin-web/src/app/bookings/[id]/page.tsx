import { BookingDetailRouteClient } from "@/components/routes/BookingDetailRouteClient";

type BookingDetailRouteProps = {
  params: Promise<{ id: string }>;
};

export default async function BookingDetailRoute({
  params,
}: BookingDetailRouteProps) {
  const { id } = await params;
  return <BookingDetailRouteClient id={id} />;
}
