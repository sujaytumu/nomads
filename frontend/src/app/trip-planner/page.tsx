"use client";

import { useState } from "react";

import ProtectedPage from "@/components/ProtectedPage";
import { createTrip } from "@/lib/tripApi";
import { fetchUser } from "@/lib/userApi";

export default function TripPlannerPage() {
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
  const [result, setResult] = useState<string | null>(null);
  const [loadingUser, setLoadingUser] = useState(false);
  const [loadingTrip, setLoadingTrip] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [cityLocked, setCityLocked] = useState(false);

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
    setResult(null);
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
    };

    try {
      const data = await createTrip(payload);
      setResult(`Trip created with id ${data.tripRequestId}`);
    } catch (error) {
      setError("Failed to create trip");
    } finally {
      setLoadingTrip(false);
    }
  };

  const handleLoadUser = async () => {
    if (!form.userId) {
      setError("User ID is required");
      return;
    }
    setError(null);
    setLoadingUser(true);
    try {
      const user = await fetchUser(Number(form.userId));
      setForm((prev) => ({
        ...prev,
        city: user.city ?? "",
        userLatitude: user.latitude?.toString() ?? "",
        userLongitude: user.longitude?.toString() ?? "",
        interest: user.interestType ?? prev.interest,
        travelMode: user.travelPreference ?? prev.travelMode,
      }));
      setCityLocked(true);
    } catch (err) {
      setError("Failed to load user");
    } finally {
      setLoadingUser(false);
    }
  };

  return (
    <ProtectedPage>
      <div className="section py-12">
        <div className="card p-8 space-y-6">
        <div>
          <h2 className="text-2xl font-bold">Trip Planner</h2>
          <p className="text-slate-600">Create a weekend trip request with preferences.</p>
        </div>

        <div className="grid md:grid-cols-2 gap-4">
          <label className="space-y-2">
            <span className="text-sm font-semibold">User ID</span>
            <div className="flex gap-3">
              <input name="userId" value={form.userId} onChange={handleChange} className="w-full border rounded-xl px-4 py-2" />
              <button className="btn-outline" type="button" onClick={handleLoadUser} disabled={loadingUser}>
                {loadingUser ? "Loading..." : "Load"}
              </button>
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
              <p className="text-xs text-slate-500">City is locked to user profile</p>
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

        <button className="btn-primary" onClick={handleSubmit} disabled={loadingTrip}>
          {loadingTrip ? "Creating..." : "Create Trip"}
        </button>
        {result && <p className="text-sm text-slate-600">{result}</p>}
        {error && <p className="text-sm text-red-600">{error}</p>}
        </div>
      </div>
    </ProtectedPage>
  );
}
