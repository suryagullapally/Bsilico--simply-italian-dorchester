import type { Metadata } from "next";
import {
  BASILICO_OG_IMAGE_ALT,
  BASILICO_OG_IMAGE_PATH,
  SITE_NAME,
  siteUrl,
} from "@/lib/site-config";

type PageMetadataOptions = {
  description: string;
  imagePath?: string;
  noindex?: boolean;
  path: string;
  title: string;
};

export function buildPageMetadata({
  description,
  imagePath = BASILICO_OG_IMAGE_PATH,
  noindex = false,
  path,
  title,
}: PageMetadataOptions): Metadata {
  const canonicalUrl = siteUrl(path);
  const imageUrl = siteUrl(imagePath);

  return {
    title: {
      absolute: title,
    },
    description,
    alternates: noindex
      ? undefined
      : {
          canonical: canonicalUrl,
        },
    openGraph: {
      title,
      description,
      siteName: SITE_NAME,
      type: "website",
      url: canonicalUrl,
      locale: "en_GB",
      images: [
        {
          url: imageUrl,
          width: 1600,
          height: 893,
          alt: BASILICO_OG_IMAGE_ALT,
        },
      ],
    },
    twitter: {
      card: "summary_large_image",
      title,
      description,
      images: [imageUrl],
    },
    robots: noindex
      ? {
          index: false,
          follow: false,
          googleBot: {
            index: false,
            follow: false,
          },
        }
      : {
          index: true,
          follow: true,
          googleBot: {
            index: true,
            follow: true,
            "max-image-preview": "large",
            "max-snippet": -1,
            "max-video-preview": -1,
          },
        },
  };
}
