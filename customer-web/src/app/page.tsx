import type { Metadata } from "next";
import { HomePage } from "@/components/home/HomePage";
import { buildPageMetadata } from "@/lib/seo";
import { SITE_DESCRIPTION } from "@/lib/site-config";

export const metadata: Metadata = buildPageMetadata({
  title: "Basilico | Italian Restaurant & Pizza in Dorchester",
  description: SITE_DESCRIPTION,
  path: "/",
});

export default function Home() {
  return <HomePage />;
}
