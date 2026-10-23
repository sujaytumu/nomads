"use client";

import { useEffect, useState } from "react";

import PackageBrowser from "@/components/PackageBrowser";
import { getCached, peekCached } from "@/lib/api";

const URL_PATH = "/api/packages/all";

// Loaded in the browser: the page opens at once, shows last visit's packages while fresh ones load,
// and no longer waits on a sleeping server before showing anything.
export default function PackagesPage() {
  const [pkgs, setPkgs] = useState<any[] | null>(null);
  const [failed, setFailed] = useState(false);

  useEffect(() => {
    const saved = peekCached<any[]>(URL_PATH);
    if (saved) setPkgs(saved);
    getCached<any[]>(URL_PATH, { ttl: 60000, persist: true })
      .then((list) => setPkgs(list || []))
      .catch(() => setFailed(true));
  }, []);

  return (
    <div className="section py-8">
      <h1 className="mb-5 text-2xl font-semibold tracking-tight sm:text-3xl">Packages</h1>
      {pkgs ? (
        <PackageBrowser initial={pkgs} />
      ) : failed ? (
        <div className="card space-y-2 p-5">
          <p className="text-sm text-red-400">Couldn&apos;t load packages.</p>
          <button type="button" className="btn btn-secondary btn-sm" onClick={() => window.location.reload()}>Try again</button>
        </div>
      ) : (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3" aria-busy="true">
          {[0, 1, 2].map((i) => <div key={i} className="card h-72 animate-pulse" />)}
        </div>
      )}
    </div>
  );
}
