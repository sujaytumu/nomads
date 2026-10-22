"use client";

import { useState } from "react";
import { applyUpdate } from "@/lib/useAppUpdate";

/** The one "Update" button. It only exists while a newer version is waiting. */
export default function UpdateButton({ className = "" }: { className?: string }) {
  const [busy, setBusy] = useState(false);
  return (
    <button
      type="button"
      onClick={() => {
        setBusy(true);
        applyUpdate();
      }}
      data-loading={busy}
      aria-label="A new version of NOMAD is ready. Update now"
      className={`btn btn-primary btn-pulse animate-pop-in gap-1.5 px-4 py-1.5 text-xs ${className}`}
    >
      {!busy && (
        <svg aria-hidden="true" viewBox="0 0 24 24" className="h-3.5 w-3.5" fill="none" stroke="currentColor" strokeWidth="2.4" strokeLinecap="round" strokeLinejoin="round">
          <path d="M21 12a9 9 0 1 1-3-6.7M21 4v5h-5" />
        </svg>
      )}
      {busy ? "Updating" : "Update"}
    </button>
  );
}
