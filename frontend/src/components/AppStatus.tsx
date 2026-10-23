"use client";

import { useEffect, useState } from "react";
import InstallApp from "@/components/InstallApp";
import { applyUpdate, useAppStatus } from "@/lib/useAppUpdate";

/** Profile card: is the app installed, and is it on the latest version. */
export default function AppStatus() {
  const { available, checking, checkedAt, version, check } = useAppStatus();
  const [installed, setInstalled] = useState<boolean | null>(null);
  const [busy, setBusy] = useState(false);
  const [justChecked, setJustChecked] = useState(false);

  useEffect(() => {
    setInstalled(window.matchMedia("(display-mode: standalone)").matches || !!(navigator as any).standalone || /wv\)/i.test(navigator.userAgent));
  }, []);

  const time = checkedAt?.toLocaleTimeString("en-IN", { hour: "2-digit", minute: "2-digit" });

  return (
    <div className="space-y-3">
      <div className={`flex items-center gap-3 rounded-2xl border p-3 ${available ? "border-brand-500/50 bg-brand-600/15" : "border-emerald-400/25 bg-emerald-500/5"}`}>
        <span aria-hidden="true" className={`grid h-9 w-9 shrink-0 place-items-center rounded-full text-base ${available ? "bg-brand-600 text-white" : "bg-emerald-500/20 text-emerald-300"}`}>
          {available ? "↻" : "✓"}
        </span>
        <div className="min-w-0 flex-1">
          <p className="text-sm font-semibold">{available ? "A new version is ready" : "You're on the latest version"}</p>
          <p className="truncate text-xs text-slate-400">
            {checking ? "Checking…" : time ? `Checked at ${time}` : "Not checked yet"}
            {version ? ` · v${version}` : ""}
          </p>
        </div>
        {available ? (
          <button type="button" className="btn btn-primary btn-sm" data-loading={busy} onClick={() => { setBusy(true); applyUpdate(); }}>
            Update
          </button>
        ) : (
          <button
            type="button"
            className="btn btn-secondary btn-sm"
            disabled={checking}
            data-loading={checking}
            onClick={async () => {
              const newer = await check();
              if (!newer) {
                setJustChecked(true);
                setTimeout(() => setJustChecked(false), 2500);
              }
            }}
          >
            {justChecked ? "Up to date ✓" : "Check"}
          </button>
        )}
      </div>

      <div className="flex flex-wrap items-center justify-between gap-2 rounded-2xl border border-slate-800 bg-slate-900/60 p-3">
        <div>
          <p className="text-sm font-semibold">{installed === null ? "App" : installed ? "Installed on this device" : "Not installed"}</p>
          <p className="text-xs text-slate-400">{installed ? "Opens like a normal app." : "Add NOMAD to your home screen."}</p>
        </div>
        {installed === false && <div className="w-40"><InstallApp /></div>}
      </div>
    </div>
  );
}
