import type { Metadata, Viewport } from "next";
import { Fraunces, Plus_Jakarta_Sans } from "next/font/google";
import "./globals.css";

import AppShell from "@/components/AppShell";

const body = Plus_Jakarta_Sans({ subsets: ["latin"], variable: "--font-body", display: "swap" });
const display = Fraunces({ subsets: ["latin"], variable: "--font-display", display: "swap" });

const apiOrigin = process.env.NEXT_PUBLIC_API_BASE_URL;

export const viewport: Viewport = { themeColor: "#020617" };

export const metadata: Metadata = {
  manifest: "/manifest.webmanifest",
  icons: { icon: "/icon-192.png", apple: "/apple-touch-icon.png" },
  appleWebApp: { capable: true, title: "NOMAD", statusBarStyle: "black-translucent" },
  title: "NOMAD – Smart Weekend Travel",
  description: "Plan weekend city trips with smart recommendations, assistance, and payments.",
};

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="en" className={`${body.variable} ${display.variable}`}>
      <head>
        {/* Start the connection to the API while the page is still loading */}
        {apiOrigin && <link rel="preconnect" href={apiOrigin} crossOrigin="anonymous" />}
        {apiOrigin && <link rel="dns-prefetch" href={apiOrigin} />}
      </head>
      <body>
        <AppShell>{children}</AppShell>
      </body>
    </html>
  );
}
