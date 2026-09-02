import type { Metadata } from "next";
import { HomeFooter } from "@/components/home/HomeFooter";
import { HomeHeader } from "@/components/home/HomeHeader";
import { MobileConversionBar } from "@/components/home/MobileConversionBar";
import { Container } from "@/components/layout/Container";
import { BasketPageContent } from "@/components/cart/BasketPageContent";
import { buildPageMetadata } from "@/lib/seo";

export const metadata: Metadata = buildPageMetadata({
  title: "Your Order | Basilico Dorchester",
  description:
    "Review your Basilico order basket before continuing to checkout.",
  path: "/basket",
  noindex: true,
});

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
