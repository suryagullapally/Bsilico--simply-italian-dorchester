import { Container } from "@/components/layout/Container";
import { HomeImageSlot } from "@/components/home/HomeImageSlot";
import { ButtonLink } from "@/components/ui/Button";
import {
  BASILICO_ADDRESS,
  BASILICO_PHONE_DISPLAY,
  BASILICO_PHONE_HREF,
} from "@/lib/site-config";

const addressLines = [
  BASILICO_ADDRESS.street,
  BASILICO_ADDRESS.locality,
  BASILICO_ADDRESS.region,
  BASILICO_ADDRESS.postalCode,
  "United Kingdom",
];

const mapImageSrc = "/images/location/basilico-map.webp";
const directionsQuery =
  "41 Trinity Street, Dorchester, Dorset, DT1 1TT, United Kingdom";
const directionsHref = `https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(
  directionsQuery,
)}`;

export function VisitSection() {
  return (
    <section
      className="home-section home-visit"
      id="visit"
      aria-labelledby="home-visit-title"
    >
      <Container className="home-visit__container">
        <div className="home-visit__intro">
          <p className="type-eyebrow home-section__eyebrow">Visit us</p>
          <h2 className="type-h2 home-section__title" id="home-visit-title">
            Basilico in Dorchester.
          </h2>
          <address className="home-visit__address">
            41 Trinity Street, Dorchester
            <span aria-hidden="true"> · </span>
            Dorset
            <br />
            Simple Italian, sourdough pizza, dine in, takeaway and delivery.
          </address>
        </div>

        <div className="home-visit__details">
          <HomeImageSlot
            alt="Map showing Basilico on Trinity Street in Dorchester"
            className="home-visit__map-preview"
            imageClassName="home-visit__map-image"
            sizes="(max-width: 767px) 92vw, 44vw"
            src={mapImageSrc}
          />

          <dl className="visit-detail-list">
            <div className="visit-detail">
              <dt>Address</dt>
              <dd>
                <address className="visit-detail__address">
                  {addressLines.map((line) => (
                    <span key={line}>{line}</span>
                  ))}
                </address>
              </dd>
            </div>
            <div className="visit-detail">
              <dt>Phone</dt>
              <dd>
                <a
                  className="visit-detail__link"
                  href={BASILICO_PHONE_HREF}
                  aria-label={`Call Basilico on ${BASILICO_PHONE_DISPLAY}`}
                >
                  {BASILICO_PHONE_DISPLAY}
                </a>
              </dd>
            </div>
            <div className="visit-detail">
              <dt>Opening hours</dt>
              <dd>
                <span className="visit-detail__hours">
                  <span className="visit-detail__hours-row">
                    <span>Wednesday – Monday</span>
                    <span>12:00 – 23:00</span>
                  </span>
                  <span className="visit-detail__hours-row">
                    <span>Tuesday</span>
                    <span>Closed</span>
                  </span>
                </span>
              </dd>
            </div>
          </dl>

          <ButtonLink
            aria-label="Get directions to Basilico on Google Maps"
            className="home-visit__directions"
            href={directionsHref}
            target="_blank"
            rel="noopener noreferrer"
            variant="secondary"
          >
            Get Directions
          </ButtonLink>
        </div>
      </Container>
    </section>
  );
}
