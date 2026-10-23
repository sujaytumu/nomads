"use client";

import { useEffect, useState } from "react";
import { disableReminders, enableReminders, remindersOn, remindersPermission } from "@/lib/reminders";
import { toast } from "@/lib/toast";

/** Profile row: turn trip reminders on or off. */
export default function ReminderToggle() {
  const [on, setOn] = useState(false);
  const [perm, setPerm] = useState<ReturnType<typeof remindersPermission>>("default");

  useEffect(() => {
    setOn(remindersOn());
    setPerm(remindersPermission());
  }, []);

  const flip = async () => {
    if (on) {
      disableReminders();
      setOn(false);
      toast("Reminders off");
      return;
    }
    const r = await enableReminders();
    setPerm(remindersPermission());
    if (r === "on") {
      setOn(true);
      toast("Reminders on. We'll nudge you about payments and trip days.", "success");
    } else if (r === "denied") {
      toast("Notifications are blocked. Allow them in your browser or phone settings.", "error");
    }
  };

  const note =
    perm === "unsupported"
      ? "Not available in this app or browser."
      : perm === "denied"
        ? "Blocked in settings. Allow notifications to turn this on."
        : on
          ? "Payment, trip-tomorrow, trip-day and next-stop reminders while the app is open."
          : "Get nudges for payments, the day before, and each stop.";

  return (
    <div className="flex items-center justify-between gap-3 rounded-2xl border border-slate-800 bg-slate-900/60 p-3">
      <div className="min-w-0">
        <p className="text-sm font-semibold">Trip reminders</p>
        <p className="text-xs text-slate-400">{note}</p>
      </div>
      <button
        type="button"
        role="switch"
        aria-checked={on}
        aria-label="Trip reminders"
        disabled={perm === "unsupported"}
        onClick={flip}
        className={`relative h-7 w-12 shrink-0 rounded-full transition-colors disabled:opacity-40 ${on ? "bg-brand-600" : "bg-slate-700"}`}
      >
        <span className={`absolute left-0.5 top-0.5 h-6 w-6 rounded-full bg-white shadow transition-transform ${on ? "translate-x-5" : ""}`} />
      </button>
    </div>
  );
}
