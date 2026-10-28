import { api } from "./api";

// Notifications from the server that arrive even when NOMAD is closed (Web Push).

export const pushSupported = () =>
  typeof window !== "undefined" && "serviceWorker" in navigator && "PushManager" in window && "Notification" in window;

function keyBytes(b64url: string): Uint8Array {
  const pad = "=".repeat((4 - (b64url.length % 4)) % 4);
  const raw = atob((b64url + pad).replace(/-/g, "+").replace(/_/g, "/"));
  const out = new Uint8Array(raw.length);
  for (let i = 0; i < raw.length; i++) out[i] = raw.charCodeAt(i);
  return out;
}

function b64url(buf: ArrayBuffer | null): string {
  if (!buf) return "";
  const bytes = new Uint8Array(buf);
  let s = "";
  for (let i = 0; i < bytes.length; i++) s += String.fromCharCode(bytes[i]);
  return btoa(s).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "");
}

async function registration() {
  if (!pushSupported()) return null;
  return (await navigator.serviceWorker.getRegistration()) ?? (await navigator.serviceWorker.ready);
}

/** True if this device is signed up for push. */
export async function pushActive(): Promise<boolean> {
  try {
    const reg = await registration();
    return !!(reg && (await reg.pushManager.getSubscription()));
  } catch {
    return false;
  }
}

/** Signs this device up. Returns false if push isn't available (unsupported, or the server has no keys yet). */
export async function subscribePush(): Promise<boolean> {
  try {
    const reg = await registration();
    if (!reg) return false;
    const { data } = await api.get("/api/push/key");
    if (!data?.enabled || !data?.key) return false;
    let sub = await reg.pushManager.getSubscription();
    if (!sub) {
      sub = await reg.pushManager.subscribe({
        userVisibleOnly: true,
        applicationServerKey: keyBytes(data.key) as unknown as BufferSource,
      });
    }
    await api.post("/api/push/subscribe", {
      endpoint: sub.endpoint,
      p256dh: b64url(sub.getKey("p256dh")),
      auth: b64url(sub.getKey("auth")),
    });
    return true;
  } catch {
    return false;
  }
}

export async function unsubscribePush(): Promise<void> {
  try {
    const reg = await registration();
    const sub = reg ? await reg.pushManager.getSubscription() : null;
    if (!sub) return;
    await api.post("/api/push/unsubscribe", { endpoint: sub.endpoint }).catch(() => undefined);
    await sub.unsubscribe();
  } catch {
    /* ignore */
  }
}
