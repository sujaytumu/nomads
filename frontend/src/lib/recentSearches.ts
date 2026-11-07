const KEY = "nomad_recent_searches";
const MAX = 6;

export function readSearches(): string[] {
  try {
    const raw = JSON.parse(localStorage.getItem(KEY) || "[]");
    return Array.isArray(raw) ? raw.filter((s) => typeof s === "string").slice(0, MAX) : [];
  } catch {
    return [];
  }
}

/** Newest first, case-insensitive no repeats, at most 6. Device only. */
export function rememberSearch(text: string) {
  const t = text.trim();
  if (t.length < 2) return;
  try {
    const next = [t, ...readSearches().filter((s) => s.toLowerCase() !== t.toLowerCase())].slice(0, MAX);
    localStorage.setItem(KEY, JSON.stringify(next));
  } catch {
    /* private mode */
  }
}

export function clearSearches() {
  try {
    localStorage.removeItem(KEY);
  } catch {
    /* ignore */
  }
}
