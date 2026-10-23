// On-device reminders. They fire while NOMAD is open or installed on the device
// (no server push), and each one is shown only once.

const ON_KEY = "nomad_notify";

export const remindersSupported = () => typeof window !== "undefined" && "Notification" in window;
export const remindersPermission = (): NotificationPermission | "unsupported" => (remindersSupported() ? Notification.permission : "unsupported");

export function remindersOn(): boolean {
  try {
    return remindersSupported() && Notification.permission === "granted" && localStorage.getItem(ON_KEY) === "1";
  } catch {
    return false;
  }
}

export async function enableReminders(): Promise<"on" | "denied" | "unsupported"> {
  if (!remindersSupported()) return "unsupported";
  const p = Notification.permission === "granted" ? "granted" : await Notification.requestPermission();
  if (p !== "granted") return "denied";
  try {
    localStorage.setItem(ON_KEY, "1");
  } catch {
    /* ignore */
  }
  return "on";
}

export function disableReminders() {
  try {
    localStorage.removeItem(ON_KEY);
  } catch {
    /* ignore */
  }
}

/** Shows a notification once per `tag`. Returns true if it was shown. */
export async function notifyOnce(tag: string, title: string, body: string, url = "/") {
  if (!remindersOn()) return false;
  const seenKey = `nomad_seen_${tag}`;
  try {
    if (localStorage.getItem(seenKey)) return false;
    localStorage.setItem(seenKey, "1");
  } catch {
    /* if storage fails, still show once per page load */
  }
  const opts: NotificationOptions = { body, tag, icon: "/icon-192.png", data: { url } };
  try {
    const reg = "serviceWorker" in navigator ? await navigator.serviceWorker.getRegistration() : undefined;
    if (reg) await reg.showNotification(title, opts);
    else new Notification(title, opts);
    return true;
  } catch {
    return false;
  }
}

/** Old "seen" markers are cleared so storage doesn't grow forever. */
export function pruneSeen(keep: Set<string>) {
  try {
    for (let i = localStorage.length - 1; i >= 0; i--) {
      const k = localStorage.key(i);
      if (k && k.startsWith("nomad_seen_") && !keep.has(k.slice(11))) localStorage.removeItem(k);
    }
  } catch {
    /* ignore */
  }
}
