import type { Metadata } from "next";
import { Cormorant_Garamond, Manrope } from "next/font/google";
import { CartProvider } from "@/components/cart/CartProvider";
import { SplashScreen } from "@/components/splash/SplashScreen";
import {
  BASILICO_OG_IMAGE_PATH,
  SITE_DESCRIPTION,
  SITE_NAME,
  SITE_URL,
  siteUrl,
} from "@/lib/site-config";
import { restaurantJsonLd } from "@/lib/structured-data";
import "./globals.css";

const manrope = Manrope({
  variable: "--font-basilico-sans",
  subsets: ["latin"],
  display: "swap",
});

const cormorantGaramond = Cormorant_Garamond({
  variable: "--font-basilico-display",
  subsets: ["latin"],
  weight: "variable",
  display: "swap",
});

export const metadata: Metadata = {
  metadataBase: new URL(SITE_URL),
  title: {
    default: "Basilico | Simple Italian | Dorchester",
    template: "%s | Basilico Dorchester",
  },
  description: SITE_DESCRIPTION,
  openGraph: {
    title: "Basilico | Italian Restaurant & Pizza in Dorchester",
    description: SITE_DESCRIPTION,
    siteName: SITE_NAME,
    type: "website",
    url: SITE_URL,
    locale: "en_GB",
    images: [
      {
        url: siteUrl(BASILICO_OG_IMAGE_PATH),
        width: 1600,
        height: 893,
        alt: "Basilico Italian restaurant and pizza in Dorchester",
      },
    ],
  },
  twitter: {
    card: "summary_large_image",
    title: "Basilico | Italian Restaurant & Pizza in Dorchester",
    description: SITE_DESCRIPTION,
    images: [siteUrl(BASILICO_OG_IMAGE_PATH)],
  },
  icons: {
    icon: [
      { url: "/favicon.ico" },
      { url: "/icon.png", type: "image/png", sizes: "512x512" },
    ],
    apple: [{ url: "/apple-icon.png", type: "image/png", sizes: "180x180" }],
  },
  manifest: "/manifest.webmanifest",
  robots: {
    index: true,
    follow: true,
  },
};

export default function RootLayout({ children }: LayoutProps<"/">) {
  const jsonLd = restaurantJsonLd();

  return (
    <html
      lang="en-GB"
      data-scroll-behavior="smooth"
      className={`${manrope.variable} ${cormorantGaramond.variable} h-full antialiased`}
    >
      <body className="min-h-full">
        <CartProvider>
          <script
            type="application/ld+json"
            dangerouslySetInnerHTML={{ __html: JSON.stringify(jsonLd) }}
          />
          <div id="site-content">{children}</div>
          <SplashScreen />
        </CartProvider>
      </body>
    </html>
  );
}

