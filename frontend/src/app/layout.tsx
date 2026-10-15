import type { Metadata } from "next";
import "./globals.css";
import "leaflet/dist/leaflet.css";

import TopNav from "@/components/TopNav";
import Footer from "@/components/Footer";

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
        <div className="min-h-screen">
          <header className="sticky top-0 z-40 border-b border-slate-800 bg-slate-900/95 backdrop-blur">
            <div className="section flex h-16 items-center justify-between">
              <a href="/" className="leading-tight">
                <span className="block text-xl font-bold text-brand-700">NOMAD</span>
                <span className="hidden text-xs text-slate-400 sm:block">Smart Weekend Travel & Assistance</span>
              </a>
              <TopNav />
            </div>
          </header>
          <main>{children}</main>
          <Footer />
        </div>
      </body>
    </html>
  );
}
