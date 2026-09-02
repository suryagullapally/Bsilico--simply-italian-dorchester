import type { Metadata } from "next";
import { CheckoutPageContent } from "@/components/checkout/CheckoutPageContent";
import { HomeFooter } from "@/components/home/HomeFooter";
import { HomeHeader } from "@/components/home/HomeHeader";
import { MobileConversionBar } from "@/components/home/MobileConversionBar";
import { Container } from "@/components/layout/Container";
import { buildPageMetadata } from "@/lib/seo";

export const metadata: Metadata = buildPageMetadata({
  title: "Checkout | Basilico Dorchester",
  description:
    "Review your Basilico order details for delivery or collection before payment.",
  path: "/checkout",
  noindex: true,
});

export default function CheckoutPage() {
  return (
    <>
      <HomeHeader />
      <main className="checkout-page">
        <Container className="checkout-page__container">
          <CheckoutPageContent />
        </Container>
      </main>
      <HomeFooter />
      <MobileConversionBar />
    </>
  );
}
