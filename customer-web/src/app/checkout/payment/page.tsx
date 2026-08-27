import type { Metadata } from "next";
import Link from "next/link";
import { HomeFooter } from "@/components/home/HomeFooter";
import { HomeHeader } from "@/components/home/HomeHeader";
import { MobileConversionBar } from "@/components/home/MobileConversionBar";
import { Container } from "@/components/layout/Container";
import { ButtonLink } from "@/components/ui/Button";
import { routes } from "@/lib/routes";

export const metadata: Metadata = {
  description:
    "Basilico online payment will be connected in the next checkout phase.",
  title: "Payment | Basilico Dorchester",
};

export default function CheckoutPaymentPage() {
  return (
    <>
      <HomeHeader />
      <main className="checkout-page">
        <Container className="checkout-page__container">
          <section className="checkout-placeholder" aria-labelledby="payment-title">
            <p className="type-eyebrow checkout-placeholder__eyebrow">
              Payment
            </p>
            <h1 className="type-h1 checkout-placeholder__title" id="payment-title">
              Your order is ready for payment.
            </h1>
            <p className="type-body checkout-placeholder__copy">
              Online payment will be connected next.
            </p>
            <div className="checkout-placeholder__actions">
              <ButtonLink href={routes.checkout} variant="primary">
                BACK TO CHECKOUT
              </ButtonLink>
              <Link className="checkout-placeholder__menu-link" href={routes.basket}>
                Back to basket
              </Link>
            </div>
          </section>
        </Container>
      </main>
      <HomeFooter />
      <MobileConversionBar />
    </>
  );
}
