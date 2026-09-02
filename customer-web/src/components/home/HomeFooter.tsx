import Link from "next/link";
import { BrandLogo } from "@/components/brand/BrandLogo";
import { Container } from "@/components/layout/Container";
import { routes } from "@/lib/routes";
import {
  BASILICO_ADDRESS,
  BASILICO_PHONE_DISPLAY,
  BASILICO_PHONE_HREF,
} from "@/lib/site-config";

const footerLinks = [
  { href: routes.menu, label: "Menu" },
  { href: routes.order, label: "Order Online" },
  { href: routes.book, label: "Book a Table" },
  { href: routes.contact, label: "Contact" },
  { href: routes.privacy, label: "Privacy" },
  { href: routes.allergens, label: "Allergens" },
];

export function HomeFooter() {
  return (
    <footer className="home-footer">
      <Container className="home-footer__container">
        <div className="home-footer__brand">
          <Link href={routes.home} aria-label="Basilico home">
            <BrandLogo className="home-footer__logo" sizes="8rem" />
          </Link>
          <p className="type-small home-footer__location">
            Italian restaurant in Dorchester, Dorset
          </p>
          <address className="home-footer__contact">
            <span>
              {BASILICO_ADDRESS.street}, {BASILICO_ADDRESS.locality}{" "}
              {BASILICO_ADDRESS.postalCode}
            </span>
            <a href={BASILICO_PHONE_HREF}>{BASILICO_PHONE_DISPLAY}</a>
            <span>Wednesday - Monday 12:00 - 23:00</span>
            <span>Tuesday closed</span>
          </address>
        </div>

        <nav className="home-footer__links" aria-label="Footer navigation">
          {footerLinks.map((link) => (
            <Link href={link.href} key={link.label}>
              {link.label}
            </Link>
          ))}
        </nav>

        <div className="home-footer__social" aria-label="Social links">
          <span className="sr-only">Social links will be added here.</span>
        </div>
      </Container>
    </footer>
  );
}
