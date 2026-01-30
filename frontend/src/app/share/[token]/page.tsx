"use client";

import { fetchSharedTrip } from "@/lib/shareApi";
import { useEffect, useState } from "react";

export default function ShareTripPage({ params }: { params: { token: string } }) {
  const [trip, setTrip] = useState<any>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    fetchSharedTrip(params.token)
      .then((data) => setTrip(data))
      .catch(() => setError("Shared trip not found"));
  }, [params.token]);

  return (
    <div className="section py-12 space-y-6">
      <div className="card p-6 space-y-4">
        <h2 className="text-2xl font-bold">Shared Trip</h2>
        {error && <p className="text-sm text-red-600">{error}</p>}
        {trip && (
          <div className="space-y-3">
            <p className="text-sm text-slate-600">Trip ID: {trip.tripRequestId}</p>
            <p className="text-sm text-slate-600">City: {trip.city}</p>
            <div className="grid gap-3">
              {trip.plans?.map((plan: any, index: number) => (
                <div key={`${plan.placeId}-${index}`} className="border rounded-xl p-4">
                  <p className="font-semibold">Day {plan.dayNumber} - {plan.placeName}</p>
                  <p className="text-sm text-slate-600">{plan.startTime} - {plan.endTime}</p>
                </div>
              ))}
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
