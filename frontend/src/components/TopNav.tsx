"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { useCallback, useEffect, useRef, useState } from "react";
import { fetchMe } from "@/lib/authApi";

type User = {
  id: number;
  email: string;
  name?: string;
  role?: string;
};

type NavItem = { href: string; label: string };

// Top-level links never change with auth state, so nothing shifts when
// the user logs in/out or when the profile finishes loading.
const MAIN_LINKS: NavItem[] = [
  { href: "/", label: "Home" },
  { href: "/packages", label: "Packages" },
  { href: "/map", label: "Map" },
];

// Ordered by the real journey: plan -> see your trips -> review -> pay.
const TRIP_LINKS: NavItem[] = [
  { href: "/trip-planner", label: "Plan a trip" },
  { href: "/bookings", label: "My Trips" },
  { href: "/trip-summary", label: "Trip summary" },
  { href: "/route-view", label: "Route" },
  { href: "/payment", label: "Payment" },
  { href: "/wishlist", label: "Wishlist" },
];

const ADMIN_LINKS: NavItem[] = [
  { href: "/admin", label: "Admin" },
  { href: "/group-status", label: "Groups" },
];

type AuthState = "loading" | "out" | "in";

function isActive(pathname: string, href: string) {
  if (href === "/") return pathname === "/";
  return pathname === href || pathname.startsWith(`${href}/`);
}

function Chevron({ open }: { open: boolean }) {
  return (
    <svg
      aria-hidden="true"
      viewBox="0 0 20 20"
      className={`h-4 w-4 transition-transform ${open ? "rotate-180" : ""}`}
      fill="currentColor"
    >
      <path d="M5.3 7.3a1 1 0 0 1 1.4 0L10 10.6l3.3-3.3a1 1 0 1 1 1.4 1.4l-4 4a1 1 0 0 1-1.4 0l-4-4a1 1 0 0 1 0-1.4z" />
    </svg>
  );
}

export default function TopNav() {
  const pathname = usePathname() ?? "/";
  const [authState, setAuthState] = useState<AuthState>("loading");
  const [user, setUser] = useState<User | null>(null);
  const [openMenu, setOpenMenu] = useState<"trip" | "account" | null>(null);
  const [mobileOpen, setMobileOpen] = useState(false);
  const navRef = useRef<HTMLDivElement>(null);

  const loadUser = useCallback(() => {
    const token = localStorage.getItem("nomad_token");
    if (!token) {
      setUser(null);
      setAuthState("out");
      return;
    }
    // Signed in as soon as a token exists; details fill in afterwards.
    setAuthState("in");
    fetchMe()
      .then((data) =>
        setUser({ id: data.id, email: data.email, name: data.name, role: data.role })
      )
      .catch((err) => {
        // Only treat a rejected token as logged out, not a flaky network.
        const status = err?.response?.status;
        if (status === 401 || status === 403) {
          localStorage.removeItem("nomad_token");
          setUser(null);
          setAuthState("out");
        }
      });
  }, []);

  useEffect(() => {
    loadUser();
    // Keep multiple tabs in sync.
    const onStorage = (e: StorageEvent) => {
      if (e.key === "nomad_token") loadUser();
    };
    window.addEventListener("storage", onStorage);
    return () => window.removeEventListener("storage", onStorage);
  }, [loadUser]);

  // Close menus on navigation.
  useEffect(() => {
    setOpenMenu(null);
    setMobileOpen(false);
  }, [pathname]);

  // Close dropdowns on outside click / Escape.
  useEffect(() => {
    const onClick = (e: MouseEvent) => {
      if (navRef.current && !navRef.current.contains(e.target as Node)) {
        setOpenMenu(null);
      }
    };
    const onKey = (e: KeyboardEvent) => {
      if (e.key === "Escape") {
        setOpenMenu(null);
        setMobileOpen(false);
      }
    };
    document.addEventListener("mousedown", onClick);
    document.addEventListener("keydown", onKey);
    return () => {
      document.removeEventListener("mousedown", onClick);
      document.removeEventListener("keydown", onKey);
    };
  }, []);

  const logout = () => {
    localStorage.removeItem("nomad_token");
    setUser(null);
    setAuthState("out");
    // Full navigation so every page resets its auth-dependent state.
    window.location.href = "/";
  };

  const isAdmin = user?.role === "ADMIN";
  const displayName = user?.name || user?.email || "Account";
  const initial = displayName.trim().charAt(0).toUpperCase() || "?";
  const tripActive = TRIP_LINKS.some((l) => isActive(pathname, l.href));
  const accountActive =
    isActive(pathname, "/profile") || ADMIN_LINKS.some((l) => isActive(pathname, l.href));

  const toggle = (menu: "trip" | "account") =>
    setOpenMenu((current) => (current === menu ? null : menu));

  return (
    <div ref={navRef} className="flex items-center gap-2">
      {/* Desktop navigation */}
      <nav aria-label="Main" className="hidden lg:flex items-center gap-1">
        {MAIN_LINKS.map((link) => (
          <Link
            key={link.href}
            href={link.href}
            aria-current={isActive(pathname, link.href) ? "page" : undefined}
            className={`nav-link ${isActive(pathname, link.href) ? "nav-link-active" : ""}`}
          >
            {link.label}
          </Link>
        ))}

        <div className="relative">
          <button
            type="button"
            aria-haspopup="menu"
            aria-expanded={openMenu === "trip"}
            onClick={() => toggle("trip")}
            className={`nav-link gap-1 ${tripActive || openMenu === "trip" ? "nav-link-active" : ""}`}
          >
            Trips
            <Chevron open={openMenu === "trip"} />
          </button>
          {openMenu === "trip" && (
            <div role="menu" className="nav-menu left-0">
              {TRIP_LINKS.map((link) => (
                <Link
                  key={link.href}
                  href={link.href}
                  role="menuitem"
                  className={`nav-menu-item ${isActive(pathname, link.href) ? "nav-menu-item-active" : ""}`}
                >
                  {link.label}
                </Link>
              ))}
            </div>
          )}
        </div>
      </nav>

      {/* Auth control: fixed-width slot so the header never jumps */}
      <div className="hidden lg:flex w-52 justify-end">
        {authState === "loading" && (
          <div aria-hidden="true" className="h-9 w-40 rounded-lg bg-slate-800/60 animate-pulse" />
        )}

        {authState === "out" && (
          <div className="flex items-center gap-2">
            <Link href="/auth" className="nav-btn nav-btn-ghost">Log in</Link>
            <Link href="/auth?mode=register" className="nav-btn nav-btn-solid">Sign up</Link>
          </div>
        )}

        {authState === "in" && (
          <div className="relative">
            <button
              type="button"
              aria-haspopup="menu"
              aria-expanded={openMenu === "account"}
              onClick={() => toggle("account")}
              className={`nav-btn nav-btn-ghost gap-2 w-52 justify-between ${accountActive || openMenu === "account" ? "nav-link-active" : ""}`}
            >
              <span className="flex items-center gap-2 min-w-0">
                <span className="grid h-6 w-6 shrink-0 place-items-center rounded-full bg-brand-600 text-xs font-bold text-white">
                  {initial}
                </span>
                <span className="truncate">{user ? displayName : "Account"}</span>
              </span>
              <Chevron open={openMenu === "account"} />
            </button>
            {openMenu === "account" && (
              <div role="menu" className="nav-menu right-0">
                <div className="px-3 py-2 text-xs text-slate-400 truncate">
                  {user?.email ?? "Signed in"}
                  {isAdmin && <span className="ml-2 badge">Admin</span>}
                </div>
                <Link
                  href="/profile"
                  role="menuitem"
                  className={`nav-menu-item ${isActive(pathname, "/profile") ? "nav-menu-item-active" : ""}`}
                >
                  Profile
                </Link>
                {isAdmin &&
                  ADMIN_LINKS.map((link) => (
                    <Link
                      key={link.href}
                      href={link.href}
                      role="menuitem"
                      className={`nav-menu-item ${isActive(pathname, link.href) ? "nav-menu-item-active" : ""}`}
                    >
                      {link.label}
                    </Link>
                  ))}
                <div className="my-1 border-t border-slate-800" />
                <button
                  type="button"
                  role="menuitem"
                  onClick={logout}
                  className="nav-menu-item w-full text-left text-red-300 hover:text-red-200"
                >
                  Log out
                </button>
              </div>
            )}
          </div>
        )}
      </div>

      {/* Mobile / tablet toggle */}
      <button
        type="button"
        aria-label="Toggle menu"
        aria-expanded={mobileOpen}
        onClick={() => setMobileOpen((v) => !v)}
        className="nav-btn nav-btn-ghost lg:hidden"
      >
        <svg aria-hidden="true" viewBox="0 0 24 24" className="h-5 w-5" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round">
          {mobileOpen ? <path d="M6 6l12 12M18 6L6 18" /> : <path d="M4 7h16M4 12h16M4 17h16" />}
        </svg>
      </button>

      {mobileOpen && (
        <div className="lg:hidden absolute inset-x-0 top-full z-40 border-b border-slate-800 bg-slate-900 shadow-xl shadow-black/30">
          <div className="section py-4 space-y-4">
            <MobileGroup title="Explore" links={MAIN_LINKS} pathname={pathname} />
            <MobileGroup title="Trips" links={TRIP_LINKS} pathname={pathname} />
            {authState === "in" && (
              <MobileGroup
                title="Account"
                links={[{ href: "/profile", label: "Profile" }, ...(isAdmin ? ADMIN_LINKS : [])]}
                pathname={pathname}
              />
            )}
            <div className="pt-2 border-t border-slate-800">
              {authState === "in" ? (
                <button type="button" onClick={logout} className="nav-btn nav-btn-ghost w-full text-red-300">
                  Log out{user?.email ? ` (${user.email})` : ""}
                </button>
              ) : (
                <div className="grid grid-cols-2 gap-2">
                  <Link href="/auth" className="nav-btn nav-btn-ghost">Log in</Link>
                  <Link href="/auth?mode=register" className="nav-btn nav-btn-solid">Sign up</Link>
                </div>
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

function MobileGroup({
  title,
  links,
  pathname,
}: {
  title: string;
  links: NavItem[];
  pathname: string;
}) {
  return (
    <div>
      <p className="mb-1 px-3 text-xs font-semibold uppercase tracking-wide text-slate-500">{title}</p>
      <div className="grid grid-cols-2 gap-1">
        {links.map((link) => (
          <Link
            key={link.href}
            href={link.href}
            className={`nav-link ${isActive(pathname, link.href) ? "nav-link-active" : ""}`}
          >
            {link.label}
          </Link>
        ))}
      </div>
    </div>
  );
}
