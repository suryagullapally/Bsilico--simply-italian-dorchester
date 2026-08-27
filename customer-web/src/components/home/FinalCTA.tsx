import { Container } from "@/components/layout/Container";
import { ButtonLink } from "@/components/ui/Button";
import { routes } from "@/lib/routes";

export function FinalCTA() {
  return (
    <section className="home-final-cta" aria-labelledby="home-final-cta-title">
      <Container className="home-final-cta__container">
        <div>
          <p className="type-eyebrow home-final-cta__eyebrow">Ready for Basilico?</p>
          <h2 className="type-h2 home-final-cta__title" id="home-final-cta-title">
            Your table is waiting.
          </h2>
        </div>

        <div className="home-final-cta__actions" aria-label="Final actions">
          <ButtonLink href={routes.order}>Order Online</ButtonLink>
          <ButtonLink href={routes.book} variant="secondary">
            Book a Table
          </ButtonLink>
        </div>
      </Container>
    </section>
  );
}
