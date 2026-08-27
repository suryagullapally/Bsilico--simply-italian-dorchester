import type { Metadata } from "next";
import { BookingPageContent } from "@/components/booking/BookingPageContent";
import { HomeFooter } from "@/components/home/HomeFooter";
import { HomeHeader } from "@/components/home/HomeHeader";
import { MobileConversionBar } from "@/components/home/MobileConversionBar";
import { Container } from "@/components/layout/Container";

export const metadata: Metadata = {
  description:
    "Send a table booking request for Basilico, Simple Italian in Dorchester.",
  title: "Book a Table | Basilico Dorchester",
};

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
