import type { Metadata } from "next";
import "./globals.css";
import "leaflet/dist/leaflet.css";

import TopNav from "@/components/TopNav";

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
          <header className="bg-slate-900 border-b border-slate-800">
            <div className="section flex items-center justify-between py-5">
              <div>
                <h1 className="text-xl font-bold text-brand-700">NOMAD</h1>
                <p className="text-sm text-slate-400">Smart Weekend Travel & Assistance</p>
              </div>
              <TopNav />
            </div>
          </header>
          <main>{children}</main>
          <footer className="border-t border-slate-800 bg-slate-900 mt-16">
            <div className="section py-8 text-sm text-slate-400">
              © 2026 NOMAD by Tripfactory Internship Travelathon
            </div>
          </footer>
        </div>
      </body>
    </html>
  );
}
