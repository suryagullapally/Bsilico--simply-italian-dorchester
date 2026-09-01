import { MenuEditRouteClient } from "@/components/routes/MenuEditRouteClient";

type EditMenuItemPageProps = {
  params: Promise<{ id: string }>;
};

export default async function EditMenuItemPage({ params }: EditMenuItemPageProps) {
  const { id } = await params;
  return <MenuEditRouteClient id={id} />;
}
