"use client";

import { useState } from "react";

import { api } from "@/lib/api";

export default function DownloadItineraryButton({ tripId }: { tripId: number }) {
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const download = async () => {
    setBusy(true);
    setError(null);
    try {
      // Authenticated fetch (JWT header) - a plain <a href> couldn't send the token.
      const res = await api.get(`/api/trips/${tripId}/itinerary.pdf`, { responseType: "blob" });
      const url = URL.createObjectURL(res.data);
      const a = document.createElement("a");
      a.href = url;
      a.download = `itinerary-${tripId}.pdf`;
      document.body.appendChild(a);
      a.click();
      a.remove();
      URL.revokeObjectURL(url);
    } catch {
      setError("Couldn't download itinerary");
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="pt-2">
      <button className="btn-outline" onClick={download} disabled={busy}>
        {busy ? "Preparing..." : "Download itinerary (PDF)"}
      </button>
      {error && <p className="text-sm text-red-400 mt-1">{error}</p>}
    </div>
  );
}
