import type { Metadata } from "next";
import { Cormorant_Garamond, Manrope } from "next/font/google";
import { CartProvider } from "@/components/cart/CartProvider";
import { SplashScreen } from "@/components/splash/SplashScreen";
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
  title: "Basilico | Simple Italian | Dorchester",
  description:
    "Basilico is an Italian restaurant serving pizza and Italian food in Dorchester, Dorset.",
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
