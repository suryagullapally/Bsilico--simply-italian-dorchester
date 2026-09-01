import type { Metadata } from "next";
import { CheckoutPageContent } from "@/components/checkout/CheckoutPageContent";
import { HomeFooter } from "@/components/home/HomeFooter";
import { HomeHeader } from "@/components/home/HomeHeader";
import { MobileConversionBar } from "@/components/home/MobileConversionBar";
import { Container } from "@/components/layout/Container";

export const metadata: Metadata = {
  description:
    "Review your Basilico order details for delivery or collection before payment.",
  title: {
    absolute: "Checkout | Basilico Dorchester",
  },
};

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
