import Link from "next/link";

const COLUMNS: { title: string; links: { href: string; label: string }[] }[] = [
  {
    title: "Explore",
    links: [
      { href: "/", label: "Home" },
      { href: "/packages", label: "Packages" },
      { href: "/map", label: "Map" },
    ],
  },
  {
    title: "Trips",
    links: [
      { href: "/trip-planner", label: "Plan a trip" },
      { href: "/bookings", label: "My Trips" },
      { href: "/wishlist", label: "Wishlist" },
      { href: "/route-view", label: "Route viewer" },
    ],
  },
  {
    title: "Account",
    links: [
      { href: "/auth", label: "Log in" },
      { href: "/auth?mode=register", label: "Sign up" },
      { href: "/profile", label: "Profile" },
    ],
  },
];

export default function Footer() {
  return (
    <footer className="mt-16 border-t border-slate-800 bg-slate-900">
      <div className="section py-10">
        <div className="grid gap-8 md:grid-cols-[1.6fr_repeat(3,1fr)]">
          <div className="space-y-2">
            <p className="text-lg font-bold text-brand-700">NOMAD</p>
            <p className="max-w-xs text-sm text-slate-400">
              Smart weekend city trips with recommendations, on-trip assistance and payments in one place.
            </p>
          </div>

          {COLUMNS.map((col) => (
            <nav key={col.title} aria-label={col.title}>
              <p className="mb-3 text-xs font-semibold uppercase tracking-wide text-slate-500">
                {col.title}
              </p>
              <ul className="space-y-2">
                {col.links.map((link) => (
                  <li key={link.href}>
                    <Link href={link.href} className="text-sm text-slate-300 transition-colors hover:text-white">
                      {link.label}
                    </Link>
                  </li>
                ))}
              </ul>
            </nav>
          ))}
        </div>

        <div className="mt-8 flex flex-col gap-2 border-t border-slate-800 pt-6 text-sm text-slate-500 sm:flex-row sm:items-center sm:justify-between">
          <p>© 2026 NOMAD by Tripfactory Internship Travelathon</p>
          <p>Smart Weekend Travel &amp; Assistance</p>
        </div>
      </div>
    </footer>
  );
}
