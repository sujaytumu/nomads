"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";

import ProtectedPage from "@/components/ProtectedPage";
import TripSteps from "@/components/TripSteps";
import { createTrip } from "@/lib/tripApi";
import { fetchMe } from "@/lib/authApi";
import { formatCost } from "@/lib/tripStatus";

const INTERESTS = [
  { v: "FOOD", l: "Food", icon: "🍜" },
  { v: "CULTURE", l: "Culture", icon: "🏛️" },
  { v: "NATURE", l: "Nature", icon: "🌿" },
  { v: "ADVENTURE", l: "Adventure", icon: "🧗" },
  { v: "SHOPPING", l: "Shopping", icon: "🛍️" },
  { v: "NIGHTLIFE", l: "Nightlife", icon: "🌙" },
  { v: "RELAXATION", l: "Relax", icon: "🧘" },
];

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
      weekendType: form.weekendType,
      interest: form.interest,
      travelMode: form.travelMode,
      pickupRequired: form.pickupRequired,
      userLatitude: form.userLatitude ? Number(form.userLatitude) : undefined,
      userLongitude: form.userLongitude ? Number(form.userLongitude) : undefined,
      groupSize: form.groupSize ? Number(form.groupSize) : undefined,
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
          <h1 className="font-display text-2xl font-semibold">Plan your weekend</h1>
          <p className="text-slate-400">
            Tell us what you like and we'll build a weekend itinerary. Nothing is booked until you pay.
          </p>
        </div>

        {loadingUser && <p className="text-sm text-slate-400">Loading your profile… (may take up to a minute if the server was asleep)</p>}

        <div className="space-y-5">
          <div className="flex flex-wrap items-center gap-2 text-sm">
            <span className="font-semibold">Starting from</span>
            <span className="badge">📍 {form.city || "your city"}</span>
            {cityLocked && <Link href="/profile" className="text-xs text-brand-600 underline">Change in profile</Link>}
          </div>

          <div className="space-y-2">
            <p className="text-sm font-semibold">How long?</p>
            <div className="grid grid-cols-2 gap-2 sm:max-w-sm">
              {[{ v: "ONE_DAY", l: "One day" }, { v: "TWO_DAY", l: "Two days" }].map((o) => (
                <button key={o.v} type="button" onClick={() => setForm((p) => ({ ...p, weekendType: o.v }))} className={chip(form.weekendType === o.v)}>{o.l}</button>
              ))}
            </div>
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
                <button key={o.v} type="button" onClick={() => setForm((p) => ({ ...p, travelMode: o.v }))} className={chip(form.travelMode === o.v)}>{o.l}</button>
              ))}
            </div>
            {form.travelMode === "GROUP" && (
              <input name="groupSize" type="number" min={2} value={form.groupSize} onChange={handleChange} placeholder="How many people?" aria-label="Group size" className="w-full rounded-xl border px-4 py-2.5 sm:max-w-sm" />
            )}
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
        <button className="btn btn-primary w-full sm:w-auto" onClick={handleSubmit} disabled={loadingTrip || loadingUser}>
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
