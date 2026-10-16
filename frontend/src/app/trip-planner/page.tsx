"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";

import ProtectedPage from "@/components/ProtectedPage";
import TripSteps from "@/components/TripSteps";
import { createTrip } from "@/lib/tripApi";
import { fetchMe } from "@/lib/authApi";
import { formatCost } from "@/lib/tripStatus";

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
    fetchMe()
      .then((user) => {
        setForm((prev) => ({
          ...prev,
          userId: String(user.id),
          city: user.city ?? "",
          userLatitude: user.latitude?.toString() ?? "",
          userLongitude: user.longitude?.toString() ?? "",
          interest: user.interestType ?? prev.interest,
          travelMode: user.travelPreference ?? prev.travelMode,
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
        <div className="card p-8 space-y-6">
        <TripSteps current={created ? 2 : 1} tripId={created?.tripRequestId} />
        <div>
          <h2 className="text-2xl font-bold">Trip Planner</h2>
          <p className="text-slate-300">
            Tell us what you like and we'll build a weekend itinerary. Nothing is booked until you pay.
          </p>
        </div>

        <div className="grid md:grid-cols-2 gap-4">
          <label className="space-y-2">
            <span className="text-sm font-semibold">Your Account</span>
            <div className="w-full border rounded-xl px-4 py-2 bg-slate-900 text-slate-300 text-sm">
              {loadingUser ? "Loading your profile… (may take up to a minute if the server was asleep)" : form.userId ? `Logged in (user #${form.userId})` : "Not loaded"}
            </div>
          </label>
          <label className="space-y-2">
            <span className="text-sm font-semibold">City</span>
            <input
              name="city"
              value={form.city}
              onChange={handleChange}
              className="w-full border rounded-xl px-4 py-2"
              disabled={cityLocked}
            />
            {cityLocked && (
              <p className="text-xs text-slate-400">City is locked to user profile</p>
            )}
          </label>
          <label className="space-y-2">
            <span className="text-sm font-semibold">Weekend Type</span>
            <select name="weekendType" value={form.weekendType} onChange={handleChange} className="w-full border rounded-xl px-4 py-2">
              <option value="ONE_DAY">One Day</option>
              <option value="TWO_DAY">Two Day</option>
            </select>
          </label>
          <label className="space-y-2">
            <span className="text-sm font-semibold">Interest</span>
            <select name="interest" value={form.interest} onChange={handleChange} className="w-full border rounded-xl px-4 py-2">
              {[
                "FOOD",
                "CULTURE",
                "NATURE",
                "ADVENTURE",
                "SHOPPING",
                "NIGHTLIFE",
                "RELAXATION",
              ].map((interest) => (
                <option key={interest} value={interest}>
                  {interest}
                </option>
              ))}
            </select>
          </label>
          <label className="space-y-2">
            <span className="text-sm font-semibold">Travel Mode</span>
            <select name="travelMode" value={form.travelMode} onChange={handleChange} className="w-full border rounded-xl px-4 py-2">
              <option value="SOLO">Solo</option>
              <option value="GROUP">Group</option>
            </select>
          </label>
          <label className="space-y-2">
            <span className="text-sm font-semibold">Group Size (optional)</span>
            <input name="groupSize" value={form.groupSize} onChange={handleChange} className="w-full border rounded-xl px-4 py-2" />
          </label>
          <label className="space-y-2">
            <span className="text-sm font-semibold">Latitude</span>
            <input
              name="userLatitude"
              value={form.userLatitude}
              onChange={handleChange}
              className="w-full border rounded-xl px-4 py-2"
              disabled={cityLocked}
            />
          </label>
          <label className="space-y-2">
            <span className="text-sm font-semibold">Longitude</span>
            <input
              name="userLongitude"
              value={form.userLongitude}
              onChange={handleChange}
              className="w-full border rounded-xl px-4 py-2"
              disabled={cityLocked}
            />
          </label>
          <label className="flex items-center gap-3">
            <input type="checkbox" name="pickupRequired" checked={form.pickupRequired} onChange={handleChange} />
            <span className="text-sm font-semibold">Pickup Assistance</span>
          </label>
        </div>

        <label className="block max-w-xs space-y-2">
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
