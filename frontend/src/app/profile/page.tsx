"use client";

import Link from "next/link";
import { useEffect, useMemo, useState } from "react";

import InstallApp from "@/components/InstallApp";
import PageHeader from "@/components/PageHeader";
import ProtectedPage from "@/components/ProtectedPage";
import { api } from "@/lib/api";
import { fetchMe } from "@/lib/authApi";
import { updateUser } from "@/lib/profileApi";

const CITIES: Record<string, [number, number]> = {
  Bengaluru: [12.9716, 77.5946],
  Mumbai: [19.076, 72.8777],
  Delhi: [28.6139, 77.209],
  Hyderabad: [17.385, 78.4867],
  Vishakapatnam: [17.6868, 83.2185],
};

const INTERESTS = [
  { value: "FOOD", label: "Food", icon: "🍜" },
  { value: "CULTURE", label: "Culture", icon: "🏛️" },
  { value: "NATURE", label: "Nature", icon: "🌿" },
  { value: "ADVENTURE", label: "Adventure", icon: "🧗" },
  { value: "SHOPPING", label: "Shopping", icon: "🛍️" },
  { value: "NIGHTLIFE", label: "Nightlife", icon: "🌙" },
  { value: "RELAXATION", label: "Relax", icon: "🧘" },
];

const UNIQUE = [
  { icon: "👥", title: "Auto groups", text: "Same city, interest & weekend? We group you.", href: "/group-status" },
  { icon: "💸", title: "Split costs", text: "Log shared spends, see who pays whom.", href: "/group-status" },
  { icon: "🚐", title: "Pickup & driver", text: "Vehicle, driver and live pickup status.", href: "/pickup" },
  { icon: "🗺️", title: "Day-by-day route", text: "Pick places, we plan days and the route.", href: "/trip-planner" },
  { icon: "📄", title: "Itinerary anywhere", text: "PDF, email or calendar in one tap.", href: "/bookings" },
  { icon: "🆘", title: "Trip safety", text: "Emergency card and packing list per trip.", href: "/bookings" },
];

const chip = (on: boolean) =>
  `rounded-full border px-4 py-2 text-sm font-semibold transition-colors ${
    on ? "border-brand-600 bg-brand-600 text-white shadow-md shadow-brand-600/25" : "border-slate-700 bg-slate-900 text-slate-300 hover:bg-slate-800"
  }`;

type Form = { id: string; email: string; name: string; phoneNumber: string; city: string; latitude: string; longitude: string; interestType: string; travelPreference: string };
const EMPTY: Form = { id: "", email: "", name: "", phoneNumber: "", city: "", latitude: "", longitude: "", interestType: "CULTURE", travelPreference: "SOLO" };

export default function ProfilePage() {
  const [form, setForm] = useState<Form>(EMPTY);
  const [saved, setSaved] = useState<Form>(EMPTY);
  const [status, setStatus] = useState<{ ok: boolean; text: string } | null>(null);
  const [saving, setSaving] = useState(false);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState(false);
  const [stats, setStats] = useState<{ trips: number; booked: number } | null>(null);

  const loadProfile = () => {
    setLoading(true);
    setLoadError(false);
    fetchMe()
      .then((d) => {
        const f: Form = {
          id: String(d.id),
          email: d.email ?? "",
          name: d.name ?? "",
          phoneNumber: d.phoneNumber ?? "",
          city: d.city ?? "",
          latitude: String(d.latitude ?? ""),
          longitude: String(d.longitude ?? ""),
          interestType: d.interestType ?? "CULTURE",
          travelPreference: d.travelPreference ?? "SOLO",
        };
        setForm(f);
        setSaved(f);
      })
      .catch(() => setLoadError(true))
      .finally(() => setLoading(false));
    api
      .get("/api/trips/me")
      .then(({ data }) => {
        const list = Array.isArray(data) ? data : [];
        setStats({ trips: list.length, booked: list.filter((t: any) => t.status === "CONFIRMED").length });
      })
      .catch(() => undefined);
  };

  useEffect(loadProfile, []);

  const dirty = useMemo(() => JSON.stringify(form) !== JSON.stringify(saved), [form, saved]);
  const set = (patch: Partial<Form>) => {
    setStatus(null);
    setForm((p) => ({ ...p, ...patch }));
  };

  // Picking a city also sets the coordinates used for pickup and distance.
  const pickCity = (city: string) => {
    const c = CITIES[city];
    set(c ? { city, latitude: String(c[0]), longitude: String(c[1]) } : { city });
  };

  const handleSave = async () => {
    setSaving(true);
    setStatus(null);
    try {
      await updateUser(Number(form.id), {
        name: form.name,
        city: form.city,
        phoneNumber: form.phoneNumber || undefined,
        latitude: Number(form.latitude),
        longitude: Number(form.longitude),
        interestType: form.interestType,
        travelPreference: form.travelPreference,
      });
      setSaved(form);
      setStatus({ ok: true, text: "Saved. Your suggestions will follow these choices." });
    } catch {
      setStatus({ ok: false, text: "Couldn't save. Check your connection and try again." });
    } finally {
      setSaving(false);
    }
  };

  const logout = () => {
    localStorage.removeItem("nomad_token");
    window.location.replace("/");
  };

  const initials = (saved.name || saved.email || "N").trim().split(/\s+/).map((w) => w[0]).slice(0, 2).join("").toUpperCase();
  const interest = INTERESTS.find((i) => i.value === saved.interestType);

  // One clear "what to do now" based on where the user is.
  const next = !saved.city
    ? { title: "Pick your home city", hint: "5 seconds. Unlocks picks near you.", href: "#city", cta: "Choose city" }
    : stats && stats.trips === 0
    ? { title: "Plan your first weekend", hint: "Choose places. We build the days and route.", href: "/trip-planner", cta: "Start planning" }
    : stats && stats.booked === 0
    ? { title: "Finish your booking", hint: "Pay to unlock group, pickup and cost split.", href: "/bookings", cta: "Open my trips" }
    : stats
    ? { title: "You're all set", hint: "Meet your group, check pickup, split costs.", href: "/group-status", cta: "Open group" }
    : { title: "Explore places", hint: "See what's around you.", href: "/map", cta: "Explore" };

  return (
    <ProtectedPage>
      <div className="section max-w-3xl space-y-5 py-8 pb-28">
        <PageHeader title="Profile" subtitle="Set your city and style once. We plan the rest." />

        {loading && <div className="card h-40 animate-pulse" aria-label="Loading your profile" />}
        {!loading && loadError && (
          <div className="card space-y-3 p-5">
            <p className="text-sm text-red-300">Couldn't load your profile. The server may still be waking up.</p>
            <button className="btn btn-secondary btn-sm" onClick={loadProfile}>Retry</button>
          </div>
        )}

        {!loading && !loadError && (
          <>
            <div className="card flex flex-wrap items-center gap-4 p-5">
              <div className="flex h-16 w-16 shrink-0 items-center justify-center rounded-full bg-gradient-to-br from-brand-500 to-ocean-500 font-display text-xl font-bold text-white">{initials}</div>
              <div className="min-w-0 flex-1">
                <p className="truncate font-display text-xl font-semibold">{saved.name || "Traveller"}</p>
                <p className="truncate text-sm text-slate-400">{saved.email}</p>
                <div className="mt-2 flex flex-wrap gap-2 text-xs font-semibold">
                  {saved.city && <span className="badge">📍 {saved.city}</span>}
                  {interest && <span className="badge">{interest.icon} {interest.label}</span>}
                  <span className="badge">{saved.travelPreference === "GROUP" ? "👥 Group" : "🧍 Solo"}</span>
                </div>
              </div>
              {stats && (
                <Link href="/bookings" className="flex gap-5 text-center">
                  <div><p className="font-display text-2xl font-semibold">{stats.trips}</p><p className="text-xs text-slate-400">Trips</p></div>
                  <div><p className="font-display text-2xl font-semibold">{stats.booked}</p><p className="text-xs text-slate-400">Booked</p></div>
                </Link>
              )}
            </div>

            <section className="card flex flex-wrap items-center gap-3 border-brand-600/40 p-4">
              <div className="min-w-0 flex-1">
                <p className="text-xs font-semibold uppercase tracking-wide text-brand-500">Do this next</p>
                <p className="font-display text-base font-semibold">{next.title}</p>
                <p className="text-sm text-slate-400">{next.hint}</p>
              </div>
              <Link href={next.href} className="btn btn-primary btn-sm">{next.cta}</Link>
            </section>

            <section className="card space-y-3 p-4">
              <h2 className="font-display text-base font-semibold">How NOMAD works</h2>
              <ol className="grid grid-cols-2 gap-2 text-xs sm:grid-cols-4">
                {["Set city & style", "Plan a weekend", "Book & pay", "Group, pickup, costs"].map((t, i) => (
                  <li key={t} className="flex items-center gap-2 rounded-xl border border-slate-800 bg-slate-900/60 px-3 py-2 font-semibold text-slate-300">
                    <span className="grid h-5 w-5 shrink-0 place-items-center rounded-full bg-brand-600 text-[11px] text-white">{i + 1}</span>{t}
                  </li>
                ))}
              </ol>
            </section>

            <section className="card space-y-4 p-5">
              <h2 className="font-display text-lg font-semibold">About you</h2>
              <div className="grid gap-4 sm:grid-cols-2">
                <label className="space-y-1 text-sm font-medium">Name
                  <input value={form.name} onChange={(e) => set({ name: e.target.value })} autoComplete="name" className="mt-1 w-full rounded-xl border px-4 py-2.5" />
                </label>
                <label className="space-y-1 text-sm font-medium">Phone (for pickup updates)
                  <input value={form.phoneNumber} onChange={(e) => set({ phoneNumber: e.target.value })} inputMode="tel" autoComplete="tel" placeholder="+91…" className="mt-1 w-full rounded-xl border px-4 py-2.5" />
                </label>
              </div>
            </section>

            <section id="city" className="card scroll-mt-20 space-y-3 p-5">
              <h2 className="font-display text-lg font-semibold">Home city</h2>
              <div className="flex flex-wrap gap-2">
                {Object.keys(CITIES).map((c) => (
                  <button key={c} type="button" onClick={() => pickCity(c)} className={chip(form.city === c)}>📍 {c}</button>
                ))}
              </div>
            </section>

            <section className="card space-y-4 p-5">
              <div>
                <h2 className="font-display text-lg font-semibold">Travel style</h2>
                <p className="text-sm text-slate-400">What you enjoy most. We use it on Home and in the planner.</p>
              </div>
              <div className="flex flex-wrap gap-2">
                {INTERESTS.map((i) => (
                  <button key={i.value} type="button" onClick={() => set({ interestType: i.value })} className={chip(form.interestType === i.value)}>
                    <span aria-hidden="true" className="mr-1">{i.icon}</span>{i.label}
                  </button>
                ))}
              </div>
              <div className="grid grid-cols-2 gap-2 sm:max-w-sm">
                {[{ v: "SOLO", l: "🧍 Solo" }, { v: "GROUP", l: "👥 With a group" }].map((o) => (
                  <button key={o.v} type="button" onClick={() => set({ travelPreference: o.v })} className={chip(form.travelPreference === o.v)}>{o.l}</button>
                ))}
              </div>
            </section>

            <section className="card space-y-3 p-4">
              <h2 className="font-display text-base font-semibold">What's unique about NOMAD</h2>
              <div className="grid grid-cols-2 gap-2 sm:grid-cols-3">
                {UNIQUE.map((u) => (
                  <Link key={u.title} href={u.href} className="rounded-xl border border-slate-800 bg-slate-900/60 p-3 transition-colors hover:bg-slate-800">
                    <p className="text-sm font-semibold"><span aria-hidden="true" className="mr-1">{u.icon}</span>{u.title}</p>
                    <p className="mt-0.5 text-xs leading-snug text-slate-400">{u.text}</p>
                  </Link>
                ))}
              </div>
            </section>

            <section className="card space-y-3 p-5">
              <h2 className="font-display text-lg font-semibold">App</h2>
              <div className="flex flex-wrap items-center gap-2">
                <InstallApp />
                <Link href="/wishlist" className="btn btn-secondary btn-sm">♡ Wishlist</Link>
                <Link href="/bookings" className="btn btn-secondary btn-sm">My Trips</Link>
                <button type="button" onClick={logout} className="btn btn-ghost btn-sm">Log out</button>
              </div>
            </section>

            <div className="fixed inset-x-0 bottom-20 z-30 px-4 sm:static sm:bottom-auto sm:px-0">
              <div className="mx-auto flex max-w-3xl items-center gap-3 rounded-2xl border border-slate-800 bg-slate-900/95 p-3 shadow-lift backdrop-blur sm:border-0 sm:bg-transparent sm:p-0 sm:shadow-none">
                <button className="btn btn-primary" onClick={handleSave} disabled={!dirty || saving} data-loading={saving}>{saving ? "Saving…" : "Save changes"}</button>
                <p role="status" className={`text-sm ${status ? (status.ok ? "text-green-400" : "text-red-400") : "text-slate-400"}`}>
                  {status ? status.text : dirty ? "You have unsaved changes" : "All saved"}
                </p>
              </div>
            </div>
          </>
        )}
      </div>
    </ProtectedPage>
  );
}
