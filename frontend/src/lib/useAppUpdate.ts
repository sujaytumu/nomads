"use client";

import { useCallback, useEffect, useState } from "react";

const CURRENT = process.env.NEXT_PUBLIC_BUILD_ID || "";

/** True once the server is running a newer build than the one this screen was loaded from. */
export function useAppUpdate(): boolean {
  const [available, setAvailable] = useState(false);

  const check = useCallback(async () => {
    if (!CURRENT) return;
    try {
      const res = await fetch(`/build-id?t=${Date.now()}`, { cache: "no-store" });
      if (!res.ok) return;
      const data = await res.json();
      if (data?.id && data.id !== "dev" && data.id !== CURRENT) setAvailable(true);
    } catch {
      /* offline or server waking up: try again next time */
    }
  }, []);

  useEffect(() => {
    check();
    const timer = setInterval(check, 60000);
    const onVisible = () => document.visibilityState === "visible" && check();
    document.addEventListener("visibilitychange", onVisible);
    window.addEventListener("online", check);
    return () => {
      clearInterval(timer);
      document.removeEventListener("visibilitychange", onVisible);
      window.removeEventListener("online", check);
    };
  }, [check]);

  return available;
}

/** Loads the newest version: refreshes the service worker, then reloads. */
export async function applyUpdate() {
  try {
    if ("serviceWorker" in navigator) {
      const regs = await navigator.serviceWorker.getRegistrations();
      await Promise.all(regs.map((r) => r.update().catch(() => undefined)));
    }
  } catch {
    /* reload anyway */
  }
  window.location.reload();
}
