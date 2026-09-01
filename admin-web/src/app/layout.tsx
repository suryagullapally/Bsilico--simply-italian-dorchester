import type { Metadata } from "next";
import { Cormorant_Garamond, Manrope } from "next/font/google";
import { AuthProvider } from "@/components/auth/AuthProvider";
import { AdminShell } from "@/components/layout/AdminShell";
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
  title: "Basilico Admin",
  description: "Admin interface for Basilico restaurant operations.",
  robots: {
    index: false,
    follow: false,
    nocache: true,
  },
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="en-GB" className={`${manrope.variable} ${cormorantGaramond.variable}`}>
      <body>
        <AuthProvider>
          <AdminShell>{children}</AdminShell>
        </AuthProvider>
      </body>
    </html>
  );
}
