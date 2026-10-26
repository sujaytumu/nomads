"use client";

import { useRouter } from "next/navigation";
import { useCallback, useEffect, useState } from "react";

import PageHeader from "@/components/PageHeader";
import ProtectedPage from "@/components/ProtectedPage";
import { Bone } from "@/components/Skeleton";
import { fetchInbox, markAllInboxRead, markInboxRead, type InboxItem } from "@/lib/inboxApi";

const ICONS: Record<string, string> = {
  BOOKING_CONFIRMED: "🎉",
  BOOKING_CANCELLED: "🛑",
  PAYMENT_FAILED: "⚠️",
};

function ago(iso: string) {
  const s = Math.max(1, Math.floor((Date.now() - new Date(iso).getTime()) / 1000));
  if (s < 60) return "just now";
  const m = Math.floor(s / 60);
  if (m < 60) return `${m}m`;
  const h = Math.floor(m / 60);
  if (h < 24) return `${h}h`;
  const d = Math.floor(h / 24);
  if (d < 7) return `${d}d`;
  return new Date(iso).toLocaleDateString("en-IN", { day: "numeric", month: "short" });
}

export default function NotificationsPage() {
  const router = useRouter();
  const [items, setItems] = useState<InboxItem[] | null>(null);
  const [unread, setUnread] = useState(0);
  const [page, setPage] = useState(0);
  const [more, setMore] = useState(false);
  const [error, setError] = useState(false);
  const [busy, setBusy] = useState(false);

  const load = useCallback((p: number) => {
    setBusy(true);
    fetchInbox(p, 20)
      .then((r) => {
        setItems((cur) => (p === 0 ? r.items : [...(cur || []), ...r.items]));
        setUnread(r.unread);
        setMore(r.hasMore);
        setPage(p);
        setError(false);
      })
      .catch(() => setError(true))
      .finally(() => setBusy(false));
  }, []);

  useEffect(() => load(0), [load]);

  const open = (n: InboxItem) => {
    if (!n.read) {
      setItems((cur) => (cur || []).map((x) => (x.id === n.id ? { ...x, read: true } : x)));
      setUnread((u) => Math.max(0, u - 1));
      markInboxRead(n.id).catch(() => undefined);
    }
    if (n.link) router.push(n.link);
  };

  const readAll = () => {
    setItems((cur) => (cur || []).map((x) => ({ ...x, read: true })));
    setUnread(0);
    markAllInboxRead().catch(() => load(0));
  };

  return (
    <ProtectedPage>
      <div className="section max-w-2xl space-y-4 py-8 pb-28">
        <PageHeader title="Notifications" subtitle="Bookings, payments and trip updates." />

        {unread > 0 && (
          <div className="flex items-center justify-between">
            <p className="text-sm text-slate-400">{unread} unread</p>
            <button type="button" className="btn btn-ghost btn-sm" onClick={readAll}>Mark all as read</button>
          </div>
        )}

        {items === null && !error && (
          <div className="space-y-2" aria-busy="true">
            {[0, 1, 2].map((i) => <Bone key={i} className="h-20 w-full rounded-2xl" />)}
          </div>
        )}

        {error && (
          <div className="card space-y-2 p-5">
            <p className="text-sm text-red-300">Couldn&apos;t load your notifications.</p>
            <button type="button" className="btn btn-secondary btn-sm" onClick={() => load(0)}>Retry</button>
          </div>
        )}

        {items && items.length === 0 && !error && (
          <div className="card p-8 text-center">
            <p className="text-4xl" aria-hidden="true">🔔</p>
            <p className="mt-2 font-semibold">You&apos;re all caught up</p>
            <p className="text-sm text-slate-400">We&apos;ll tell you here when a booking is confirmed or changes.</p>
          </div>
        )}

        {items && items.length > 0 && (
          <ul className="stagger space-y-2">
            {items.map((n) => (
              <li key={n.id}>
                <button type="button" onClick={() => open(n)} className={`card flex w-full items-start gap-3 p-4 text-left transition-colors ${n.read ? "opacity-70" : "border-brand-500/40"}`}>
                  <span aria-hidden="true" className="grid h-10 w-10 shrink-0 place-items-center rounded-full bg-slate-800 text-xl">{ICONS[n.type] || "🔔"}</span>
                  <span className="min-w-0 flex-1">
                    <span className="flex items-baseline justify-between gap-2">
                      <span className="truncate text-sm font-semibold">{n.title}</span>
                      <span className="shrink-0 text-xs text-slate-500">{ago(n.createdAt)}</span>
                    </span>
                    {n.body && <span className="mt-0.5 block text-sm text-slate-400">{n.body}</span>}
                  </span>
                  {!n.read && <span aria-label="Unread" className="mt-2 h-2.5 w-2.5 shrink-0 rounded-full bg-[#0095f6]" />}
                </button>
              </li>
            ))}
          </ul>
        )}

        {more && (
          <div className="text-center">
            <button type="button" className="btn btn-secondary btn-sm" disabled={busy} data-loading={busy} onClick={() => load(page + 1)}>Show older</button>
          </div>
        )}
      </div>
    </ProtectedPage>
  );
}
