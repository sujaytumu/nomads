"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { useCallback, useEffect, useState } from "react";
import { api } from "@/lib/api";
import { fetchMe } from "@/lib/authApi";
import InstallApp from "@/components/InstallApp";

type User = { id: number; email: string; name?: string; role?: string };
type Item = { href: string; label: string; icon: string; badge?: boolean };
type Group = { title: string; items: Item[] };

// Every feature has a fixed place here. The list never changes with the page,
// so nothing moves when you click.
const GROUPS: Group[] = [
  {
    title: "Discover",
    items: [
      { href: "/", label: "Home", icon: "M3 11l9-8 9 8M5 10v10h5v-6h4v6h5V10" },
      { href: "/packages", label: "Packages", icon: "M4 8l8-4 8 4-8 4-8-4zM4 8v8l8 4 8-4V8" },
      { href: "/map", label: "Map", icon: "M9 4l-6 2v14l6-2 6 2 6-2V4l-6 2-6-2zM9 4v14M15 6v14" },
      { href: "/wishlist", label: "Wishlist", icon: "M12 21s-8-5.4-8-11a4.5 4.5 0 0 1 8-2.8A4.5 4.5 0 0 1 20 10c0 5.6-8 11-8 11z" },
    ],
  },
  {
    title: "Your trips",
    items: [
      { href: "/trip-planner", label: "Plan a trip", icon: "M12 5v14M5 12h14" },
      { href: "/bookings", label: "My Trips", icon: "M4 7h16v12H4zM9 7V4h6v3", badge: true },
      { href: "/trip-summary", label: "Itinerary", icon: "M8 6h12M8 12h12M8 18h12M4 6h.01M4 12h.01M4 18h.01" },
      { href: "/route-view", label: "Route", icon: "M5 19a2 2 0 1 0 0-4 2 2 0 0 0 0 4zM19 9a2 2 0 1 0 0-4 2 2 0 0 0 0 4zM7 17c6 0 2-10 10-10" },
      { href: "/payment", label: "Payment", icon: "M3 7h18v12H3zM3 11h18" },
      { href: "/pickup", label: "Pickup", icon: "M3 15V9l2-4h14l2 4v6M3 15h18M7 18a1 1 0 1 0 0-2 1 1 0 0 0 0 2zM17 18a1 1 0 1 0 0-2 1 1 0 0 0 0 2z" },
      { href: "/review", label: "Reviews", icon: "M12 3l2.7 5.6 6.1.9-4.4 4.3 1 6.1L12 17l-5.4 2.9 1-6.1L3.2 9.5l6.1-.9z" },
    ],
  },
];

const ADMIN_GROUP: Group = {
  title: "Admin",
  items: [
    { href: "/admin", label: "Admin", icon: "M12 3l8 3v6c0 5-3.5 8-8 9-4.5-1-8-4-8-9V6z" },
    { href: "/group-status", label: "Groups", icon: "M8 11a3 3 0 1 0 0-6 3 3 0 0 0 0 6zM16 11a3 3 0 1 0 0-6 3 3 0 0 0 0 6zM2 20c0-3 3-5 6-5s6 2 6 5M14 15c3 0 8 1 8 5" },
  ],
};

// Pages that work without signing in. Everything else sends you to /auth first.
const isPublic = (path: string) => path === "/auth" || path.startsWith("/share/");

// The four things people do most. Everything else is under "More".
const TABS: Item[] = [
  { href: "/", label: "Home", icon: "M3 11l9-8 9 8M5 10v10h5v-6h4v6h5V10" },
  { href: "/trip-planner", label: "Plan", icon: "M12 5v14M5 12h14" },
  { href: "/bookings", label: "My Trips", icon: "M4 7h16v12H4zM9 7V4h6v3", badge: true },
];

function active(path: string, href: string) {
  return href === "/" ? path === "/" : path === href || path.startsWith(href + "/");
}

function Icon({ d }: { d: string }) {
  return (
    <svg aria-hidden="true" viewBox="0 0 24 24" className="h-[18px] w-[18px] shrink-0" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
      <path d={d} />
    </svg>
  );
}

export default function AppShell({ children }: { children: React.ReactNode }) {
  const path = usePathname() ?? "/";
  const router = useRouter();
  const [authState, setAuthState] = useState<"loading" | "out" | "in">("loading");
  const [user, setUser] = useState<User | null>(null);
  const [open, setOpen] = useState(false);
  const [needsPayment, setNeedsPayment] = useState(0);

  const load = useCallback(() => {
    if (!localStorage.getItem("nomad_token")) {
      setUser(null);
      setAuthState("out");
      return;
    }
    setAuthState("in");
    fetchMe()
      .then((d) => setUser({ id: d.id, email: d.email, name: d.name, role: d.role }))
      .catch((err) => {
        const s = err?.response?.status;
        if (s === 401 || s === 403) {
          localStorage.removeItem("nomad_token");
          setUser(null);
          setAuthState("out");
        }
      });
    api
      .get("/api/trips/me")
      .then((r) =>
        setNeedsPayment((r.data || []).filter((t: any) => t.status === "PLANNED" || t.status === "PAYMENT_PENDING").length)
      )
      .catch(() => undefined);
  }, []);

  useEffect(() => {
    load();
    const onStorage = (e: StorageEvent) => e.key === "nomad_token" && load();
    window.addEventListener("storage", onStorage);
    return () => window.removeEventListener("storage", onStorage);
  }, [load, path]);

  useEffect(() => setOpen(false), [path]);

  // Signed out: the only place to be is the login/register page.
  useEffect(() => {
    if (authState === "out" && !isPublic(path)) router.replace("/auth");
  }, [authState, path, router]);

  const logout = () => {
    localStorage.removeItem("nomad_token");
    window.location.href = "/";
  };

  const groups = user?.role === "ADMIN" ? [...GROUPS, ADMIN_GROUP] : GROUPS;
  const name = user?.name || user?.email || "Account";

  const nav = (
    <nav aria-label="Main" className="flex-1 space-y-5 overflow-y-auto px-3 py-4">
      {groups.map((g) => (
        <div key={g.title}>
          <p className="mb-1 px-3 text-[11px] font-semibold uppercase tracking-wider text-slate-500">{g.title}</p>
          <ul className="space-y-0.5">
            {g.items.map((it) => (
              <li key={it.href}>
                <Link href={it.href} aria-current={active(path, it.href) ? "page" : undefined} className={`side-link ${active(path, it.href) ? "side-link-active" : ""}`}>
                  <Icon d={it.icon} />
                  <span className="flex-1">{it.label}</span>
                  {it.badge && needsPayment > 0 && (
                    <span title="Trips waiting for payment" className="rounded-full bg-amber-400/20 px-2 text-xs font-bold text-amber-300">{needsPayment}</span>
                  )}
                </Link>
              </li>
            ))}
          </ul>
        </div>
      ))}
    </nav>
  );

  const account = (
    <div className="space-y-2 border-t border-slate-800 p-3">
      <InstallApp />
      {authState === "in" ? (
        <div className="space-y-2">
          <Link href="/profile" className={`side-link ${active(path, "/profile") ? "side-link-active" : ""}`}>
            <span className="grid h-7 w-7 shrink-0 place-items-center rounded-full bg-brand-600 text-xs font-bold text-white">{name.trim().charAt(0).toUpperCase()}</span>
            <span className="min-w-0 flex-1">
              <span className="block truncate text-sm">{name}</span>
              <span className="block truncate text-xs font-normal text-slate-500">Profile &amp; settings</span>
            </span>
          </Link>
          <button type="button" onClick={logout} className="btn btn-ghost btn-sm w-full">Log out</button>
        </div>
      ) : authState === "out" ? (
        <div className="grid grid-cols-2 gap-2">
          <Link href="/auth" className="btn btn-secondary btn-sm">Log in</Link>
          <Link href="/auth?mode=register" className="btn btn-primary btn-sm">Sign up</Link>
        </div>
      ) : (
        <div className="h-9 animate-pulse rounded-lg bg-slate-800/60" />
      )}
    </div>
  );

  const brand = (
    <Link href="/" className="flex h-14 items-center px-6 text-xl font-bold tracking-wide text-brand-700">NOMAD</Link>
  );

  // Not signed in: no menus, just the page (login/register) on its own.
  if (authState !== "in") {
    if (authState === "out" && isPublic(path)) return <main className="min-h-screen">{children}</main>;
    return (
      <div className="grid min-h-screen place-items-center text-sm text-slate-500">
        <span className="text-xl font-bold tracking-wide text-brand-700">NOMAD</span>
      </div>
    );
  }

  const tabClass = (on: boolean) =>
    `relative flex flex-1 flex-col items-center gap-0.5 py-2 text-[11px] font-semibold transition-colors ${on ? "text-white" : "text-slate-400"}`;

  return (
    <div className="min-h-screen pb-16 lg:pb-0 lg:pl-64">
      {/* Desktop sidebar: fixed size, always the same options */}
      <aside className="fixed inset-y-0 left-0 z-40 hidden w-64 flex-col border-r border-slate-800 bg-slate-900 lg:flex">
        {brand}
        {nav}
        {account}
      </aside>

      {/* Mobile: slim top bar */}
      <header className="sticky top-0 z-30 flex h-12 items-center justify-between border-b border-slate-800 bg-slate-900/95 px-4 backdrop-blur lg:hidden">
        <Link href="/" className="text-lg font-bold tracking-wide text-brand-700">NOMAD</Link>
        <Link href="/profile" aria-label="Profile" className="grid h-8 w-8 place-items-center rounded-full bg-brand-600 text-xs font-bold text-white">
          {name.trim().charAt(0).toUpperCase()}
        </Link>
      </header>

      {/* Mobile: thumb-reach tab bar. The last tab opens everything else. */}
      <nav aria-label="Quick" className="fixed inset-x-0 bottom-0 z-40 flex border-t border-slate-800 bg-slate-900/95 pb-[env(safe-area-inset-bottom)] backdrop-blur lg:hidden">
        {TABS.map((it) => (
          <Link key={it.href} href={it.href} aria-current={active(path, it.href) ? "page" : undefined} className={tabClass(active(path, it.href))}>
            <Icon d={it.icon} />
            {it.label}
            {it.badge && needsPayment > 0 && <span className="absolute right-[28%] top-1 h-2 w-2 rounded-full bg-amber-400" />}
          </Link>
        ))}
        <button type="button" aria-expanded={open} onClick={() => setOpen((v) => !v)} className={tabClass(open)}>
          <Icon d={open ? "M6 6l12 12M18 6L6 18" : "M4 7h16M4 12h16M4 17h16"} />
          {open ? "Close" : "More"}
        </button>
      </nav>

      {/* Mobile "More" sheet */}
      {open && (
        <div className="fixed inset-x-0 bottom-0 top-12 z-30 flex flex-col bg-slate-900 pb-[calc(3.25rem+env(safe-area-inset-bottom))] lg:hidden">
          {nav}
          {account}
        </div>
      )}

      <main className="min-h-[calc(100vh-3rem)]">{children}</main>
      <footer className="hidden border-t border-slate-800 px-6 py-4 text-xs text-slate-500 lg:block">
        © 2026 NOMAD · Smart Weekend Travel &amp; Assistance
      </footer>
    </div>
  );
}
