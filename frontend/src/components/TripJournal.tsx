"use client";

import { useEffect, useMemo, useState } from "react";

import { Bone } from "@/components/Skeleton";
import { addNote, deleteNote, downloadJournal, fetchNotes, updateNote, type JournalNote } from "@/lib/journalApi";
import { errorText, toast } from "@/lib/toast";

const MOODS = ["😍", "🤩", "😋", "😎", "😴", "🥵", "🌧️", "🥹"];

/** A small diary for a booked trip: a line or two per moment, grouped by day, downloadable as text. */
export default function TripJournal({ tripId, days, city }: { tripId: number | string; days: number; city?: string | null }) {
  const [notes, setNotes] = useState<JournalNote[] | null>(null);
  const [failed, setFailed] = useState(false);
  const [text, setText] = useState("");
  const [day, setDay] = useState(1);
  const [mood, setMood] = useState("");
  const [editing, setEditing] = useState<number | null>(null);
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    let live = true;
    fetchNotes(tripId).then((n) => live && setNotes(n)).catch(() => live && setFailed(true));
    return () => {
      live = false;
    };
  }, [tripId]);

  const byDay = useMemo(() => {
    const m = new Map<number, JournalNote[]>();
    (notes || []).forEach((n) => m.set(n.dayNumber, [...(m.get(n.dayNumber) || []), n]));
    return Array.from(m.entries()).sort((a, b) => a[0] - b[0]);
  }, [notes]);

  async function save() {
    const t = text.trim();
    if (!t) return;
    setBusy(true);
    try {
      if (editing !== null) {
        const u = await updateNote(tripId, editing, { text: t, dayNumber: day, mood });
        setNotes((cur) => (cur || []).map((n) => (n.id === u.id ? u : n)));
        toast("Note updated", "success");
      } else {
        const c = await addNote(tripId, { text: t, dayNumber: day, mood });
        setNotes((cur) => [...(cur || []), c]);
        toast("Added to your journal", "success");
      }
      setText("");
      setMood("");
      setEditing(null);
    } catch (e) {
      toast(errorText(e, "Couldn't save that note."), "error");
    } finally {
      setBusy(false);
    }
  }

  async function remove(id: number) {
    const before = notes;
    setNotes((cur) => (cur || []).filter((n) => n.id !== id));
    try {
      await deleteNote(tripId, id);
    } catch (e) {
      setNotes(before);
      toast(errorText(e, "Couldn't delete that note."), "error");
    }
  }

  function startEdit(n: JournalNote) {
    setEditing(n.id);
    setText(n.text);
    setDay(n.dayNumber);
    setMood(n.mood || "");
  }

  if (failed) return null;
  if (!notes) return <Bone className="h-40 w-full rounded-3xl" />;

  const total = notes.length;
  return (
    <section className="card space-y-4 p-6" aria-label="Trip journal">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <h3 className="text-lg font-semibold">Trip journal</h3>
        {total > 0 && (
          <button type="button" className="btn btn-secondary btn-sm" onClick={() => downloadJournal(tripId, city).catch((e) => toast(errorText(e, "Couldn't download."), "error"))}>
            Download diary
          </button>
        )}
      </div>

      <div className="space-y-2 rounded-2xl bg-slate-800/60 p-3">
        <textarea value={text} onChange={(e) => setText(e.target.value.slice(0, 1000))} rows={2} placeholder="What's happening right now?" aria-label="Journal note" className="w-full rounded-xl border-0 bg-slate-900 p-3 text-sm" />
        <div className="flex flex-wrap items-center gap-2">
          <div className="flex gap-1" role="group" aria-label="Mood">
            {MOODS.map((m) => (
              <button key={m} type="button" aria-pressed={mood === m} onClick={() => setMood(mood === m ? "" : m)} className={`grid h-8 w-8 place-items-center rounded-lg text-lg transition-transform active:scale-90 ${mood === m ? "bg-brand-600/40 ring-1 ring-brand-400" : "bg-slate-900"}`}>{m}</button>
            ))}
          </div>
          <select value={day} onChange={(e) => setDay(Number(e.target.value))} aria-label="Day" className="ml-auto rounded-lg border-0 bg-slate-900 px-2 py-1.5 text-sm">
            {Array.from({ length: Math.max(1, days) }, (_, i) => <option key={i + 1} value={i + 1}>Day {i + 1}</option>)}
          </select>
          <button type="button" className="btn btn-primary btn-sm" disabled={busy || !text.trim()} data-loading={busy} onClick={save}>{editing !== null ? "Update" : "Add"}</button>
          {editing !== null && <button type="button" className="btn btn-ghost btn-sm" onClick={() => { setEditing(null); setText(""); setMood(""); }}>Cancel</button>}
        </div>
      </div>

      {total === 0 ? (
        <p className="text-sm text-slate-400">Nothing yet. Jot down a favourite moment, a great meal or a tip for next time.</p>
      ) : (
        byDay.map(([d, list]) => (
          <div key={d}>
            <p className="eyebrow mb-1.5">Day {d}</p>
            <ul className="space-y-2">
              {list.map((n) => (
                <li key={n.id} className="flex items-start gap-3 rounded-2xl border border-slate-800 px-3 py-2.5">
                  <span aria-hidden="true" className="text-xl leading-6">{n.mood || "📝"}</span>
                  <div className="min-w-0 flex-1">
                    <p className="whitespace-pre-wrap break-words text-sm">{n.text}</p>
                    <p className="mt-0.5 text-[11px] text-slate-500">{new Date(n.createdAt).toLocaleString("en-IN", { day: "numeric", month: "short", hour: "2-digit", minute: "2-digit" })}</p>
                  </div>
                  <button type="button" aria-label="Edit note" className="text-xs font-semibold text-[#0095f6]" onClick={() => startEdit(n)}>Edit</button>
                  <button type="button" aria-label="Delete note" className="text-xs font-semibold text-[#ed4956]" onClick={() => remove(n.id)}>Delete</button>
                </li>
              ))}
            </ul>
          </div>
        ))
      )}
    </section>
  );
}
