"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { useCart } from "@/components/cart/CartProvider";
import { routes } from "@/lib/routes";

const quickActions = [
  { href: routes.order, label: "Order" },
  { href: routes.book, label: "Book" },
  { href: routes.basket, label: "Basket" },
];

export function MobileConversionBar() {
  const { itemCount } = useCart();
  const pathname = usePathname();

  if (
    pathname === routes.book ||
    pathname === routes.checkout ||
    pathname.startsWith(`${routes.checkout}/`)
  ) {
    return null;
  }

  return (
    <nav className="mobile-conversion-bar" aria-label="Quick actions">
      {quickActions.map((action) => (
        <Link
          className="mobile-conversion-bar__item"
          href={action.href}
          key={action.label}
        >
          <span>{action.label}</span>
          {action.href === routes.basket ? (
            <span
              className="mobile-conversion-bar__count"
              aria-label={`${itemCount} item${itemCount === 1 ? "" : "s"}`}
              aria-live="polite"
            >
              {itemCount}
            </span>
          ) : null}
        </Link>
      ))}
    </nav>
  );
}
