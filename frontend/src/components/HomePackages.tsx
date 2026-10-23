"use client";

import Link from "next/link";
import { useEffect, useState } from "react";

import PackageCard from "@/components/PackageCard";
import { getCached, peekCached } from "@/lib/api";

const URL_PATH = "/api/packages/homepage";

/**
 * Loaded in the browser instead of on the server: the page opens at once and these fill in.
 * Last visit's packages are shown immediately and quietly refreshed.
 */
export default function HomePackages() {
  const [packages, setPackages] = useState<any[] | null>(null);

  useEffect(() => {
    const saved = peekCached<any[]>(URL_PATH);
    if (saved) setPackages(saved);
    getCached<any[]>(URL_PATH, { ttl: 60000, persist: true })
      .then((list) => setPackages(list || []))
      .catch(() => setPackages((cur) => cur ?? []));
  }, []);

  return (
    <section className="section pb-10">
      <div className="mb-4">
        <h2 className="font-display text-xl font-semibold sm:text-2xl">Weekend packages</h2>
      </div>
      <div className="-mx-5 flex snap-x scroll-pl-5 gap-4 overflow-x-auto px-5 pb-3 md:mx-0 md:grid md:grid-cols-3 md:gap-6 md:overflow-visible md:px-0 [&>*]:w-[82%] [&>*]:shrink-0 [&>*]:snap-start md:[&>*]:w-auto">
        {packages === null
          ? [0, 1, 2].map((i) => <div key={i} className="card h-64 animate-pulse" />)
          : packages.map((p: any) => <PackageCard key={p.id} pkg={p} />)}
      </div>
      <div className="mt-4 text-right">
        <Link href="/packages" className="btn btn-secondary btn-sm">Explore more packages →</Link>
      </div>
    </section>
  );
}
