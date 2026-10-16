import React from "react";
import PackageBrowser from "@/components/PackageBrowser";

async function fetchPackages() {
  const res = await fetch(`${process.env.NEXT_PUBLIC_API_BASE_URL}/api/packages/all`, { cache: 'no-store' });
  if (!res.ok) throw new Error('Failed to fetch packages');
  return res.json();
}

export default async function PackagesPage() {
  const pkgs = await fetchPackages();
  return (
    <div className="section py-8">
      <h1 className="text-2xl font-bold tracking-tight mb-1">Packages</h1><p className="mb-6 text-sm text-slate-400">Ready-made weekends. Enroll, pay, and the itinerary PDF is emailed to you.</p>
      <PackageBrowser initial={pkgs} />
    </div>
  );
}
