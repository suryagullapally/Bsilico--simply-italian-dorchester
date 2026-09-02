import type { MetadataRoute } from "next";
import { siteUrl } from "@/lib/site-config";

const publicRoutes = [
  "/",
  "/menu",
  "/menu/create-your-own",
  "/book",
  "/privacy",
  "/allergens",
];

export default function sitemap(): MetadataRoute.Sitemap {
  const now = new Date();

  return publicRoutes.map((route) => ({
    url: siteUrl(route),
    lastModified: now,
    changeFrequency: route.startsWith("/menu") ? "daily" : "weekly",
    priority: route === "/" ? 1 : 0.7,
  }));
}
