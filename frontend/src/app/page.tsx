import HomeDashboard from "@/components/HomeDashboard";
import PickedForYou from "@/components/PickedForYou";
import PackageCard from "@/components/PackageCard";


async function fetchHomepagePackages() {
  const res = await fetch(`${process.env.NEXT_PUBLIC_API_BASE_URL}/api/packages/homepage`, { cache: 'no-store' });
  if (!res.ok) return [];
  return res.json();
}

export default async function HomePage() {
  return (
    <div>
      <HomeDashboard />
      <PickedForYou />
      <section className="section pb-16">
        <div className="mb-6">
          <h2 className="font-display text-2xl font-semibold">Weekend packages</h2>
          <p className="text-sm text-slate-400">Ready-made weekends. Enroll, pay, and your itinerary PDF lands in your inbox.</p>
        </div>
        <div className="-mx-5 flex snap-x scroll-pl-5 gap-4 overflow-x-auto px-5 pb-3 md:mx-0 md:grid md:grid-cols-3 md:gap-6 md:overflow-visible md:px-0 [&>*]:w-[82%] [&>*]:shrink-0 [&>*]:snap-start md:[&>*]:w-auto">
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
