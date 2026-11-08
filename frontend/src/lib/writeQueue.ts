import { api } from "./api";
import { toast } from "./toast";

// Writes made while the phone has no signal are kept on the device and sent when it is back online.
// Only "add" requests are queued, and only when the browser says it is offline, so a request that may have
// reached the server is never sent a second time.

export type QueuedWrite = { id: string; url: string; data: unknown; label: string; at: number };

const KEY = "nomad_write_queue";
const MAX = 50;
export const QUEUE_EVENT = "nomad:queue";
export const SYNCED_EVENT = "nomad:synced";

function read(): QueuedWrite[] {
  try {
    const raw = localStorage.getItem(KEY);
    const list = raw ? JSON.parse(raw) : [];
    return Array.isArray(list) ? list : [];
  } catch {
    return [];
  }
}

function write(list: QueuedWrite[]) {
  try {
    localStorage.setItem(KEY, JSON.stringify(list));
  } catch {
    /* storage full or blocked */
  }
  window.dispatchEvent(new CustomEvent(QUEUE_EVENT, { detail: list.length }));
}

export const queuedCount = () => (typeof window === "undefined" ? 0 : read().length);
export const isOffline = () => typeof navigator !== "undefined" && navigator.onLine === false;

/** Sends the request now, or keeps it for later if the device is offline. */
export async function postOrQueue<T = any>(url: string, data: unknown, label: string): Promise<{ queued: false; data: T } | { queued: true }> {
  if (isOffline()) {
    const list = read();
    if (list.length >= MAX) throw new Error("Too many changes are waiting to be sent. Connect to the internet first.");
    list.push({ id: `${Date.now()}-${Math.random().toString(36).slice(2, 8)}`, url, data, label, at: Date.now() });
    write(list);
    return { queued: true };
  }
  const res = await api.post(url, data);
  return { queued: false, data: res.data as T };
}

let flushing = false;

/** Sends everything that is waiting, in order. Stops at the first connection problem and tries again later. */
export async function flushQueue(): Promise<void> {
  if (typeof window === "undefined" || flushing || isOffline()) return;
  if (!localStorage.getItem("nomad_token")) return;
  flushing = true;
  let sent = 0;
  let dropped = 0;
  try {
    let list = read();
    while (list.length) {
      const item = list[0];
      try {
        await api.post(item.url, item.data);
        sent++;
      } catch (err: any) {
        if (!err?.response) break; // still no connection: keep it and retry later
        const status = err.response.status;
        if (status === 401 || status === 429 || status >= 500) break; // may work later
        dropped++; // the server refused it for good (for example the trip was removed)
        toast(`Couldn't save "${item.label}": ${err.response.data?.message || "the server refused it"}`, "error");
      }
      list = read().filter((q) => q.id !== item.id);
      write(list);
    }
  } finally {
    flushing = false;
  }
  if (sent > 0) {
    toast(sent === 1 ? "Your offline change was saved" : `${sent} offline changes were saved`, "success");
    window.dispatchEvent(new CustomEvent(SYNCED_EVENT));
  } else if (dropped > 0) {
    window.dispatchEvent(new CustomEvent(SYNCED_EVENT));
  }
}
