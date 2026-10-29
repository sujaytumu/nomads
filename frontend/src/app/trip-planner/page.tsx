"use client";


import Link from "next/link";
import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";

import ProtectedPage from "@/components/ProtectedPage";
import TripSteps from "@/components/TripSteps";
import { createTrip } from "@/lib/tripApi";
import { fetchMe } from "@/lib/authApi";
import { formatCost } from "@/lib/tripStatus";

import { CardSkeleton } from "@/components/Skeleton";
import LoadingHint from "@/components/LoadingHint";
const INTERESTS = [
  { v: "FOOD", l: "Food", icon: "🍜" },
  { v: "CULTURE", l: "Culture", icon: "🏛️" },
  { v: "NATURE", l: "Nature", icon: "🌿" },
  { v: "ADVENTURE", l: "Adventure", icon: "🧗" },
  { v: "SHOPPING", l: "Shopping", icon: "🛍️" },
  { v: "NIGHTLIFE", l: "Nightlife", icon: "🌙" },
  { v: "RELAXATION", l: "Relax", icon: "🧘" },
];

const MAX_SOLO_DAYS = 2;
const MAX_DAYS = 7;
const MAX_PEOPLE = 8;
const clampDays = (d: number, mode: string) => Math.max(1, Math.min(mode === "GROUP" ? MAX_DAYS : MAX_SOLO_DAYS, d || 2));

const chip = (on: boolean) =>
  `rounded-full border px-4 py-2 text-sm font-semibold transition-colors ${
    on ? "border-brand-600 bg-brand-600 text-white shadow-md shadow-brand-600/25" : "border-slate-700 bg-slate-900 text-slate-300 hover:bg-slate-800"
  }`;

export default function TripPlannerPage() {
  const router = useRouter();
  const [tripDate, setTripDateValue] = useState("");
  const [form, setForm] = useState({
    userId: "",
    city: "",
    weekendType: "TWO_DAY",
    days: 2,
    interest: "CULTURE",
    travelMode: "SOLO",
    pickupRequired: false,
    userLatitude: "",
    userLongitude: "",
    groupSize: "",
  });
  const [created, setCreated] = useState<any>(null);
  const [loadingUser, setLoadingUser] = useState(true);
  const [loadingTrip, setLoadingTrip] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [cityLocked, setCityLocked] = useState(false);

  useEffect(() => {
    const qs = new URLSearchParams(window.location.search);
    fetchMe()
      .then((user) => {
        setForm((prev) => ({
          ...prev,
          userId: String(user.id),
          city: user.city ?? "",
          userLatitude: user.latitude?.toString() ?? "",
          userLongitude: user.longitude?.toString() ?? "",
          // An interest chosen on Home (?interest=FOOD) beats the profile default.
          interest: qs.get("interest") ?? user.interestType ?? prev.interest,
          travelMode: qs.get("mode") ?? user.travelPreference ?? prev.travelMode,
          weekendType: qs.get("weekend") ?? prev.weekendType,
          days: clampDays(Number(qs.get("days")) || (qs.get("weekend") === "ONE_DAY" ? 1 : prev.days), qs.get("mode") ?? user.travelPreference ?? prev.travelMode),
          groupSize: qs.get("group") ?? prev.groupSize,
          pickupRequired: qs.get("pickup") === "1" ? true : prev.pickupRequired,
        }));
        setCityLocked(true);
      })
      .catch(() => setError("Failed to load your profile"))
      .finally(() => setLoadingUser(false));
  }, []);

  const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) => {
    const { name, value, type } = e.target;
    const checked = type === "checkbox" ? (e.target as HTMLInputElement).checked : undefined;
    setForm((prev) => ({
      ...prev,
      [name]: checked ?? value,
    }));
  };

  const handleSubmit = async () => {
    setError(null);
    setCreated(null);
    if (!form.userId) {
      setError("User ID is required");
      return;
    }
    setLoadingTrip(true);
    const payload = {
      userId: Number(form.userId),
      city: form.city || undefined,
      weekendType: form.days === 1 ? "ONE_DAY" : "TWO_DAY",
      days: form.days,
      interest: form.interest,
      travelMode: form.travelMode,
      pickupRequired: form.pickupRequired,
      userLatitude: form.userLatitude ? Number(form.userLatitude) : undefined,
      userLongitude: form.userLongitude ? Number(form.userLongitude) : undefined,
      groupSize: form.travelMode === "GROUP" ? Number(form.groupSize) || 2 : undefined,
      tripDate: tripDate || undefined,
    };

    try {
      const data = await createTrip(payload);
      setCreated(data);
      // One flow: straight on to reviewing the itinerary, then pay.
      router.push(`/trip-summary?tripId=${data.tripRequestId}`);
      return;
    } catch (error: any) {
      setError(error?.response?.data?.message || "Failed to create trip");
    } finally {
      setLoadingTrip(false);
    }
  };

  return (
    <ProtectedPage>
      <div className="section py-8">
        <div className="card space-y-6 p-5 sm:p-8">
        <TripSteps current={created ? 2 : 1} tripId={created?.tripRequestId} />
        <div>
          <h1 className="font-display text-2xl font-semibold">Plan your trip</h1>
          <p className="text-slate-400">
            Tell us what you like and we'll build your itinerary. Nothing is booked until you pay.
          </p>
        </div>

        {loadingUser && (
          <div className="space-y-2">
            <CardSkeleton rows={2} label="Loading your profile" />
            <LoadingHint label="Loading your profile" />
          </div>
        )}

        <div className="space-y-5">
          <div className="flex flex-wrap items-center gap-2 text-sm">
            <span className="font-semibold">Starting from</span>
            <span className="badge">📍 {form.city || "your city"}</span>
            {cityLocked && <Link href="/profile" className="text-xs text-brand-600 underline">Change in profile</Link>}
          </div>

          <div className="space-y-2">
            <p className="text-sm font-semibold">What do you feel like?</p>
            <div className="flex flex-wrap gap-2">
              {INTERESTS.map((i) => (
                <button key={i.v} type="button" onClick={() => setForm((p) => ({ ...p, interest: i.v }))} className={chip(form.interest === i.v)}>
                  <span aria-hidden="true" className="mr-1">{i.icon}</span>{i.l}
                </button>
              ))}
            </div>
          </div>

          <div className="space-y-2">
            <p className="text-sm font-semibold">Who's going?</p>
            <div className="grid grid-cols-2 gap-2 sm:max-w-sm">
              {[{ v: "SOLO", l: "🧍 Solo" }, { v: "GROUP", l: "👥 Group" }].map((o) => (
                <button
                  key={o.v}
                  type="button"
                  onClick={() => setForm((p) => ({ ...p, travelMode: o.v, days: clampDays(p.days, o.v), groupSize: o.v === "GROUP" ? p.groupSize || "2" : p.groupSize }))}
                  className={chip(form.travelMode === o.v)}
                >
                  {o.l}
                </button>
              ))}
            </div>
          </div>

          {form.travelMode === "GROUP" && (
            <div className="space-y-2">
              <p className="text-sm font-semibold">How many people? <span className="font-normal text-slate-500">(up to {MAX_PEOPLE})</span></p>
              <div className="flex flex-wrap gap-2">
                {Array.from({ length: MAX_PEOPLE - 1 }, (_, i) => i + 2).map((n) => (
                  <button key={n} type="button" aria-label={`${n} people`} onClick={() => setForm((p) => ({ ...p, groupSize: String(n) }))} className={`${chip(Number(form.groupSize || 2) === n)} min-w-[3rem]`}>{n}</button>
                ))}
              </div>
            </div>
          )}

          <div className="space-y-2">
            <p className="text-sm font-semibold">How many days?</p>
            <div className="flex flex-wrap gap-2">
              {Array.from({ length: form.travelMode === "GROUP" ? MAX_DAYS : MAX_SOLO_DAYS }, (_, i) => i + 1).map((n) => (
                <button key={n} type="button" aria-label={n === 1 ? "1 day" : `${n} days`} onClick={() => setForm((p) => ({ ...p, days: n }))} className={`${chip(form.days === n)} min-w-[3rem]`}>{n}</button>
              ))}
            </div>
            <p className="text-xs text-slate-400">
              {form.travelMode === "GROUP"
                ? `${form.days === 1 ? "A day trip" : `${form.days} days`}: about ${form.days === 1 ? 4 : form.days * 3} stops, grouped by area to save time and money.`
                : "Solo trips are 1 or 2 days. Pick Group for trips of up to 7 days."}
            </p>
          </div>

          <label className="flex cursor-pointer items-center justify-between gap-4 rounded-2xl border border-slate-700 px-4 py-3 sm:max-w-sm">
            <span>
              <span className="block text-sm font-semibold">Pickup assistance</span>
              <span className="block text-xs text-slate-400">A cab from your door (+₹300)</span>
            </span>
            <input type="checkbox" name="pickupRequired" checked={form.pickupRequired} onChange={handleChange} className="h-5 w-5 accent-brand-600" />
          </label>
        </div>

        <label className="block space-y-2 sm:max-w-sm">
          <span className="text-sm font-semibold">When are you going? <span className="font-normal text-slate-500">(optional)</span></span>
          <input type="date" min={new Date().toISOString().slice(0, 10)} value={tripDate} onChange={(e) => setTripDateValue(e.target.value)} className="w-full rounded-xl border px-4 py-2" />
        </label>
        <button className="btn btn-primary btn-pulse w-full sm:w-auto" onClick={handleSubmit} disabled={loadingTrip || loadingUser} data-loading={loadingTrip}>
          {loadingTrip ? "Building your itinerary…" : "Plan my trip →"}
        </button>
        {error && <p className="text-sm text-red-400">{error}</p>}

        {created && (
          <div className="rounded-xl border border-green-500/30 bg-green-500/10 p-4 space-y-3" role="status">
            <div>
              <p className="font-semibold text-green-300">
                Trip #{created.tripRequestId} planned{created.city ? ` for ${created.city}` : ""}
              </p>
              <p className="text-sm text-slate-300">
                Estimated cost {formatCost(created.estimatedCost)}. It isn't booked yet — review the itinerary,
                then pay to confirm.
              </p>
            </div>
            <div className="flex flex-wrap gap-3">
              <Link href={`/trip-summary?tripId=${created.tripRequestId}`} className="btn-primary">
                Review itinerary
              </Link>
              <Link href={`/payment?tripRequestId=${created.tripRequestId}`} className="btn-outline">
                Pay &amp; book
              </Link>
              <Link href="/bookings" className="btn-outline">
                My trips
              </Link>
            </div>
          </div>
        )}
        </div>
      </div>
    </ProtectedPage>
  );
}
