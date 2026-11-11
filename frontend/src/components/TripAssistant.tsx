"use client";

import { useEffect, useRef, useState } from "react";
import { askTripAssistant, type ChatTurn } from "@/lib/assistantApi";

type Msg = ChatTurn & { mode?: string };

const SUGGESTIONS = ["What's on day 1?", "What should I pack?", "Is the pace too tiring?", "Will it rain?", "How much will it cost?"];

/** Ask questions about this trip in plain words. Answers come from the trip's own plan. */
export default function TripAssistant({ tripId }: { tripId: number }) {
  const [open, setOpen] = useState(false);
  const [messages, setMessages] = useState<Msg[]>([]);
  const [text, setText] = useState("");
  const [busy, setBusy] = useState(false);
  const end = useRef<HTMLDivElement | null>(null);

  useEffect(() => {
    end.current?.scrollIntoView({ behavior: "smooth", block: "nearest" });
  }, [messages, busy]);

  const send = async (raw: string) => {
    const question = raw.trim();
    if (!question || busy) return;
    const history = messages.map(({ role, content }) => ({ role, content }));
    setMessages((m) => [...m, { role: "user", content: question }]);
    setText("");
    setBusy(true);
    try {
      const res = await askTripAssistant(tripId, question, history);
      setMessages((m) => [...m, { role: "assistant", content: res.answer, mode: res.mode }]);
    } catch (err: any) {
      const limited = err?.response?.status === 429;
      setMessages((m) => [
        ...m,
        { role: "assistant", content: limited ? "You've asked a lot of questions this hour. Please try again a little later." : "I couldn't answer that just now. The server may be waking up, so please try again in a moment.", mode: "error" },
      ]);
    } finally {
      setBusy(false);
    }
  };

  return (
    <section className="card space-y-3 p-5" aria-label="Ask about your trip">
      <button type="button" onClick={() => setOpen((o) => !o)} aria-expanded={open} className="flex w-full items-center justify-between text-left">
        <span>
          <span className="block font-display text-base font-semibold">💬 Ask about your trip</span>
          <span className="block text-xs text-slate-400">Day plans, packing, weather, cost, pace.</span>
        </span>
        <span aria-hidden="true" className="text-slate-400">{open ? "−" : "+"}</span>
      </button>

      {open && (
        <div className="space-y-3">
          {messages.length === 0 && (
            <div className="flex flex-wrap gap-2">
              {SUGGESTIONS.map((s) => (
                <button key={s} type="button" onClick={() => send(s)} className="rounded-full border border-slate-700 bg-slate-900/60 px-3 py-1.5 text-xs font-semibold text-slate-200 hover:bg-slate-800">
                  {s}
                </button>
              ))}
            </div>
          )}

          <div className="max-h-80 space-y-2 overflow-y-auto" role="log" aria-live="polite">
            {messages.map((m, i) => (
              <div key={i} className={`flex ${m.role === "user" ? "justify-end" : "justify-start"}`}>
                <p className={`max-w-[85%] whitespace-pre-wrap rounded-2xl px-3 py-2 text-sm ${m.role === "user" ? "bg-brand-600 text-white" : m.mode === "error" || m.mode === "limit" ? "bg-amber-500/10 text-amber-200" : "bg-slate-800 text-slate-100"}`}>
                  {m.content}
                </p>
              </div>
            ))}
            {busy && (
              <div className="flex justify-start" aria-label="Thinking">
                <span className="flex gap-1 rounded-2xl bg-slate-800 px-3 py-3" aria-hidden="true">
                  <span className="h-1.5 w-1.5 animate-bounce rounded-full bg-slate-400" />
                  <span className="h-1.5 w-1.5 animate-bounce rounded-full bg-slate-400 [animation-delay:120ms]" />
                  <span className="h-1.5 w-1.5 animate-bounce rounded-full bg-slate-400 [animation-delay:240ms]" />
                </span>
              </div>
            )}
            <div ref={end} />
          </div>

          <form
            className="flex gap-2"
            onSubmit={(e) => {
              e.preventDefault();
              send(text);
            }}
          >
            <input
              value={text}
              onChange={(e) => setText(e.target.value)}
              maxLength={500}
              placeholder="Ask anything about this trip…"
              aria-label="Your question"
              className="min-w-0 flex-1 rounded-xl border border-slate-700 bg-slate-900/60 px-3 py-2.5 text-sm outline-none focus:border-brand-500"
            />
            <button type="submit" disabled={busy || !text.trim()} className="btn btn-primary btn-sm">Ask</button>
          </form>
          <p className="text-[11px] text-slate-500">Answers can be wrong. Check opening hours and prices locally.</p>
        </div>
      )}
    </section>
  );
}
