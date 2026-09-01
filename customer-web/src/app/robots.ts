import type { MetadataRoute } from "next";
import { SITE_URL } from "@/lib/site-config";

export default function robots(): MetadataRoute.Robots {
  return {
    rules: {
      userAgent: "*",
      allow: ["/", "/menu", "/book", "/privacy", "/allergens"],
      disallow: ["/basket", "/checkout", "/checkout/payment", "/order"],
    },
    sitemap: `${SITE_URL}/sitemap.xml`,
  };
}
