export type RecentPlace = { id: number; name: string; city?: string; category?: string; imageUrl?: string | null };

const KEY = "nomad_recent_places";
const MAX = 8;

export function readRecent(): RecentPlace[] {
  try {
    const raw = JSON.parse(localStorage.getItem(KEY) || "[]");
    return Array.isArray(raw) ? raw.filter((p) => p && typeof p.id === "number" && typeof p.name === "string").slice(0, MAX) : [];
  } catch {
    return [];
  }
}

/** Newest first, no repeats, at most 8. Stored on this device only. */
export function rememberPlace(p: RecentPlace) {
  try {
    const next = [p, ...readRecent().filter((x) => x.id !== p.id)].slice(0, MAX);
    localStorage.setItem(KEY, JSON.stringify(next));
  } catch {
    /* private mode: skip */
  }
}

export function clearRecent() {
  try {
    localStorage.removeItem(KEY);
  } catch {
    /* ignore */
  }
}
