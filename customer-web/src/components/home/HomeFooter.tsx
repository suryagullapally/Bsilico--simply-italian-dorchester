import Link from "next/link";
import { BrandLogo } from "@/components/brand/BrandLogo";
import { Container } from "@/components/layout/Container";
import { routes } from "@/lib/routes";

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
            Dorchester &middot; Dorset
          </p>
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
