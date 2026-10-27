import { api } from "./api";

export type JournalNote = { id: number; dayNumber: number; mood?: string | null; text: string; createdAt: string; updatedAt: string };

export async function fetchNotes(tripId: number | string): Promise<JournalNote[]> {
  const { data } = await api.get(`/api/trips/${tripId}/notes`);
  return data as JournalNote[];
}

export async function addNote(tripId: number | string, body: { text: string; dayNumber: number; mood?: string }): Promise<JournalNote> {
  const { data } = await api.post(`/api/trips/${tripId}/notes`, body);
  return data as JournalNote;
}

export async function updateNote(tripId: number | string, id: number, body: { text: string; dayNumber: number; mood?: string }): Promise<JournalNote> {
  const { data } = await api.put(`/api/trips/${tripId}/notes/${id}`, body);
  return data as JournalNote;
}

export async function deleteNote(tripId: number | string, id: number) {
  await api.delete(`/api/trips/${tripId}/notes/${id}`);
}

/** Downloads the journal as a .txt diary. */
export async function downloadJournal(tripId: number | string, city?: string | null) {
  const res = await api.get(`/api/trips/${tripId}/notes/export`, { responseType: "blob" });
  const url = URL.createObjectURL(res.data as Blob);
  const a = document.createElement("a");
  a.href = url;
  a.download = `nomad-${(city || "trip").toLowerCase().replace(/[^a-z0-9]+/g, "-")}-journal.txt`;
  document.body.appendChild(a);
  a.click();
  a.remove();
  setTimeout(() => URL.revokeObjectURL(url), 2000);
}
