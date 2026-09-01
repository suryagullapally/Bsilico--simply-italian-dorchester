import type { Metadata } from "next";
import { HomeFooter } from "@/components/home/HomeFooter";
import { HomeHeader } from "@/components/home/HomeHeader";
import { MobileConversionBar } from "@/components/home/MobileConversionBar";
import { Container } from "@/components/layout/Container";
import { PaymentPageContent } from "@/components/checkout/PaymentPageContent";

export const metadata: Metadata = {
  description:
    "Complete secure online payment for your Basilico order.",
  title: {
    absolute: "Payment | Basilico Dorchester",
  },
};

type CheckoutPaymentPageProps = {
  searchParams?: Promise<{
    order?: string | string[];
  }>;
};

export default async function CheckoutPaymentPage({
  searchParams,
}: CheckoutPaymentPageProps) {
  const resolvedSearchParams = await searchParams;
  const orderReference = getOrderReference(resolvedSearchParams?.order);

  return (
    <>
      <HomeHeader />
      <main className="checkout-page">
        <Container className="checkout-page__container">
          <PaymentPageContent orderReference={orderReference} />
        </Container>
      </main>
      <HomeFooter />
      <MobileConversionBar />
    </>
  );
}

function getOrderReference(order: string | string[] | undefined) {
  const value = Array.isArray(order) ? order[0] : order;

  return typeof value === "string" ? value.trim() : "";
}
