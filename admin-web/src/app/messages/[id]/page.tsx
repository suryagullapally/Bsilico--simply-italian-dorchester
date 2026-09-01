import { MessageDetailPage } from "@/components/messages/MessageDetailPage";

type PageProps = {
  params: Promise<{ id: string }>;
};

export default async function MessageDetailRoute({ params }: PageProps) {
  const { id } = await params;

  return <MessageDetailPage id={id} />;
}
