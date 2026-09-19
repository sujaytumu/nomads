"use client";

import { useEffect, useState } from "react";

import ProtectedPage from "@/components/ProtectedPage";
import { api } from "@/lib/api";

type Booking = {
  tripRequestId: number;
  city: string;
  status: string;
  estimatedCost: number | null;
  createdAt: string;
  shareToken?: string;
};

const STATUS_STYLES: Record<string, string> = {
  CONFIRMED: "bg-green-500/15 text-green-300",
  PAYMENT_PENDING: "bg-yellow-500/15 text-yellow-300",
  CANCELLED: "bg-red-500/15 text-red-300",
};

export default function BookingHistoryPage() {
  const [bookings, setBookings] = useState<Booking[]>([]);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState(false);

  const load = () => {
    setLoading(true);
    setLoadError(false);
    api.get("/api/trips/me")
      .then((res) => setBookings(res.data || []))
      .catch(() => setLoadError(true))
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    load();
  }, []);

  return (
    <ProtectedPage>
      <div className="section py-12 space-y-6">
        <div className="card p-6 space-y-4">
          <h2 className="text-2xl font-bold">Booking History</h2>
          <p className="text-sm text-slate-300">Every trip you've created, past and pending.</p>

          {loading && (
            <p className="text-sm text-slate-400">Loading your bookings… (may take up to a minute if the server was asleep)</p>
          )}

          {!loading && loadError && (
            <div className="space-y-2">
              <p className="text-sm text-red-400">Couldn't load your bookings.</p>
              <button className="btn-outline" onClick={load}>Retry</button>
            </div>
          )}

          {!loading && !loadError && bookings.length === 0 && (
            <p className="text-sm text-slate-400">No bookings yet. Create one from Trip Planner.</p>
          )}

          {!loading && !loadError && bookings.length > 0 && (
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead>
                  <tr className="text-left border-b">
                    <th className="py-2 pr-4">Trip ID</th>
                    <th className="py-2 pr-4">City</th>
                    <th className="py-2 pr-4">Date</th>
                    <th className="py-2 pr-4">Status</th>
                    <th className="py-2 pr-4">Amount</th>
                    <th className="py-2 pr-4">Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {bookings.map((b) => (
                    <tr key={b.tripRequestId} className="border-b last:border-0">
                      <td className="py-3 pr-4">#{b.tripRequestId}</td>
                      <td className="py-3 pr-4">{b.city || "—"}</td>
                      <td className="py-3 pr-4">{b.createdAt ? new Date(b.createdAt).toLocaleDateString() : "—"}</td>
                      <td className="py-3 pr-4">
                        <span className={`px-2 py-1 rounded-full text-xs font-medium ${STATUS_STYLES[b.status] || "bg-slate-800 text-slate-200"}`}>
                          {b.status}
                        </span>
                      </td>
                      <td className="py-3 pr-4">{b.estimatedCost != null ? `₹${b.estimatedCost}` : "—"}</td>
                      <td className="py-3 pr-4 space-x-3">
                        <a href={`/trip-summary?tripId=${b.tripRequestId}`} className="text-indigo-600 hover:underline">View</a>
                        {b.status === "PAYMENT_PENDING" && (
                          <a href={`/payment?tripRequestId=${b.tripRequestId}&prefillAmount=${b.estimatedCost ?? ""}`} className="text-indigo-600 hover:underline">Pay</a>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      </div>
    </ProtectedPage>
  );
}
