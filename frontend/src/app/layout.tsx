import type { Metadata } from "next";
import "./globals.css";
import "leaflet/dist/leaflet.css";

import AppShell from "@/components/AppShell";

export const metadata: Metadata = {
  title: "NOMAD – Smart Weekend Travel",
  description: "Plan weekend city trips with smart recommendations, assistance, and payments.",
};

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="en">
      <body>
        <AppShell>{children}</AppShell>
      </body>
    </html>
  );
}
