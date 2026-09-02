import type { Metadata } from "next";
import Link from "next/link";
import { HomeFooter } from "@/components/home/HomeFooter";
import { HomeHeader } from "@/components/home/HomeHeader";
import { MobileConversionBar } from "@/components/home/MobileConversionBar";
import { Container } from "@/components/layout/Container";
import { routes } from "@/lib/routes";
import { buildPageMetadata } from "@/lib/seo";

export const metadata: Metadata = buildPageMetadata({
  title: "Privacy | Basilico Dorchester",
  description:
    "Privacy information for the Basilico customer website, online ordering and table booking.",
  path: "/privacy",
});

export default function PrivacyPage() {
  return (
    <>
      <HomeHeader />
      <main className="checkout-page">
        <Container className="checkout-page__container">
          <section className="checkout-placeholder" aria-labelledby="privacy-title">
            <p className="type-eyebrow checkout-placeholder__eyebrow">
              Privacy
            </p>
            <h1 className="type-h1 checkout-placeholder__title" id="privacy-title">
              Privacy information.
            </h1>
            <p className="type-body checkout-placeholder__copy">
              Basilico privacy information will be added before launch.
            </p>
            <Link className="checkout-placeholder__menu-link" href={routes.home}>
              Back to Basilico
            </Link>
          </section>
        </Container>
      </main>
      <HomeFooter />
      <MobileConversionBar />
    </>
  );
}
