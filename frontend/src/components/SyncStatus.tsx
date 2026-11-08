"use client";

import { useEffect, useState } from "react";
import { flushQueue, isOffline, queuedCount, QUEUE_EVENT } from "@/lib/writeQueue";

/** Sends saved-offline changes when the connection returns, and shows a small pill while anything is waiting. */
export default function SyncStatus() {
  const [count, setCount] = useState(0);
  const [offline, setOffline] = useState(false);

  useEffect(() => {
    const refresh = () => {
      setCount(queuedCount());
      setOffline(isOffline());
    };
    const tryFlush = () => {
      refresh();
      flushQueue().finally(refresh);
    };
    refresh();
    tryFlush();
    window.addEventListener("online", tryFlush);
    window.addEventListener("offline", refresh);
    window.addEventListener(QUEUE_EVENT, refresh);
    document.addEventListener("visibilitychange", () => document.visibilityState === "visible" && tryFlush());
    const timer = window.setInterval(tryFlush, 60000);
    return () => {
      window.removeEventListener("online", tryFlush);
      window.removeEventListener("offline", refresh);
      window.removeEventListener(QUEUE_EVENT, refresh);
      window.clearInterval(timer);
    };
  }, []);

  if (!offline && count === 0) return null;
  return (
    <div role="status" className="pointer-events-none fixed inset-x-0 top-16 z-40 flex justify-center px-4 lg:left-64">
      <span className="pointer-events-auto rounded-full border border-amber-400/30 bg-slate-900/95 px-4 py-1.5 text-xs font-semibold text-amber-300 shadow-lg backdrop-blur">
        {offline ? "You're offline" : "Sending your changes…"}
        {count > 0 ? ` · ${count} waiting to send` : ""}
      </span>
    </div>
  );
}
