"use client";

import Link from "next/link";
import { useCart } from "@/components/cart/CartProvider";
import { routes } from "@/lib/routes";

export function CartHeaderLink() {
  const { itemCount } = useCart();

  return (
    <Link
      className="home-header__basket"
      href={routes.basket}
      aria-label={`Basket with ${itemCount} item${itemCount === 1 ? "" : "s"}`}
    >
      <span>Basket</span>
      <span className="home-header__basket-count">{itemCount}</span>
    </Link>
  );
}
