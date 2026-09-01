import type { Metadata } from "next";
import { HomeFooter } from "@/components/home/HomeFooter";
import { HomeHeader } from "@/components/home/HomeHeader";
import { MobileConversionBar } from "@/components/home/MobileConversionBar";
import { Container } from "@/components/layout/Container";
import { BasketPageContent } from "@/components/cart/BasketPageContent";

export const metadata: Metadata = {
  description:
    "Review your Basilico order basket before continuing to checkout.",
  title: {
    absolute: "Your Order | Basilico Dorchester",
  },
};

export default function BasketPage() {
  return (
    <>
      <HomeHeader />
      <main className="basket-page">
        <Container className="basket-page__container">
          <BasketPageContent />
        </Container>
      </main>
      <HomeFooter />
      <MobileConversionBar />
    </>
  );
}
