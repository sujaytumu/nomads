import axios from "axios";

export const api = axios.create({
  baseURL: process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080",
  // Render's free tier can take 30-50s to wake from a cold start after being
  // idle. 90s gives real cold starts room to finish, while still failing
  // with a clear, retryable error if something is genuinely stuck, instead
  // of a spinner that never resolves (axios has no timeout by default).
  timeout: 90000,
});

api.interceptors.request.use((config) => {
  if (typeof window !== "undefined") {
    const token = localStorage.getItem("nomad_token");
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
  }
  return config;
});

// Any successful write (book, pay, cancel, edit profile...) makes cached reads stale.
api.interceptors.response.use((res) => {
  const m = (res.config.method || "get").toLowerCase();
  if (m !== "get") {
    // Logging in or registering switches who "me" is, so drop the on-device copy too.
    if ((res.config.url || "").includes("/api/auth/")) clearCache();
    else mem.clear();
  }
  return res;
});

/* ---------- tiny read cache ----------
 * The same few GETs (who am I, my trips, homepage packages) were being fetched by the
 * shell, the page guard and the page itself on every screen. Against a sleeping free
 * backend that meant several slow round-trips before anything showed.
 *  - identical in-flight requests are shared
 *  - results are reused for `ttl` ms
 *  - `persist` keeps a copy on the device so the next visit can paint instantly
 *    (peekCached) while fresh data loads in the background
 */
const PREFIX = "nomad_cache:";
const mem = new Map<string, { at: number; data: unknown }>();
const inflight = new Map<string, Promise<unknown>>();

export function peekCached<T = any>(url: string): T | undefined {
  const hit = mem.get(url);
  if (hit) return hit.data as T;
  if (typeof window === "undefined") return undefined;
  try {
    const raw = localStorage.getItem(PREFIX + url);
    return raw ? (JSON.parse(raw) as T) : undefined;
  } catch {
    return undefined;
  }
}

export function getCached<T = any>(url: string, opts: { ttl?: number; persist?: boolean } = {}): Promise<T> {
  const ttl = opts.ttl ?? 30000;
  const hit = mem.get(url);
  if (hit && Date.now() - hit.at < ttl) return Promise.resolve(hit.data as T);
  const pending = inflight.get(url);
  if (pending) return pending as Promise<T>;
  const p = api
    .get(url)
    .then((r) => {
      mem.set(url, { at: Date.now(), data: r.data });
      if (opts.persist && typeof window !== "undefined") {
        try {
          localStorage.setItem(PREFIX + url, JSON.stringify(r.data));
        } catch {
          /* storage full or blocked: caching is optional */
        }
      }
      return r.data as T;
    })
    .finally(() => inflight.delete(url));
  inflight.set(url, p);
  return p;
}

export function clearCache() {
  mem.clear();
  inflight.clear();
  if (typeof window === "undefined") return;
  try {
    Object.keys(localStorage)
      .filter((k) => k.startsWith(PREFIX))
      .forEach((k) => localStorage.removeItem(k));
    // The offline copy of your trips goes too, so nothing is left behind after logging out.
    localStorage.removeItem("nomad_offline_trips");
  } catch {
    /* ignore */
  }
}
