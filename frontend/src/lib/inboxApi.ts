import { api } from "./api";

export type InboxItem = {
  id: number;
  type: string;
  title: string;
  body?: string | null;
  link?: string | null;
  read: boolean;
  createdAt: string;
};

export type InboxPage = { items: InboxItem[]; unread: number; page: number; size: number; totalItems: number; hasMore: boolean };

export async function fetchInbox(page = 0, size = 20, unreadOnly = false): Promise<InboxPage> {
  const { data } = await api.get("/api/inbox", { params: { page, size, unreadOnly } });
  return data as InboxPage;
}

export async function fetchUnreadCount(): Promise<number> {
  const { data } = await api.get("/api/inbox/unread-count");
  return Number(data?.unread) || 0;
}

export async function markInboxRead(id: number) {
  await api.post(`/api/inbox/${id}/read`);
}

export async function markAllInboxRead() {
  await api.post("/api/inbox/read-all");
}
