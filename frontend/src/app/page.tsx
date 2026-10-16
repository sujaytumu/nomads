import dynamic from "next/dynamic";
import HomeDashboard from "@/components/HomeDashboard";
import PackageCard from "@/components/PackageCard";

const PlacesList = dynamic(() => import("@/components/PlacesList"), { ssr: false });

async function fetchHomepagePackages() {
  const res = await fetch(`${process.env.NEXT_PUBLIC_API_BASE_URL}/api/packages/homepage`, { cache: 'no-store' });
  if (!res.ok) return [];
  return res.json();
}

export default async function HomePage() {
  return (
    <div>
      <HomeDashboard />
      <section className="section pb-16">
        <div className="mb-6">
          <h3 className="text-2xl font-semibold">Nearby Attractions</h3>
          <p className="text-sm text-slate-300">Places near you — explore and add to your tour.</p>
        </div>
        <PlacesList />
      </section>

      <section className="section pb-16">
        <div className="mb-6">
          <h3 className="text-2xl font-semibold">Featured Weekend Packages</h3>
          <p className="text-sm text-slate-300">Choose a curated weekend package and enroll instantly.</p>
        </div>
        <div className="grid md:grid-cols-3 gap-6">
          {
            (await fetchHomepagePackages()).map((p: any) => (
              <PackageCard key={p.id} pkg={p} />
            ))
          }
        </div>
      <div className="mt-4 text-right">
        <a href="/packages" className="btn btn-secondary">Explore more packages</a>
      </div>
      </section>
    </div>
  );
}
