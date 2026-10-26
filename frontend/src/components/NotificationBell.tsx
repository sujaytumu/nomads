"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { usePathname } from "next/navigation";
import { fetchUnreadCount } from "@/lib/inboxApi";

/** Bell with an unread badge. Checks quietly every minute, when you come back to the tab, and when you change page. */
export default function NotificationBell({ className = "" }: { className?: string }) {
  const [unread, setUnread] = useState(0);
  const path = usePathname();

  useEffect(() => {
    let live = true;
    const load = () => fetchUnreadCount().then((n) => live && setUnread(n)).catch(() => undefined);
    load();
    const t = setInterval(load, 60000);
    const onVisible = () => document.visibilityState === "visible" && load();
    document.addEventListener("visibilitychange", onVisible);
    return () => {
      live = false;
      clearInterval(t);
      document.removeEventListener("visibilitychange", onVisible);
    };
  }, [path]);

  return (
    <Link href="/notifications" aria-label={unread > 0 ? `Notifications, ${unread} unread` : "Notifications"} className={`relative grid h-9 w-9 place-items-center rounded-full text-slate-200 transition-colors hover:bg-slate-800 ${className}`}>
      <svg aria-hidden="true" viewBox="0 0 24 24" className="h-[22px] w-[22px]" fill="none" stroke="currentColor" strokeWidth="1.9" strokeLinecap="round" strokeLinejoin="round">
        <path d="M6 9a6 6 0 1 1 12 0c0 5 2 6 2 6H4s2-1 2-6zM10 19a2 2 0 0 0 4 0" />
      </svg>
      {unread > 0 && (
        <span className="absolute -right-0.5 -top-0.5 grid h-[18px] min-w-[18px] place-items-center rounded-full bg-[#ff3040] px-1 text-[10px] font-bold leading-none text-white ring-2 ring-slate-950">
          {unread > 9 ? "9+" : unread}
        </span>
      )}
    </Link>
  );
}
