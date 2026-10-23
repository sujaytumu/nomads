"use client";

import { useState } from "react";

import { downloadItineraryPdf } from "@/lib/tripApi";

type Props = {
  tripId: number | string;
  label?: string;
  className?: string;
};

export default function DownloadItineraryButton({
  tripId,
  label = "Download itinerary (PDF)",
  className = "btn-outline",
}: Props) {
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleClick = async () => {
    setBusy(true);
    setError(null);
    try {
      await downloadItineraryPdf(Number(tripId));
    } catch (err: any) {
      setError(err?.message || "Couldn't create the PDF.");
    } finally {
      setBusy(false);
    }
  };

  return (
    <span className="inline-flex flex-col items-start gap-1">
      <button type="button" className={className} onClick={handleClick} disabled={busy} data-loading={busy}>
        {busy ? "Preparing PDF…" : label}
      </button>
      {error && (
        <span className="text-xs text-red-400" role="alert">
          {error}
        </span>
      )}
    </span>
  );
}
