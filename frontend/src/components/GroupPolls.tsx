"use client";

import { useCallback, useEffect, useState } from "react";
import { closePoll, createPoll, fetchPolls, votePoll, type Poll } from "@/lib/pollApi";
import { errorText, toast } from "@/lib/toast";

/** Group polls: ask the group to choose a place, a date or a plan. One vote each, changeable until the poll closes. */
export default function GroupPolls({ groupId }: { groupId: number }) {
  const [polls, setPolls] = useState<Poll[] | null>(null);
  const [failed, setFailed] = useState(false);
  const [adding, setAdding] = useState(false);
  const [question, setQuestion] = useState("");
  const [choices, setChoices] = useState(["", ""]);
  const [saving, setSaving] = useState(false);
  const [voting, setVoting] = useState<number | null>(null);

  const load = useCallback(() => {
    setFailed(false);
    fetchPolls(groupId)
      .then(setPolls)
      .catch(() => setFailed(true));
  }, [groupId]);

  useEffect(() => {
    setPolls(null);
    load();
    // Other members vote while you are looking: refresh quietly.
    const timer = setInterval(() => document.visibilityState === "visible" && load(), 20000);
    return () => clearInterval(timer);
  }, [load]);

  const replace = (p: Poll) => setPolls((cur) => (cur || []).map((x) => (x.id === p.id ? p : x)));
  const filled = choices.map((c) => c.trim()).filter(Boolean);
  const canSave = question.trim().length >= 3 && new Set(filled.map((c) => c.toLowerCase())).size >= 2 && !saving;

  const submit = async () => {
    setSaving(true);
    try {
      const p = await createPoll(groupId, question.trim(), filled);
      setPolls((cur) => [p, ...(cur || [])]);
      setQuestion("");
      setChoices(["", ""]);
      setAdding(false);
      toast("Poll posted. Your group can vote now.", "success");
    } catch (e) {
      toast(errorText(e, "Couldn't create the poll."), "error");
    } finally {
      setSaving(false);
    }
  };

  const vote = async (p: Poll, optionId: number) => {
    if (p.closed || p.myOptionId === optionId || voting) return;
    setVoting(p.id);
    try {
      replace(await votePoll(groupId, p.id, optionId));
    } catch (e) {
      toast(errorText(e, "Couldn't save your vote."), "error");
    } finally {
      setVoting(null);
    }
  };

  const close = async (p: Poll) => {
    if (!confirm("Close this poll? Nobody can vote after that.")) return;
    try {
      replace(await closePoll(groupId, p.id));
      toast("Poll closed", "success");
    } catch (e) {
      toast(errorText(e, "Couldn't close the poll."), "error");
    }
  };

  return (
    <div className="space-y-4">
      {!adding ? (
        <button type="button" className="btn btn-primary btn-sm" onClick={() => setAdding(true)}>+ New poll</button>
      ) : (
        <div className="card space-y-3 p-4">
          <label className="block space-y-1 text-sm font-semibold">
            Question
            <input value={question} onChange={(e) => setQuestion(e.target.value)} maxLength={200} placeholder="Where should we eat on Saturday?" className="mt-1 w-full rounded-xl border px-4 py-2.5 font-normal" />
          </label>
          <div className="space-y-2">
            <p className="text-sm font-semibold">Choices</p>
            {choices.map((c, i) => (
              <div key={i} className="flex gap-2">
                <input
                  value={c}
                  onChange={(e) => setChoices((cur) => cur.map((x, j) => (j === i ? e.target.value : x)))}
                  maxLength={80}
                  placeholder={`Choice ${i + 1}`}
                  aria-label={`Choice ${i + 1}`}
                  className="w-full rounded-xl border px-4 py-2"
                />
                {choices.length > 2 && (
                  <button type="button" aria-label={`Remove choice ${i + 1}`} className="btn btn-ghost btn-sm" onClick={() => setChoices((cur) => cur.filter((_, j) => j !== i))}>✕</button>
                )}
              </div>
            ))}
            {choices.length < 6 && (
              <button type="button" className="btn btn-secondary btn-sm" onClick={() => setChoices((cur) => [...cur, ""])}>+ Add choice</button>
            )}
          </div>
          <div className="flex flex-wrap gap-2">
            <button type="button" className="btn btn-primary btn-sm" disabled={!canSave} data-loading={saving} onClick={submit}>Post poll</button>
            <button type="button" className="btn btn-ghost btn-sm" onClick={() => setAdding(false)}>Cancel</button>
          </div>
        </div>
      )}

      {failed && (
        <div className="card flex items-center justify-between gap-3 p-4">
          <p className="text-sm text-red-400">Couldn&apos;t load polls.</p>
          <button type="button" className="btn btn-secondary btn-sm" onClick={load}>Retry</button>
        </div>
      )}
      {polls === null && !failed && <div className="card h-28 animate-pulse" />}
      {polls && polls.length === 0 && !adding && (
        <div className="card p-6 text-center text-sm text-slate-400">No polls yet. Start one to decide a place or a plan together.</div>
      )}

      {(polls || []).map((p) => (
        <div key={p.id} className="card space-y-3 p-4">
          <div className="flex items-start justify-between gap-3">
            <div className="min-w-0">
              <p className="font-semibold">{p.question}</p>
              <p className="text-xs text-slate-400">
                {p.createdByName} · {p.totalVotes} {p.totalVotes === 1 ? "vote" : "votes"}
                {p.closed ? " · closed" : ""}
              </p>
            </div>
            {p.mine && !p.closed && <button type="button" className="btn btn-ghost btn-sm shrink-0" onClick={() => close(p)}>Close</button>}
          </div>
          <ul className="space-y-2">
            {p.options.map((o) => {
              const pct = p.totalVotes ? Math.round((o.votes / p.totalVotes) * 100) : 0;
              const mine = p.myOptionId === o.id;
              const lead = p.totalVotes > 0 && o.votes === Math.max(...p.options.map((x) => x.votes));
              return (
                <li key={o.id}>
                  <button
                    type="button"
                    disabled={p.closed || voting === p.id}
                    aria-pressed={mine}
                    onClick={() => vote(p, o.id)}
                    className={`relative w-full overflow-hidden rounded-xl border px-3 py-2.5 text-left transition-colors disabled:cursor-default ${mine ? "border-brand-500 bg-brand-600/10" : "border-slate-800 bg-slate-900/60 hover:border-brand-500/50"}`}
                  >
                    <span aria-hidden="true" className={`absolute inset-y-0 left-0 transition-all duration-500 ${lead ? "bg-gradient-to-r from-brand-600/35 to-ocean-500/25" : "bg-slate-700/30"}`} style={{ width: `${pct}%` }} />
                    <span className="relative flex items-center justify-between gap-3 text-sm">
                      <span className="flex min-w-0 items-center gap-2 font-semibold">
                        <span aria-hidden="true" className={`grid h-5 w-5 shrink-0 place-items-center rounded-full border text-[11px] ${mine ? "border-brand-500 bg-brand-600 text-white" : "border-slate-600 text-transparent"}`}>✓</span>
                        <span className="truncate">{o.text}</span>
                      </span>
                      <span className="shrink-0 text-xs text-slate-300">{o.votes} · {pct}%</span>
                    </span>
                  </button>
                </li>
              );
            })}
          </ul>
          {!p.closed && <p className="text-xs text-slate-500">{p.myOptionId ? "Tap another choice to change your vote." : "Tap a choice to vote."}</p>}
        </div>
      ))}
    </div>
  );
}
