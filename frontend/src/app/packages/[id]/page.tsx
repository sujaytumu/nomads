import React from "react";
import Link from "next/link";
import Cover from "@/components/Cover";
import { notFound } from "next/navigation";
import dynamic from "next/dynamic";

const EnrollButton = dynamic(() => import("@/components/EnrollButton"), { ssr: false });

async function fetchPackage(id: string) {
  const res = await fetch(`${process.env.NEXT_PUBLIC_API_BASE_URL}/api/packages/${id}`, { cache: 'no-store' });
  if (res.status === 404) return null;
  if (!res.ok) throw new Error('Failed to fetch package');
  return res.json();
}

export default async function PackageDetailPage({ params }: { params: { id: string } }) {
  const pkg = await fetchPackage(params.id);
  if (!pkg) return notFound();

  const places: any[] = pkg.places ?? [];
  const label = (c?: string) => (c ? c.charAt(0) + c.slice(1).toLowerCase() : "");

  return (
    <div className="section space-y-6 py-6 sm:py-10">
      <Link href="/packages" className="text-sm text-slate-400 hover:text-slate-200">← All packages</Link>

      <div className="relative overflow-hidden rounded-3xl">
        <Cover src={pkg.imageUrl} alt={pkg.name} kind="PACKAGE" className="h-52 sm:h-72" />
        <div className="absolute inset-x-0 bottom-0 bg-gradient-to-t from-black/60 to-transparent p-5 sm:p-8">
          <h1 className="font-display text-2xl font-semibold text-white sm:text-4xl">{pkg.name}</h1>
          {pkg.averageRating ? <p className="mt-1 text-sm font-semibold text-white/90">★ {Number(pkg.averageRating).toFixed(1)} average rating</p> : null}
        </div>
      </div>

      <div className="grid gap-6 lg:grid-cols-[1fr_320px] lg:items-start">
        <aside className="card space-y-4 p-5 lg:order-2 lg:sticky lg:top-6">
          <div>
            <p className="text-xs font-semibold uppercase tracking-wider text-slate-400">Price per person</p>
            <p className="font-display text-3xl font-semibold">₹{Number(pkg.price).toLocaleString("en-IN")}</p>
          </div>
          <EnrollButton packageId={pkg.id} amount={pkg.price} />
          <ul className="space-y-1.5 text-sm text-slate-400">
            <li>✓ Pay once, booked instantly</li>
            <li>✓ Itinerary PDF emailed to you</li>
            <li>✓ Pickup can be added later</li>
          </ul>
        </aside>

        <div className="space-y-5 lg:order-1">
          <p className="text-slate-300">{pkg.description}</p>
          <h2 className="font-display text-xl font-semibold">Places in this package</h2>
          <div className="grid gap-4 sm:grid-cols-2">
            {places.map((place: any) => (
              <div key={place.id} className="card overflow-hidden">
                <Cover src={place.imageUrl} alt={place.name} kind={place.category} className="h-32" />
                <div className="p-3">
                  <h3 className="truncate font-display text-base font-semibold">{place.name}</h3>
                  <p className="text-xs text-slate-400">
                    {[place.city, label(place.category), place.rating ? `★ ${Number(place.rating).toFixed(1)}` : null].filter(Boolean).join(" · ")}
                  </p>
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
}
