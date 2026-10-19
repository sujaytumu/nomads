"use client";

import { useEffect, useState } from "react";

// The Android app (APK) asks GitHub for the newest release. If it is newer than the installed
// build, a popup offers the update. Inside a normal browser this does nothing.
const REPO = "sujaytumu/nomads";
const SNOOZE_KEY = "nomad_update_snooze";
const SIX_HOURS = 6 * 60 * 60 * 1000;

type Info = { latest: number; url: string; label: string };

export default function UpdatePrompt() {
  const [info, setInfo] = useState<Info | null>(null);
  const [started, setStarted] = useState(false);

  useEffect(() => {
    const native = (window as any).NomadNative;
    if (!native || typeof native.getVersionCode !== "function") return; // not the Android app
    let stopped = false;

    const check = async () => {
      try {
        const current = Number(native.getVersionCode());
        const variant = String(native.getVariant?.() || "mobile");
        const res = await fetch(`https://api.github.com/repos/${REPO}/releases/latest`, {
          headers: { Accept: "application/vnd.github+json" },
        });
        if (!res.ok) return;
        const rel = await res.json();
        const m = /^apk-(\d+)$/.exec(String(rel.tag_name || ""));
        if (!m) return;
        const latest = 100 + Number(m[1]); // same rule the build uses for versionCode
        const asset = (rel.assets || []).find((a: any) => a.name === `nomads-${variant}.apk`);
        if (!asset || !(latest > current)) return;
        try {
          const s = JSON.parse(localStorage.getItem(SNOOZE_KEY) || "null");
          if (s && s.v === latest && Date.now() - s.t < SIX_HOURS) return; // "Later" was tapped recently
        } catch { /* ignore */ }
        if (!stopped) setInfo({ latest, url: asset.browser_download_url, label: `1.${m[1]}` });
      } catch {
        /* offline or rate limited: try again next time */
      }
    };

    check();
    const onVisible = () => document.visibilityState === "visible" && check();
    document.addEventListener("visibilitychange", onVisible);
    return () => {
      stopped = true;
      document.removeEventListener("visibilitychange", onVisible);
    };
  }, []);

  if (!info) return null;

  const later = () => {
    try { localStorage.setItem(SNOOZE_KEY, JSON.stringify({ v: info.latest, t: Date.now() })); } catch { /* ignore */ }
    setInfo(null);
  };

  const update = () => {
    try { (window as any).NomadNative.downloadUpdate(info.url); } catch { /* ignore */ }
    setStarted(true);
    setTimeout(() => setInfo(null), 2500);
  };

  return (
    <div className="fixed inset-0 z-[100] flex items-end justify-center bg-black/60 p-4 sm:items-center" role="dialog" aria-modal="true" aria-labelledby="update-title">
      <div className="card w-full max-w-sm space-y-3 p-5">
        <h2 id="update-title" className="font-display text-lg font-semibold">Update available</h2>
        <p className="text-sm text-slate-300">
          NOMAD {info.label} is ready. It installs over your current app. Your account and trips stay as they are.
        </p>
        {started ? (
          <p role="status" className="text-sm text-green-300">Downloading… the installer opens when it finishes.</p>
        ) : (
          <div className="grid grid-cols-2 gap-2">
            <button type="button" onClick={later} className="btn btn-secondary">Later</button>
            <button type="button" onClick={update} className="btn btn-primary">Update now</button>
          </div>
        )}
      </div>
    </div>
  );
}
