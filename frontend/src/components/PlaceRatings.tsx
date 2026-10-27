"use client";

import { useCallback, useEffect, useState } from "react";

import { Bone } from "@/components/Skeleton";
import { deletePlaceRating, fetchMyRating, fetchPlaceRatings, savePlaceRating, type MyRating, type PlaceRatings } from "@/lib/placeRatingApi";
import { errorText, toast } from "@/lib/toast";

function Stars({ value, size = "text-base" }: { value: number; size?: string }) {
  return (
    <span aria-label={`${value} out of 5 stars`} className={`${size} tracking-tight text-amber-300`}>
      {"★".repeat(Math.round(value))}
      <span className="text-slate-600">{"★".repeat(5 - Math.round(value))}</span>
    </span>
  );
}

/** What travellers say about a place, plus a way to rate it once you have been there. */
export default function PlaceRatingsSection({ placeId }: { placeId: number | string }) {
  const [data, setData] = useState<PlaceRatings | null>(null);
  const [mine, setMine] = useState<MyRating | null>(null);
  const [signedIn, setSignedIn] = useState(false);
  const [stars, setStars] = useState(0);
  const [comment, setComment] = useState("");
  const [busy, setBusy] = useState(false);
  const [failed, setFailed] = useState(false);

  const load = useCallback(() => {
    fetchPlaceRatings(placeId).then(setData).catch(() => setFailed(true));
    const token = typeof window !== "undefined" && !!localStorage.getItem("nomad_token");
    setSignedIn(token);
    if (token) {
      fetchMyRating(placeId)
        .then((m) => {
          setMine(m);
          setStars(m.stars || 0);
          setComment(m.comment || "");
        })
        .catch(() => setMine(null));
    }
  }, [placeId]);

  useEffect(load, [load]);

  async function submit() {
    if (!stars) return toast("Tap a star first", "info");
    setBusy(true);
    try {
      const m = await savePlaceRating(placeId, stars, comment);
      setMine(m);
      toast(mine?.stars ? "Rating updated" : "Thanks for rating!", "success");
      fetchPlaceRatings(placeId).then(setData).catch(() => undefined);
    } catch (e) {
      toast(errorText(e, "Couldn't save your rating."), "error");
    } finally {
      setBusy(false);
    }
  }

  async function remove() {
    setBusy(true);
    try {
      await deletePlaceRating(placeId);
      setMine((m) => (m ? { ...m, stars: null, comment: null } : m));
      setStars(0);
      setComment("");
      fetchPlaceRatings(placeId).then(setData).catch(() => undefined);
      toast("Rating removed", "info");
    } catch (e) {
      toast(errorText(e, "Couldn't remove it."), "error");
    } finally {
      setBusy(false);
    }
  }

  if (failed) return null;
  if (!data) return <Bone className="mt-6 h-40 w-full rounded-3xl" />;

  const max = Math.max(1, ...data.distribution);

  return (
    <section className="card mt-6 space-y-5 p-5" aria-label="Traveller ratings">
      <div className="flex flex-wrap items-center gap-6">
        <div>
          <p className="font-display text-4xl font-semibold">{data.blended.toFixed(1)}</p>
          <Stars value={data.blended} size="text-lg" />
          <p className="mt-0.5 text-xs text-slate-400">{data.count === 0 ? "No traveller ratings yet" : `${data.count} traveller ${data.count === 1 ? "rating" : "ratings"}`}</p>
        </div>
        {data.count > 0 && (
          <ul className="min-w-[12rem] flex-1 space-y-1" aria-label="Rating breakdown">
            {[5, 4, 3, 2, 1].map((s) => (
              <li key={s} className="flex items-center gap-2 text-xs text-slate-400">
                <span className="w-3 text-right">{s}</span>
                <span className="h-1.5 flex-1 overflow-hidden rounded-full bg-slate-800">
                  <span className="block h-full rounded-full bg-amber-300 transition-all duration-700" style={{ width: `${(data.distribution[s - 1] / max) * 100}%` }} />
                </span>
                <span className="w-5">{data.distribution[s - 1]}</span>
              </li>
            ))}
          </ul>
        )}
      </div>

      {/* Rate it */}
      <div className="rounded-2xl bg-slate-800/60 p-4">
        {!signedIn ? (
          <p className="text-sm text-slate-300">Log in to rate places you have visited.</p>
        ) : mine && !mine.canRate ? (
          <p className="text-sm text-slate-300">You can rate this place after booking a trip that includes it.</p>
        ) : (
          <div className="space-y-3">
            <p className="text-sm font-semibold">{mine?.stars ? "Your rating" : "Rate this place"}</p>
            <div className="flex gap-1" role="radiogroup" aria-label="Your rating">
              {[1, 2, 3, 4, 5].map((n) => (
                <button key={n} type="button" role="radio" aria-checked={stars === n} aria-label={`${n} star${n > 1 ? "s" : ""}`} onClick={() => setStars(n)} className={`icon-btn h-10 w-10 text-3xl leading-none ${n <= stars ? "text-amber-300" : "text-slate-600"}`}>★</button>
              ))}
            </div>
            <textarea value={comment} onChange={(e) => setComment(e.target.value.slice(0, 500))} rows={2} placeholder="Add a comment (optional)" aria-label="Comment" className="w-full rounded-xl border-0 bg-slate-900 p-3 text-sm" />
            <div className="flex items-center gap-2">
              <button type="button" className="btn btn-primary btn-sm" disabled={busy || !stars} data-loading={busy} onClick={submit}>{mine?.stars ? "Update" : "Post rating"}</button>
              {mine?.stars ? <button type="button" className="btn btn-ghost btn-sm" disabled={busy} onClick={remove}>Remove</button> : null}
              <span className="ml-auto text-xs text-slate-500">{comment.length}/500</span>
            </div>
          </div>
        )}
      </div>

      {data.reviews.length > 0 && (
        <ul className="space-y-3">
          {data.reviews.map((r, i) => (
            <li key={i} className="border-t border-slate-800 pt-3 first:border-0 first:pt-0">
              <div className="flex items-center justify-between gap-2">
                <p className="text-sm font-semibold">{r.reviewer}</p>
                <Stars value={r.stars} size="text-sm" />
              </div>
              <p className="mt-0.5 text-sm text-slate-300">{r.comment}</p>
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}
