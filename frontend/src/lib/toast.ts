export type ToastKind = "success" | "error" | "info";

/** Shows a small message at the bottom of the screen. Safe to call from anywhere. */
export function toast(message: string, kind: ToastKind = "info") {
  if (typeof window === "undefined") return;
  window.dispatchEvent(new CustomEvent("nomad:toast", { detail: { message, kind } }));
}

/** A readable sentence from an API error. */
export function errorText(e: any, fallback = "Something went wrong. Please try again."): string {
  const d = e?.response?.data;
  if (typeof d === "string" && d.trim()) return d.slice(0, 160);
  if (d?.message) return String(d.message).slice(0, 160);
  if (d?.error) return String(d.error).slice(0, 160);
  return e?.message && !/status code/i.test(e.message) ? e.message : fallback;
}
