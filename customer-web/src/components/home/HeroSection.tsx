import Link from "next/link";
import { HeroVisual } from "@/components/home/HeroVisual";
import { Container } from "@/components/layout/Container";
import { ButtonLink } from "@/components/ui/Button";
import { routes } from "@/lib/routes";

const HERO_IMAGE_SRC = "/images/home/basilico-hero.webp";

export function HeroSection() {
  return (
    <section
      className="home-hero"
      aria-labelledby="home-hero-title"
    >
      <div className="home-hero__atmosphere" aria-hidden="true" />

      <Container className="home-hero__container">
        <div className="home-hero__content">
          <p className="type-eyebrow home-hero__eyebrow">
            Basilico - Simple Italian &middot; Dorchester, Dorset
          </p>
          <h1 className="home-hero__title" id="home-hero-title">
            Simple Italian.
            <span>Made with fire.</span>
          </h1>
          <p className="type-body home-hero__copy">
            Authentic Italian food, sourdough pizza and warm hospitality on
            Trinity Street. Dine in, order takeaway or delivery, or book a
            table online.
          </p>

          <div className="home-hero__actions" aria-label="Primary actions">
            <ButtonLink href={routes.order}>Order Online</ButtonLink>
            <ButtonLink href={routes.book} variant="secondary">
              Book a Table
            </ButtonLink>
          </div>

          <Link className="home-hero__menu-link" href={routes.menu}>
            Explore Menu
          </Link>
        </div>

        <HeroVisual src={HERO_IMAGE_SRC} />
      </Container>
    </section>
  );
}
