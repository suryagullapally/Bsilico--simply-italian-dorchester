import Link from "next/link";
import { BrandLogo } from "@/components/brand/BrandLogo";
import { CartHeaderLink } from "@/components/cart/CartHeaderLink";
import { Container } from "@/components/layout/Container";
import { primaryNavigation } from "@/components/home/home-data";
import { routes } from "@/lib/routes";

export function HomeHeader() {
  return (
    <header className="home-header">
      <Container className="home-header__container">
        <nav className="home-header__nav" aria-label="Primary navigation">
          <Link className="home-header__brand" href="/" aria-label="Basilico home">
            <BrandLogo
              className="home-header__logo"
              priority
              sizes="(max-width: 767px) 7rem, 8rem"
            />
          </Link>

          <div className="home-header__desktop-links">
            {primaryNavigation
              .filter((link) => link.label !== "Order Online")
              .map((link) => (
                <Link className="home-header__link" href={link.href} key={link.label}>
                  {link.label}
                </Link>
              ))}
          </div>

          <Link className="home-header__cta" href={routes.order}>
            Order Online
          </Link>

          <CartHeaderLink />

          <div className="home-header__mobile-actions">
            <Link className="home-header__mobile-order" href={routes.order}>
              Order
            </Link>

            <details className="home-header__mobile-menu">
              <summary className="home-header__menu-trigger">
                <span className="home-header__menu-icon" aria-hidden="true">
                  <span />
                  <span />
                </span>
                <span>Menu</span>
              </summary>

              <div className="home-header__mobile-panel">
                {primaryNavigation.map((link) => (
                  <Link
                    className="home-header__mobile-link"
                    href={link.href}
                    key={link.label}
                  >
                    {link.label}
                  </Link>
                ))}
              </div>
            </details>
          </div>
        </nav>
      </Container>
    </header>
  );
}
