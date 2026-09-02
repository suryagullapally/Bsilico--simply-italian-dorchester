import type { Metadata } from "next";
import { redirect } from "next/navigation";
import { routes } from "@/lib/routes";
import { buildPageMetadata } from "@/lib/seo";

export const metadata: Metadata = buildPageMetadata({
  title: "Order Online | Basilico Dorchester",
  description: "Order Basilico Italian food and pizza online in Dorchester.",
  path: "/order",
  noindex: true,
});

export default function OrderRedirectPage() {
  redirect(routes.menu);
}
