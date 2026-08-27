import Link from "next/link";
import { Container } from "@/components/layout/Container";
import { HomeImageSlot } from "@/components/home/HomeImageSlot";
import { signatureDishes } from "@/components/home/home-data";

export function SignatureDishes() {
  return (
    <section
      className="home-section signature-dishes"
      id="favourites"
      aria-labelledby="signature-dishes-title"
    >
      <Container>
        <div className="home-section__header signature-dishes__header">
          <p className="type-eyebrow home-section__eyebrow">
            From the Basilico kitchen
          </p>
          <h2 className="type-h2 home-section__title" id="signature-dishes-title">
            Our favourites
          </h2>
          <p className="type-body home-section__copy">
            Simple Italian favourites, ready for the table or to enjoy at home.
          </p>
        </div>

        <div className="signature-dishes__rail" aria-label="Signature dishes">
          {signatureDishes.map((dish) => (
            <article className="signature-card" key={dish.name}>
              <HomeImageSlot
                alt={`${dish.name} at Basilico`}
                className="signature-card__image"
                sizes="(max-width: 767px) 82vw, (max-width: 1279px) 35vw, 18rem"
                src={dish.imageSrc}
              />

              <div className="signature-card__body">
                <div className="signature-card__heading">
                  <h3 className="type-h3 signature-card__title">{dish.name}</h3>
                  <p className="type-price signature-card__price">{dish.price}</p>
                </div>
                <p className="type-small signature-card__description">
                  {dish.description}
                </p>
                <div className="signature-card__badges" aria-hidden="true" />
                <Link className="signature-card__link" href={dish.href}>
                  View dish
                </Link>
              </div>
            </article>
          ))}
        </div>
      </Container>
    </section>
  );
}
