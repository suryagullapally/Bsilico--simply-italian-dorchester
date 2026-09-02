import type { Metadata } from "next";
import { BookingPageContent } from "@/components/booking/BookingPageContent";
import { HomeFooter } from "@/components/home/HomeFooter";
import { HomeHeader } from "@/components/home/HomeHeader";
import { MobileConversionBar } from "@/components/home/MobileConversionBar";
import { Container } from "@/components/layout/Container";
import { buildPageMetadata } from "@/lib/seo";

export const metadata: Metadata = buildPageMetadata({
  title: "Book a Table | Basilico Dorchester",
  description:
    "Book a table at Basilico, an Italian restaurant on Trinity Street in Dorchester, Dorset.",
  path: "/book",
});

export default function BookPage() {
  return (
    <>
      <HomeHeader />
      <main className="booking-page">
        <Container className="booking-page__container">
          <BookingPageContent />
        </Container>
      </main>
      <HomeFooter />
      <MobileConversionBar />
    </>
  );
}
