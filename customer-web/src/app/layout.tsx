import type { Metadata } from "next";
import { Cormorant_Garamond, Manrope } from "next/font/google";
import { CartProvider } from "@/components/cart/CartProvider";
import { SplashScreen } from "@/components/splash/SplashScreen";
import { SITE_DESCRIPTION, SITE_NAME, SITE_URL } from "@/lib/site-config";
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
  alternates: {
    canonical: "/",
  },
  openGraph: {
    title: "Basilico | Simple Italian | Dorchester",
    description: SITE_DESCRIPTION,
    siteName: SITE_NAME,
    type: "website",
    url: "/",
    locale: "en_GB",
  },
  robots: {
    index: true,
    follow: true,
  },
};

export default function RootLayout({ children }: LayoutProps<"/">) {
  return (
    <html
      lang="en-GB"
      data-scroll-behavior="smooth"
      className={`${manrope.variable} ${cormorantGaramond.variable} h-full antialiased`}
    >
      <body className="min-h-full">
        <CartProvider>
          <div id="site-content">{children}</div>
          <SplashScreen />
        </CartProvider>
      </body>
    </html>
  );
}
