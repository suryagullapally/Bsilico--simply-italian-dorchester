import type { Metadata } from "next";
import Link from "next/link";
import { HomeFooter } from "@/components/home/HomeFooter";
import { HomeHeader } from "@/components/home/HomeHeader";
import { MobileConversionBar } from "@/components/home/MobileConversionBar";
import { Container } from "@/components/layout/Container";
import { routes } from "@/lib/routes";

export const metadata: Metadata = {
  description: "Allergen guidance for Basilico customers in Dorchester.",
  title: {
    absolute: "Allergens | Basilico Dorchester",
  },
};

export default function AllergensPage() {
  return (
    <>
      <HomeHeader />
      <main className="checkout-page">
        <Container className="checkout-page__container">
          <section className="checkout-placeholder" aria-labelledby="allergens-title">
            <p className="type-eyebrow checkout-placeholder__eyebrow">
              Allergens
            </p>
            <h1 className="type-h1 checkout-placeholder__title" id="allergens-title">
              Allergen information.
            </h1>
            <p className="type-body checkout-placeholder__copy">
              Detailed allergen information will be added before launch. Please
              tell us about any allergies or dietary requirements before
              ordering or booking.
            </p>
            <Link className="checkout-placeholder__menu-link" href={routes.menu}>
              View menu
            </Link>
          </section>
        </Container>
      </main>
      <HomeFooter />
      <MobileConversionBar />
    </>
  );
}
