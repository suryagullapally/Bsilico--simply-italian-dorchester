import type { MetadataRoute } from "next";
import { SITE_NAME } from "@/lib/site-config";

export default function manifest(): MetadataRoute.Manifest {
  return {
    name: SITE_NAME,
    short_name: "Basilico",
    description:
      "Italian restaurant and sourdough pizza in Dorchester, Dorset.",
    start_url: "/",
    display: "standalone",
    background_color: "#10110f",
    theme_color: "#d87935",
    icons: [
      {
        src: "/icon.png",
        sizes: "512x512",
        type: "image/png",
      },
      {
        src: "/apple-icon.png",
        sizes: "180x180",
        type: "image/png",
      },
    ],
  };
}
