"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { useCallback, useEffect, useRef, useState } from "react";
import { clearCache, getCached, peekCached } from "@/lib/api";
import { fetchMe, type Me } from "@/lib/authApi";
import { AuthContext } from "@/lib/auth-context";
import InstallApp from "@/components/InstallApp";
import NetworkStatus from "@/components/NetworkStatus";
import Reminders from "@/components/Reminders";
import Toaster from "@/components/Toaster";
import UpdateButton from "@/components/UpdateButton";
import { useAppUpdate } from "@/lib/useAppUpdate";
import { saveOfflineTrips } from "@/lib/offlineTrips";
import { warmUpBackend } from "@/lib/warmup";

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
      { href: "/group-status", label: "Group", icon: "M8 11a3 3 0 1 0 0-6 3 3 0 0 0 0 6zM16 11a3 3 0 1 0 0-6 3 3 0 0 0 0 6zM2 20c0-3 3-5 6-5s6 2 6 5M14 15c3 0 8 1 8 5" },
      { href: "/pickup", label: "Pickup", icon: "M3 15V9l2-4h14l2 4v6M3 15h18M7 18a1 1 0 1 0 0-2 1 1 0 0 0 0 2zM17 18a1 1 0 1 0 0-2 1 1 0 0 0 0 2z" },
      { href: "/review", label: "Reviews", icon: "M12 3l2.7 5.6 6.1.9-4.4 4.3 1 6.1L12 17l-5.4 2.9 1-6.1L3.2 9.5l6.1-.9z" },
    ],
  },
];

const ADMIN_GROUP: Group = {
  title: "Admin",
  items: [
    { href: "/admin", label: "Admin", icon: "M12 3l8 3v6c0 5-3.5 8-8 9-4.5-1-8-4-8-9V6z" },
  ],
};

// Pages that work without signing in. Everything else sends you to /auth first.
const isPublic = (path: string) => path === "/auth" || path.startsWith("/share/");

// Each phone tab owns a group of pages. Anything not listed belongs to "More".
type Section = "home" | "plan" | "trips" | "more";
const SECTION_PATHS: Record<Exclude<Section, "more">, string[]> = {
  home: ["/", "/packages", "/place"],
  plan: ["/trip-planner"],
  trips: ["/bookings", "/trip-summary", "/route-view", "/payment", "/group-status", "/pickup"],
};
function sectionOf(path: string): Section {
  if (path === "/") return "home";
  for (const key of Object.keys(SECTION_PATHS) as Exclude<Section, "more">[]) {
    if (SECTION_PATHS[key].some((p) => p !== "/" && (path === p || path.startsWith(p + "/")))) return key;
  }
  return "more";
}
// Pages that shouldn't be reopened later just by tapping a tab.
const NOT_REMEMBERED = new Set(["/payment", "/auth"]);

// The three things people do most. Everything else is under "More".
const TABS: (Item & { section: Exclude<Section, "more"> })[] = [
  { href: "/", label: "Home", section: "home", icon: "M3 11l9-8 9 8M5 10v10h5v-6h4v6h5V10" },
  { href: "/trip-planner", label: "Plan", section: "plan", icon: "M12 5v14M5 12h14" },
  { href: "/bookings", label: "My Trips", section: "trips", icon: "M4 7h16v12H4zM9 7V4h6v3", badge: true },
];
const MORE_ICON = "M4 7h16M4 12h16M4 17h16";
const LAST_KEY = "nomad_tab_last";

function Logo({ className = "" }: { className?: string }) {
  return (
    <span className={`inline-flex items-center gap-2 ${className}`}>
      <span className="grid h-8 w-8 place-items-center rounded-xl bg-gradient-to-br from-brand-500 to-ocean-400 text-white shadow-md shadow-brand-600/30">
        <svg aria-hidden="true" viewBox="0 0 24 24" className="h-[18px] w-[18px]" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round">
          <circle cx="12" cy="12" r="9" />
          <path d="M15.5 8.5l-2 5-5 2 2-5z" fill="currentColor" />
        </svg>
      </span>
      <span className="font-display text-xl font-semibold tracking-tight text-slate-100">Nomad</span>
    </span>
  );
}

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
  const [connectError, setConnectError] = useState(false);
  const [user, setUser] = useState<Me | null>(null);
  const [open, setOpen] = useState(false);
  const [needsPayment, setNeedsPayment] = useState(0);
  // Last page opened inside each tab, so coming back to a tab lands where you left it.
  const [last, setLast] = useState<Partial<Record<Section, string>>>({});
  const section = sectionOf(path);
  const updateReady = useAppUpdate();
  const mainRef = useRef<HTMLElement | null>(null);
  const slideRef = useRef<"next" | "prev" | null>(null);
  // Which way the new page slides in: decided by where its tab sits in Home > Plan > My Trips > More,
  // so tapping a tab slides like a swipe does. Pages inside the same tab just fade up.
  const navRef = useRef<{ path: string; section: Section; dir: "next" | "prev" | null }>({ path, section, dir: null });
  if (navRef.current.path !== path) {
    const order: Section[] = ["home", "plan", "trips", "more"];
    const from = order.indexOf(navRef.current.section);
    const to = order.indexOf(section);
    navRef.current = { path, section, dir: slideRef.current ?? (from === to ? null : to > from ? "next" : "prev") };
  }
  const live = useRef({ section, open, last, path, authState });
  live.current = { section, open, last, path, authState };

  useEffect(() => warmUpBackend(), []);

  // After a new release, a screen still running the old version can fail to load a page's code
  // (the tap does nothing). Reload once to pick up the new version instead of staying stuck.
  useEffect(() => {
    const stale = /ChunkLoadError|Loading chunk|Loading CSS chunk|dynamically imported module|Importing a module script failed/i;
    const recover = () => {
      try {
        if (sessionStorage.getItem("nomad_reloaded")) return;
        sessionStorage.setItem("nomad_reloaded", "1");
        setTimeout(() => sessionStorage.removeItem("nomad_reloaded"), 15000);
      } catch {
        /* ignore */
      }
      window.location.reload();
    };
    const onError = (e: ErrorEvent) => stale.test(e.message || "") && recover();
    const onReject = (e: PromiseRejectionEvent) => stale.test(String((e.reason && (e.reason.message || e.reason.name)) || e.reason || "")) && recover();
    window.addEventListener("error", onError);
    window.addEventListener("unhandledrejection", onReject);
    return () => {
      window.removeEventListener("error", onError);
      window.removeEventListener("unhandledrejection", onReject);
    };
  }, []);

  // Tabs must always open: if the in-app navigation hasn't landed shortly, load the page directly.
  const openTab = (e: React.MouseEvent, href: string) => {
    setOpen(false);
    if (e.metaKey || e.ctrlKey || e.shiftKey || e.button !== 0) return;
    const target = new URL(href, window.location.origin);
    try { navigator.vibrate?.(8); } catch { /* not supported */ }
    // Tapping the tab you are already on scrolls back to the top.
    if (target.pathname === window.location.pathname) window.scrollTo({ top: 0, behavior: "smooth" });
    window.setTimeout(() => {
      if (window.location.pathname !== target.pathname) window.location.assign(target.pathname + target.search);
    }, 900);
  };

  // Click ripple: any button-styled element gets a soft wave from the tap point.
  useEffect(() => {
    if (window.matchMedia?.("(prefers-reduced-motion: reduce)").matches) return;
    const onDown = (e: PointerEvent) => {
      const el = (e.target as HTMLElement | null)?.closest<HTMLElement>(".btn, .btn-primary, .btn-secondary, .btn-outline, .btn-ghost, .btn-danger");
      if (!el || (el as HTMLButtonElement).disabled) return;
      const r = el.getBoundingClientRect();
      const size = Math.max(r.width, r.height) * 2;
      const dot = document.createElement("span");
      dot.className = "btn-ripple";
      dot.style.width = dot.style.height = `${size}px`;
      dot.style.left = `${e.clientX - r.left - size / 2}px`;
      dot.style.top = `${e.clientY - r.top - size / 2}px`;
      el.appendChild(dot);
      setTimeout(() => dot.remove(), 650);
    };
    document.addEventListener("pointerdown", onDown, { passive: true });
    return () => document.removeEventListener("pointerdown", onDown);
  }, []);

  // Verify the login once (not on every page change). A stored copy of the profile lets the
  // app paint straight away while the server confirms in the background.
  const load = useCallback(() => {
    if (!localStorage.getItem("nomad_token")) {
      setUser(null);
      setAuthState("out");
      return;
    }
    // The app opens only for a confirmed login. A stored copy of the profile (saved after a
    // successful login) lets it open instantly; otherwise we wait for the server to confirm.
    const cached = peekCached<Me>("/api/auth/me") ?? null;
    if (cached) {
      setUser((u) => u ?? cached);
      setAuthState("in");
    }
    setConnectError(false);
    fetchMe()
      .then((d) => {
        setUser(d);
        setAuthState("in");
      })
      .catch((err) => {
        const s = err?.response?.status;
        if (s === 401 || s === 403) {
          localStorage.removeItem("nomad_token");
          clearCache();
          setUser(null);
          setAuthState("out");
        } else if (!cached) {
          setConnectError(true);
        }
      });
  }, []);

  useEffect(() => {
    load();
    const onStorage = (e: StorageEvent) => e.key === "nomad_token" && load();
    window.addEventListener("storage", onStorage);
    return () => window.removeEventListener("storage", onStorage);
  }, [load]);

  // The "needs payment" badge refreshes as you move around, but shares one cached request.
  useEffect(() => {
    if (authState !== "in") return;
    getCached<any[]>("/api/trips/me", { ttl: 15000, persist: true })
      .then((list) => {
        saveOfflineTrips(list || []);
        setNeedsPayment((list || []).filter((t: any) => t.status === "PLANNED" || t.status === "PAYMENT_PENDING").length);
      })
      .catch(() => undefined);
  }, [authState, path]);

  // Remember the last page of each tab (kept for the browser session).
  useEffect(() => {
    try {
      setLast(JSON.parse(sessionStorage.getItem(LAST_KEY) || "{}"));
    } catch {
      /* ignore */
    }
  }, []);
  useEffect(() => {
    if (NOT_REMEMBERED.has(path) || isPublic(path)) return;
    const url = path + window.location.search;
    setLast((prev) => {
      if (prev[section] === url) return prev;
      const next = { ...prev, [section]: url };
      try {
        sessionStorage.setItem(LAST_KEY, JSON.stringify(next));
      } catch {
        /* ignore */
      }
      return next;
    });
  }, [path, section]);

  useEffect(() => setOpen(false), [path]);
  // The slide direction only applies to the page change it was set for.
  useEffect(() => {
    slideRef.current = null;
  }, [path]);

  // Swipe sideways to move between the tabs, like WhatsApp: Home, Plan, My Trips, More.
  useEffect(() => {
    if (authState !== "in") return;
    const ORDER: Section[] = ["home", "plan", "trips", "more"];
    const FIXED = ["/payment", "/auth"];
    const SKIP = "nav, header, input, textarea, select, button, [contenteditable], .leaflet-container, .overflow-x-auto, [data-no-swipe]";
    let sx = 0, sy = 0, t0 = 0;
    let mode: "idle" | "x" | "y" = "idle";
    let on = false;

    const go = (dir: 1 | -1) => {
      const { section: sec, open: sheet, last: lastPages } = live.current;
      const idx = sheet ? 3 : ORDER.indexOf(sec);
      const next = idx + dir;
      if (next < 0 || next > 3) return false;
      if (next === 3) {
        if (sheet || sec === "more") return false;
        setOpen(true);
        return true;
      }
      const tab = TABS[next];
      slideRef.current = dir > 0 ? "next" : "prev";
      setOpen(false);
      router.push(lastPages[tab.section] || tab.href);
      return true;
    };
    const canGo = (dir: 1 | -1) => {
      const { section: sec, open: sheet } = live.current;
      const next = (sheet ? 3 : ORDER.indexOf(sec)) + dir;
      return next >= 0 && next <= 3 && !(next === 3 && (sheet || sec === "more"));
    };
    const settle = () => {
      const el = mainRef.current;
      if (!el) return;
      el.style.transition = "transform .22s cubic-bezier(.22,1,.36,1), opacity .22s";
      el.style.transform = "";
      el.style.opacity = "";
    };

    const onStart = (e: TouchEvent) => {
      const { path: here } = live.current;
      on = window.innerWidth < 1024 && e.touches.length === 1 && !FIXED.includes(here) && !(e.target as HTMLElement | null)?.closest?.(SKIP);
      mode = "idle";
      if (!on) return;
      sx = e.touches[0].clientX;
      sy = e.touches[0].clientY;
      t0 = Date.now();
    };
    const onMove = (e: TouchEvent) => {
      if (!on || mode === "y") return;
      const dx = e.touches[0].clientX - sx;
      const dy = e.touches[0].clientY - sy;
      if (mode === "idle") {
        if (Math.abs(dx) < 10 && Math.abs(dy) < 10) return;
        mode = Math.abs(dx) > Math.abs(dy) * 1.5 ? "x" : "y";
        if (mode === "y") return;
      }
      const el = mainRef.current;
      if (!el) return;
      // The page follows the finger a little; it resists when there is no tab that way.
      const ease = canGo(dx < 0 ? 1 : -1) ? 0.35 : 0.12;
      const shift = Math.max(-80, Math.min(80, dx * ease));
      el.style.transition = "none";
      el.style.transform = `translateX(${shift}px)`;
      el.style.opacity = String(1 - Math.min(Math.abs(shift) / 220, 0.25));
    };
    const onEnd = (e: TouchEvent) => {
      if (!on || mode !== "x") return;
      on = false;
      const dx = e.changedTouches[0].clientX - sx;
      const fast = Date.now() - t0 < 250 && Math.abs(dx) > 40;
      if ((Math.abs(dx) > 70 || fast) && go(dx < 0 ? 1 : -1)) return;
      settle();
    };
    const onCancel = () => {
      if (mode === "x") settle();
      on = false;
    };

    document.addEventListener("touchstart", onStart, { passive: true });
    document.addEventListener("touchmove", onMove, { passive: true });
    document.addEventListener("touchend", onEnd, { passive: true });
    document.addEventListener("touchcancel", onCancel, { passive: true });
    return () => {
      document.removeEventListener("touchstart", onStart);
      document.removeEventListener("touchmove", onMove);
      document.removeEventListener("touchend", onEnd);
      document.removeEventListener("touchcancel", onCancel);
    };
  }, [authState, router]);

  // While the More sheet is open: Escape closes it and the page behind doesn't scroll.
  useEffect(() => {
    if (!open) return;
    const onKey = (e: KeyboardEvent) => e.key === "Escape" && setOpen(false);
    window.addEventListener("keydown", onKey);
    const prev = document.body.style.overflow;
    document.body.style.overflow = "hidden";
    return () => {
      window.removeEventListener("keydown", onKey);
      document.body.style.overflow = prev;
    };
  }, [open]);

  // Signed out: the only place to be is the login/register page.
  useEffect(() => {
    if (authState === "out" && !isPublic(path)) router.replace("/auth");
  }, [authState, path, router]);

  const logout = () => {
    localStorage.removeItem("nomad_token");
    clearCache();
    try {
      sessionStorage.removeItem(LAST_KEY);
    } catch {
      /* ignore */
    }
    window.location.replace("/");
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
                <Link href={it.href} onClick={() => setOpen(false)} aria-current={active(path, it.href) ? "page" : undefined} className={`side-link ${active(path, it.href) ? "side-link-active" : ""}`}>
                  <Icon d={it.icon} />
                  <span className="flex-1">{it.label}</span>
                  {it.badge && needsPayment > 0 && (
                    <span title="Trips waiting for payment" className="rounded-full bg-brand-500/15 px-2 text-xs font-bold text-brand-300">{needsPayment}</span>
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
    <div className="space-y-2 border-t border-slate-700/60 p-3">
      <InstallApp />
      {authState === "in" ? (
        <div className="space-y-2">
          <Link href="/profile" onClick={() => setOpen(false)} className={`side-link ${active(path, "/profile") ? "side-link-active" : ""}`}>
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
    <Link href="/" aria-label="NOMAD home" className="flex h-16 items-center px-5"><Logo /></Link>
  );

  // Not signed in: no menus, just the page (login/register) on its own.
  if (authState !== "in") {
    if (authState === "out" && isPublic(path)) {
      return (
        <AuthContext.Provider value={{ state: authState, user: null }}>
          <main className="min-h-screen">{children}</main>
        </AuthContext.Provider>
      );
    }
    return (
      <div className="grid min-h-screen place-items-center px-6 text-center text-sm text-slate-500">
        <div className="space-y-3">
          <Logo className="justify-center" />
          {connectError ? (
            <>
              <p>Can't reach the server yet. It may be waking up.</p>
              <button type="button" className="btn btn-primary btn-sm" onClick={load}>Retry</button>
            </>
          ) : (
            <p>Signing you in…</p>
          )}
        </div>
      </div>
    );
  }

  const tabClass = (on: boolean) =>
    `relative flex min-h-[3rem] flex-1 select-none flex-col items-center justify-center gap-0.5 rounded-2xl py-2 text-[11px] font-semibold transition-colors ${on ? "bg-brand-600/25 text-slate-100 shadow-[inset_0_2px_0_0_theme(colors.brand.400)]" : "text-slate-400 active:text-slate-200"}`;

  return (
    <AuthContext.Provider value={{ state: authState, user }}>
    <div className="min-h-screen pb-28 lg:pb-0 lg:pl-64">
      {/* Desktop sidebar: fixed size, always the same options */}
      <aside className="fixed inset-y-0 left-0 z-40 hidden w-64 flex-col border-r border-slate-700/70 bg-slate-900 lg:flex">
        {brand}
        {nav}
        {account}
      </aside>

      {/* Mobile: slim top bar */}
      <header className="sticky top-0 z-30 flex h-14 items-center justify-between border-b border-slate-800/80 bg-slate-950/85 px-4 backdrop-blur-md lg:hidden">
        <Link href="/" aria-label="NOMAD home"><Logo /></Link>
        <div className="flex items-center gap-2.5">
          {updateReady && <UpdateButton />}
          <Link href="/profile" aria-label="Profile" className="grid h-8 w-8 place-items-center rounded-full bg-brand-600 text-xs font-bold text-white ring-2 ring-brand-500/30">
            {name.trim().charAt(0).toUpperCase()}
          </Link>
        </div>
      </header>

      {/* Desktop: the same Update button, top right, only while an update is waiting */}
      {updateReady && (
        <div className="sticky top-0 z-30 hidden h-14 items-center justify-end px-6 lg:flex">
          <UpdateButton className="shadow-lg" />
        </div>
      )}

      {/* Mobile: thumb-reach tab bar. The last tab opens everything else. */}
      <nav aria-label="Quick" className="fixed inset-x-3 bottom-[calc(0.75rem+env(safe-area-inset-bottom))] z-40 flex gap-1 rounded-3xl border border-slate-700/60 bg-slate-900/90 p-1.5 shadow-2xl shadow-black/50 backdrop-blur-xl lg:hidden">
        {TABS.map((it) => {
          const here = section === it.section;
          // On another tab: jump back to where you left it. Already on it: tap goes to its start.
          const href = here ? it.href : last[it.section] || it.href;
          return (
            <Link key={it.section} href={href} onClick={(e) => openTab(e, href)} aria-current={here && !open ? "page" : undefined} className={tabClass(here && !open)}>
              <Icon d={it.icon} />
              {it.label}
              {it.badge && needsPayment > 0 && <span className="absolute right-[28%] top-1.5 h-2 w-2 rounded-full bg-brand-500" />}
            </Link>
          );
        })}
        {/* Always says "More" and always the same icon; tapping again just closes the list. */}
        <button type="button" aria-expanded={open} aria-controls="more-sheet" onClick={() => setOpen((v) => !v)} className={tabClass(open || section === "more")}>
          <Icon d={MORE_ICON} />
          More
        </button>
      </nav>

      {/* Mobile "More" sheet */}
      {open && (
        <div id="more-sheet" className="fixed inset-x-0 bottom-0 top-14 z-30 flex flex-col overflow-y-auto bg-slate-950 pb-[calc(5.5rem+env(safe-area-inset-bottom))] lg:hidden">
          <Link href="/profile" onClick={() => setOpen(false)} className="mx-4 mt-4 flex items-center gap-3 rounded-2xl border border-slate-800 bg-slate-900/70 p-3 transition-colors active:bg-slate-800">
            <span className="grid h-11 w-11 shrink-0 place-items-center rounded-full bg-gradient-to-br from-brand-500 to-ocean-400 text-base font-bold text-white">{name.trim().charAt(0).toUpperCase()}</span>
            <span className="min-w-0 flex-1">
              <span className="block truncate text-sm font-semibold">{name}</span>
              <span className="block text-xs text-slate-400">Profile &amp; settings</span>
            </span>
            <span aria-hidden="true" className="text-slate-500">›</span>
          </Link>

          {groups.map((g) => (
            <section key={g.title} className="px-4 pt-5">
              <p className="eyebrow mb-2 px-1">{g.title}</p>
              <ul className="grid grid-cols-3 gap-2.5">
                {g.items.map((it) => {
                  const on = active(path, it.href);
                  return (
                    <li key={it.href}>
                      <Link
                        href={it.href}
                        onClick={() => setOpen(false)}
                        aria-current={on ? "page" : undefined}
                        className={`relative flex min-h-[5rem] flex-col items-center justify-center gap-1.5 rounded-2xl border px-1 py-3 text-center text-xs font-semibold transition-all active:scale-95 ${
                          on ? "border-brand-500/60 bg-brand-600/20 text-slate-100 shadow-glow" : "border-slate-800 bg-slate-900/70 text-slate-300"
                        }`}
                      >
                        <span className={`grid h-9 w-9 place-items-center rounded-xl ${on ? "bg-brand-600 text-white" : "bg-slate-800 text-brand-300"}`}>
                          <Icon d={it.icon} />
                        </span>
                        {it.label}
                        {it.badge && needsPayment > 0 && (
                          <span className="absolute right-2 top-2 grid h-5 min-w-5 place-items-center rounded-full bg-brand-500 px-1 text-[10px] font-bold text-white">{needsPayment}</span>
                        )}
                      </Link>
                    </li>
                  );
                })}
              </ul>
            </section>
          ))}

          <div className="mt-5 space-y-2 px-4">
            <InstallApp />
            <button type="button" onClick={logout} className="btn btn-ghost btn-sm w-full">Log out</button>
          </div>
        </div>
      )}

      <Toaster />
      <NetworkStatus />
      <Reminders updateReady={updateReady} build={process.env.NEXT_PUBLIC_BUILD_ID || "dev"} />
      <main key={path} ref={mainRef} data-dir={navRef.current.dir ?? undefined} className="min-h-[calc(100vh-3rem)]">{children}</main>
    </div>
    </AuthContext.Provider>
  );
}
