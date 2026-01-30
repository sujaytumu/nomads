import React from "react";
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

  return (
    <div className="section py-12">
      <div className="grid md:grid-cols-3 gap-6">
        <div className="col-span-2">
          <h1 className="text-3xl font-bold">{pkg.name}</h1>
          <p className="text-slate-600 mt-2">{pkg.description}</p>
          <div className="mt-6 space-y-4">
            <h3 className="text-xl font-semibold">Places in this package</h3>
            {pkg.places.map((place: any) => (
              <div key={place.id} className="card p-4 mb-2">
                <div className="flex items-center justify-between">
                  <div>
                    <h4 className="font-semibold">{place.name}</h4>
                    <div className="text-sm text-slate-500">{place.city} • Rating: {place.rating ?? 'N/A'}</div>
                  </div>
                </div>
                <p className="text-sm mt-2">Activities: Visit, Explore local cuisine, Photo stops</p>
              </div>
            ))}
          </div>
        </div>
        <aside className="space-y-4">
          <div className="card p-4">
            <div className="text-2xl font-bold">₹{pkg.price}</div>
            {/* EnrollButton will POST to enroll and redirect to payment */}
            <div className="mt-4">
              <EnrollButton packageId={pkg.id} amount={pkg.price} />
            </div>
          </div>
          <div className="card p-4">
            <h4 className="font-semibold">Average Rating</h4>
            <div className="text-xl">{pkg.averageRating?.toFixed(1) ?? 'N/A'}</div>
          </div>
        </aside>
      </div>
    </div>
  );
}
