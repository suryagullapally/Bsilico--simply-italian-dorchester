import { Container } from "@/components/layout/Container";
import { HomeImageSlot } from "@/components/home/HomeImageSlot";
import { restaurantImages } from "@/components/home/home-data";

export function AtmosphereSection() {
  return (
    <section
      className="home-section home-atmosphere"
      aria-labelledby="home-atmosphere-title"
    >
      <Container className="home-atmosphere__container">
        <div className="home-atmosphere__intro">
          <p className="type-eyebrow home-section__eyebrow">Restaurant atmosphere</p>
          <h2 className="type-h2 home-section__title" id="home-atmosphere-title">
            Warmth from the oven to the table.
          </h2>
          <p className="type-body home-section__copy">
            Inside, the mood is warm, relaxed and made for simple Italian
            evenings.
          </p>
        </div>

        <div className="home-atmosphere__gallery" aria-label="Restaurant gallery">
          {restaurantImages.map((image) => (
            <HomeImageSlot
              alt={image.alt}
              className="home-atmosphere__image"
              key={image.src}
              sizes="(max-width: 767px) 82vw, (max-width: 1279px) 32vw, 24rem"
              src={image.src}
            />
          ))}
        </div>
      </Container>
    </section>
  );
}
