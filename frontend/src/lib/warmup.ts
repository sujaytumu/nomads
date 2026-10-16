import { api } from "./api";

let started = false;

/**
 * The free backend goes to sleep when idle and takes a while to boot. Pinging it
 * the moment the app opens lets that boot happen while the person is still typing
 * their email, instead of after they press "Log in".
 */
export function warmUpBackend() {
  if (started || typeof window === "undefined") return;
  started = true;
  const base = api.defaults.baseURL;
  if (!base) return;
  fetch(`${base}/actuator/health`, { mode: "no-cors", cache: "no-store", keepalive: true }).catch(() => undefined);
}

/** Retry only when the server is waking up (no response, or a gateway error), never on real answers like 401. */
export async function withWakeRetry<T>(call: () => Promise<T>, onWaiting?: (attempt: number) => void): Promise<T> {
  let lastError: any;
  for (let attempt = 0; attempt < 4; attempt++) {
    try {
      return await call();
    } catch (err: any) {
      lastError = err;
      const status = err?.response?.status;
      const waking = !err?.response || status === 502 || status === 503 || status === 504;
      if (!waking) throw err;
      onWaiting?.(attempt + 1);
      await new Promise((r) => setTimeout(r, 3000));
    }
  }
  throw lastError;
}
