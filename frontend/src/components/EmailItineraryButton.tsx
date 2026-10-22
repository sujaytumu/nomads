"use client";

import { useState } from "react";

import { emailItinerary } from "@/lib/tripApi";

/** Re-sends the booking email. Says honestly whether it was sent. */
export default function EmailItineraryButton({ tripId, className = "btn btn-secondary btn-sm" }: { tripId: number | string; className?: string }) {
  const [busy, setBusy] = useState(false);
  const [result, setResult] = useState<null | { ok: boolean; text: string }>(null);

  const send = async () => {
    setBusy(true);
    setResult(null);
    try {
      const sent = await emailItinerary(Number(tripId));
      setResult(
        sent
          ? { ok: true, text: "Sent — check your inbox." }
          : { ok: false, text: "Email couldn't be sent (not configured, or the provider refused it). Download the PDF instead." }
      );
    } catch (err: any) {
      setResult({ ok: false, text: err?.response?.data?.message || "Couldn't send the email." });
    } finally {
      setBusy(false);
    }
  };

  return (
    <span className="inline-flex flex-col items-start gap-1">
      <button type="button" className={className} onClick={send} disabled={busy} data-loading={busy} data-done={result?.ok ? "true" : undefined}>
        {busy ? "Sending…" : result?.ok ? "Sent ✓" : "Email itinerary"}
      </button>
      {result && (
        <span role="status" className={`max-w-xs text-xs ${result.ok ? "text-green-300" : "text-red-400"}`}>{result.text}</span>
      )}
    </span>
  );
}
