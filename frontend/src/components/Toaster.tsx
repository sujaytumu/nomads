"use client";

import { useEffect, useState } from "react";
import type { ToastKind } from "@/lib/toast";

type Item = { id: number; message: string; kind: ToastKind };

const STYLE: Record<ToastKind, string> = {
  success: "border-emerald-400/40 bg-emerald-950/90 text-emerald-100",
  error: "border-rose-400/40 bg-rose-950/90 text-rose-100",
  info: "border-brand-400/40 bg-slate-900/95 text-slate-100",
};
const MARK: Record<ToastKind, string> = { success: "✓", error: "!", info: "i" };

/** Renders the messages sent with toast(). Sits above the phone tab bar. */
export default function Toaster() {
  const [items, setItems] = useState<Item[]>([]);

  useEffect(() => {
    let n = 0;
    const onToast = (e: Event) => {
      const { message, kind } = (e as CustomEvent).detail || {};
      if (!message) return;
      const id = ++n;
      setItems((cur) => [...cur.slice(-2), { id, message: String(message), kind: kind || "info" }]);
      setTimeout(() => setItems((cur) => cur.filter((t) => t.id !== id)), kind === "error" ? 5000 : 3000);
    };
    window.addEventListener("nomad:toast", onToast);
    return () => window.removeEventListener("nomad:toast", onToast);
  }, []);

  if (items.length === 0) return null;
  return (
    <div aria-live="polite" className="pointer-events-none fixed inset-x-0 bottom-[calc(5.5rem+env(safe-area-inset-bottom))] z-50 flex flex-col items-center gap-2 px-4 lg:bottom-6">
      {items.map((t) => (
        <button
          key={t.id}
          type="button"
          onClick={() => setItems((cur) => cur.filter((x) => x.id !== t.id))}
          className={`animate-pop-in pointer-events-auto flex max-w-md items-center gap-2.5 rounded-2xl border px-4 py-2.5 text-left text-sm font-medium shadow-2xl shadow-black/50 backdrop-blur-xl ${STYLE[t.kind]}`}
        >
          <span aria-hidden="true" className="grid h-5 w-5 shrink-0 place-items-center rounded-full bg-white/15 text-[11px] font-bold">{MARK[t.kind]}</span>
          <span>{t.message}</span>
        </button>
      ))}
    </div>
  );
}
