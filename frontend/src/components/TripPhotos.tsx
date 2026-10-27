"use client";

import { useEffect, useRef, useState } from "react";
import { addPhoto, compressImage, deletePhoto, fetchPhotos, MAX_PHOTOS, TripPhoto } from "@/lib/photoApi";
import { errorText, toast } from "@/lib/toast";

/** Photo gallery for one trip. The owner can add and remove pictures; they also show on the shared page. */
export default function TripPhotos({ tripId }: { tripId: number }) {
  const [photos, setPhotos] = useState<TripPhoto[] | null>(null);
  const [busy, setBusy] = useState(false);
  const [open, setOpen] = useState<TripPhoto | null>(null);
  const input = useRef<HTMLInputElement>(null);

  useEffect(() => {
    let active = true;
    setPhotos(null);
    fetchPhotos(tripId)
      .then((p) => active && setPhotos(p))
      .catch(() => active && setPhotos([]));
    return () => {
      active = false;
    };
  }, [tripId]);

  const onPick = async (files: FileList | null) => {
    if (!files || files.length === 0) return;
    const list: File[] = [];
    for (let i = 0; i < files.length; i++) list.push(files[i]);
    setBusy(true);
    let added = 0;
    try {
      for (const f of list) {
        if ((photos?.length ?? 0) + added >= MAX_PHOTOS) {
          toast(`A trip can hold up to ${MAX_PHOTOS} photos`, "info");
          break;
        }
        const dataUrl = await compressImage(f);
        const saved = await addPhoto(tripId, dataUrl);
        added++;
        setPhotos((prev) => [...(prev ?? []), saved]);
      }
      if (added) toast(added === 1 ? "Photo added" : `${added} photos added`, "success");
    } catch (e: any) {
      toast(errorText(e, "Couldn't add that photo"), "error");
    } finally {
      setBusy(false);
      if (input.current) input.current.value = "";
    }
  };

  const remove = async (p: TripPhoto) => {
    try {
      await deletePhoto(tripId, p.id);
      setPhotos((prev) => (prev ?? []).filter((x) => x.id !== p.id));
      setOpen(null);
      toast("Photo removed", "info");
    } catch (e: any) {
      toast(errorText(e, "Couldn't remove that photo"), "error");
    }
  };

  const count = photos?.length ?? 0;

  return (
    <section className="card space-y-4 p-6">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h3 className="text-lg font-semibold">Trip photos</h3>
          <p className="text-sm text-slate-400">
            {count === 0 ? "Add pictures from your trip. They also show on your shared link." : `${count} of ${MAX_PHOTOS} photos`}
          </p>
        </div>
        <button
          type="button"
          className="btn btn-secondary"
          disabled={busy || count >= MAX_PHOTOS}
          data-loading={busy ? "true" : undefined}
          onClick={() => input.current?.click()}
        >
          {busy ? "Adding…" : "Add photos"}
        </button>
        <input
          ref={input}
          type="file"
          accept="image/*"
          multiple
          className="hidden"
          onChange={(e) => onPick(e.target.files)}
        />
      </div>

      {photos === null && <div className="h-24 animate-pulse rounded-xl bg-slate-800/60" />}

      {count > 0 && (
        <ul className="grid grid-cols-3 gap-2 sm:grid-cols-4">
          {(photos ?? []).map((p) => (
            <li key={p.id}>
              <button
                type="button"
                onClick={() => setOpen(p)}
                className="block aspect-square w-full overflow-hidden rounded-xl border border-slate-700"
                aria-label={p.caption ? `Open photo: ${p.caption}` : "Open photo"}
              >
                {/* eslint-disable-next-line @next/next/no-img-element */}
                <img src={p.dataUrl} alt={p.caption || "Trip photo"} className="h-full w-full object-cover" loading="lazy" />
              </button>
            </li>
          ))}
        </ul>
      )}

      {open && (
        <div
          role="dialog"
          aria-modal="true"
          className="fixed inset-0 z-50 flex flex-col items-center justify-center gap-4 bg-black/90 p-4"
          onClick={() => setOpen(null)}
        >
          {/* eslint-disable-next-line @next/next/no-img-element */}
          <img src={open.dataUrl} alt={open.caption || "Trip photo"} className="max-h-[75vh] max-w-full rounded-xl object-contain" />
          <div className="flex gap-3" onClick={(e) => e.stopPropagation()}>
            <button type="button" className="btn btn-secondary" onClick={() => setOpen(null)}>Close</button>
            <button type="button" className="btn btn-secondary text-red-400" onClick={() => remove(open)}>Delete</button>
          </div>
        </div>
      )}
    </section>
  );
}
