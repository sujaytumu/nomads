"use client";

import { useCallback, useEffect, useState } from "react";

const CURRENT = process.env.NEXT_PUBLIC_BUILD_ID || "";

export type AppStatus = {
  /** A newer version than the one on screen is waiting. */
  available: boolean;
  /** A check is running right now. */
  checking: boolean;
  /** When the last successful check finished (null until one has). */
  checkedAt: Date | null;
  /** Short id of the version on screen (for support), or "" in development. */
  version: string;
  /** Check now. Resolves true when a newer version is waiting. */
  check: () => Promise<boolean>;
};

/** Watches the server for a newer build: on load, every minute, when you return, and when you come back online. */
export function useAppStatus(): AppStatus {
  const [available, setAvailable] = useState(false);
  const [checking, setChecking] = useState(false);
  const [checkedAt, setCheckedAt] = useState<Date | null>(null);

  const check = useCallback(async () => {
    if (!CURRENT) return false;
    setChecking(true);
    try {
      const res = await fetch(`/build-id?t=${Date.now()}`, { cache: "no-store" });
      if (!res.ok) return false;
      const data = await res.json();
      const newer = !!data?.id && data.id !== "dev" && data.id !== CURRENT;
      if (newer) setAvailable(true);
      setCheckedAt(new Date());
      return newer;
    } catch {
      /* offline or server waking up: try again next time */
      return false;
    } finally {
      setChecking(false);
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

  return { available, checking, checkedAt, version: CURRENT.slice(0, 7), check };
}

/** True once the server is running a newer build than the one this screen was loaded from. */
export function useAppUpdate(): boolean {
  return useAppStatus().available;
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
