import type { MetadataRoute } from "next";
import { SITE_URL, siteUrl } from "@/lib/site-config";

const publicRoutes = ["/", "/menu", "/book", "/privacy", "/allergens"];

export default function sitemap(): MetadataRoute.Sitemap {
  const now = new Date();

  return publicRoutes.map((route) => ({
    url: route === "/" ? SITE_URL : siteUrl(route),
    lastModified: now,
    changeFrequency: route === "/menu" ? "daily" : "weekly",
    priority: route === "/" ? 1 : 0.7,
  }));
}
