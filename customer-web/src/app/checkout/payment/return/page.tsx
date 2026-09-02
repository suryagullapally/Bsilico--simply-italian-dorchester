import type { Metadata } from "next";
import { PaymentReturnPageContent } from "@/components/checkout/PaymentReturnPageContent";
import { HomeFooter } from "@/components/home/HomeFooter";
import { HomeHeader } from "@/components/home/HomeHeader";
import { MobileConversionBar } from "@/components/home/MobileConversionBar";
import { Container } from "@/components/layout/Container";
import { buildPageMetadata } from "@/lib/seo";

export const metadata: Metadata = buildPageMetadata({
  title: "Payment Status | Basilico Dorchester",
  description: "Check the payment status for your Basilico order.",
  path: "/checkout/payment/return",
  noindex: true,
});

type CheckoutPaymentReturnPageProps = {
  searchParams?: Promise<{
    session_id?: string | string[];
  }>;
};

export default async function CheckoutPaymentReturnPage({
  searchParams,
}: CheckoutPaymentReturnPageProps) {
  const resolvedSearchParams = await searchParams;
  const sessionId = getSessionId(resolvedSearchParams?.session_id);

  return (
    <>
      <HomeHeader />
      <main className="checkout-page">
        <Container className="checkout-page__container">
          <PaymentReturnPageContent sessionId={sessionId} />
        </Container>
      </main>
      <HomeFooter />
      <MobileConversionBar />
    </>
  );
}

function getSessionId(sessionId: string | string[] | undefined) {
  const value = Array.isArray(sessionId) ? sessionId[0] : sessionId;

  return typeof value === "string" ? value.trim() : "";
}
