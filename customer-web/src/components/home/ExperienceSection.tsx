import { Container } from "@/components/layout/Container";
import { HomeImageSlot } from "@/components/home/HomeImageSlot";
import { ButtonLink } from "@/components/ui/Button";
import { routes } from "@/lib/routes";

const experiences = [
  {
    alt: "Basilico dining room prepared for restaurant guests",
    cta: "Book a Table",
    description: "Book a table and enjoy Basilico in the restaurant.",
    href: routes.book,
    imageSrc: "/images/restaurant/interior-01.webp",
    label: "Dine with us",
  },
  {
    alt: "Basilico Italian food prepared for ordering",
    cta: "Order Online",
    description: "Enjoy Basilico at home with delivery or collection.",
    href: routes.order,
    imageSrc: "/images/home/order-basilico.webp",
    label: "Order Basilico",
  },
];

export function ExperienceSection() {
  return (
    <section
      className="home-section home-experience"
      aria-labelledby="home-experience-title"
    >
      <Container>
        <div className="home-section__header home-experience__header">
          <p className="type-eyebrow home-section__eyebrow">Choose your Basilico</p>
          <h2 className="type-h2 home-section__title" id="home-experience-title">
            The experience, your way
          </h2>
        </div>

        <div className="home-experience__grid">
          {experiences.map((experience) => (
            <article className="experience-card" key={experience.label}>
              <HomeImageSlot
                alt={experience.alt}
                className="experience-card__media"
                imageClassName="experience-card__image"
                sizes="(max-width: 767px) 92vw, 44vw"
                src={experience.imageSrc}
              />
              <div className="experience-card__content">
                <h3 className="type-h3 experience-card__title">
                  {experience.label}
                </h3>
                <p className="type-body experience-card__copy">
                  {experience.description}
                </p>
                <ButtonLink href={experience.href} variant="secondary">
                  {experience.cta}
                </ButtonLink>
              </div>
            </article>
          ))}
        </div>
      </Container>
    </section>
  );
}
