"use client";

import { Suspense, useEffect, useState } from "react";

import PageHeader from "@/components/PageHeader";
import ProtectedPage from "@/components/ProtectedPage";
import TripPicker from "@/components/TripPicker";
import { createReview, fetchReviews } from "@/lib/reviewApi";
import { useTripSelection } from "@/lib/useTripSelection";

export default function ReviewPage() {
  return (
    <Suspense fallback={null}>
      <ReviewInner />
    </Suspense>
  );
}

function ReviewInner() {
  const sel = useTripSelection((t) => t.status === "CONFIRMED");
  const [rating, setRating] = useState(5);
  const [comment, setComment] = useState("");
  const [reviews, setReviews] = useState<any[]>([]);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [done, setDone] = useState(false);

  const id = sel.selected?.tripRequestId;
  useEffect(() => {
    setReviews([]);
    setDone(false);
    setError(null);
    if (id) fetchReviews(Number(id)).then(setReviews).catch(() => undefined);
  }, [id]);

  const submit = async () => {
    if (!id) return;
    setBusy(true);
    setError(null);
    try {
      await createReview({ tripRequestId: Number(id), rating, comment });
      setReviews(await fetchReviews(Number(id)));
      setComment("");
      setDone(true);
    } catch (e: any) {
      setError(e?.response?.data?.message || "Couldn't submit your review.");
    } finally {
      setBusy(false);
    }
  };

  return (
    <ProtectedPage>
      <div className="section space-y-6 py-8">
        <PageHeader title="Reviews" subtitle="Rate a trip you've booked. Only booked trips can be reviewed." />
        <div className="card space-y-5 p-6">
          <TripPicker trips={sel.trips} selected={sel.selected} onSelect={sel.select} loading={sel.loading} error={sel.error} onRetry={sel.reload} emptyText="You have no booked trips yet. Once a trip is paid and booked, you can review it here." />

          {sel.selected && (
            <div className="space-y-4">
              <div>
                <p className="mb-1.5 text-xs font-semibold uppercase tracking-wide text-slate-500">Your rating</p>
                <div className="flex gap-1" role="radiogroup" aria-label="Rating">
                  {[1, 2, 3, 4, 5].map((n) => (
                    <button key={n} type="button" role="radio" aria-checked={rating === n} aria-label={`${n} star${n > 1 ? "s" : ""}`} onClick={() => setRating(n)} className={`h-10 w-10 rounded-lg border text-xl transition-colors ${n <= rating ? "border-amber-400/50 bg-amber-400/10 text-amber-300" : "border-slate-700 text-slate-600 hover:bg-slate-800"}`}>
                      ★
                    </button>
                  ))}
                </div>
              </div>
              <textarea value={comment} onChange={(e) => setComment(e.target.value)} rows={3} placeholder="How was the trip? (optional)" className="w-full rounded-xl border px-4 py-2 text-sm" />
              <button type="button" className="btn btn-primary" onClick={submit} disabled={busy}>
                {busy ? "Submitting…" : "Submit review"}
              </button>
              {done && <p role="status" className="text-sm text-green-300">Thanks — your review was saved.</p>}
            </div>
          )}
          {error && <p role="alert" className="rounded-xl border border-red-500/20 bg-red-500/10 px-4 py-3 text-sm text-red-400">{error}</p>}
        </div>

        {sel.selected && (
          <section className="space-y-3">
            <h2 className="text-lg font-semibold">Reviews for this trip ({reviews.length})</h2>
            {reviews.length === 0 && <p className="text-sm text-slate-500">No reviews yet.</p>}
            {reviews.map((r) => (
              <div key={r.id} className="card p-4">
                <p className="text-amber-300">{"★".repeat(r.rating)}<span className="text-slate-700">{"★".repeat(Math.max(0, 5 - r.rating))}</span></p>
                {r.comment && <p className="mt-1 text-sm text-slate-300">{r.comment}</p>}
              </div>
            ))}
          </section>
        )}
      </div>
    </ProtectedPage>
  );
}
