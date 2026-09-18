"use client";

import { useEffect, useRef, useState } from "react";

import { api } from "@/lib/api";

type ChatMessage = {
  id: number;
  groupId: number;
  senderId: number;
  senderName: string;
  content: string;
  createdAt: string;
};

function wsBaseUrl(): string {
  const httpBase = process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080";
  return httpBase.replace(/^http/, "ws");
}

export default function GroupChat({ groupId }: { groupId: number }) {
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [draft, setDraft] = useState("");
  const [connected, setConnected] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const socketRef = useRef<WebSocket | null>(null);
  const bottomRef = useRef<HTMLDivElement | null>(null);

  useEffect(() => {
    // Load history first so the room isn't empty on join.
    api.get(`/api/groups/${groupId}/messages`).then((res) => setMessages(res.data)).catch(() => {
      setError("Couldn't load chat history");
    });

    const token = typeof window !== "undefined" ? localStorage.getItem("nomad_token") : null;
    if (!token) {
      setError("Not logged in");
      return;
    }

    const socket = new WebSocket(`${wsBaseUrl()}/ws/chat/${groupId}?token=${encodeURIComponent(token)}`);
    socketRef.current = socket;

    socket.onopen = () => setConnected(true);
    socket.onclose = () => setConnected(false);
    socket.onerror = () => setError("Chat connection failed - the server may still be waking up, try reopening this page in a bit");
    socket.onmessage = (event) => {
      try {
        const msg: ChatMessage = JSON.parse(event.data);
        setMessages((prev) => [...prev, msg]);
      } catch {
        // ignore malformed frames
      }
    };

    return () => {
      socket.close();
    };
  }, [groupId]);

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [messages]);

  const send = () => {
    const text = draft.trim();
    if (!text || !socketRef.current || socketRef.current.readyState !== WebSocket.OPEN) return;
    socketRef.current.send(JSON.stringify({ content: text }));
    setDraft("");
  };

  return (
    <div className="card p-4 space-y-3">
      <div className="flex items-center justify-between">
        <p className="font-semibold">Group Chat</p>
        <span className={`text-xs ${connected ? "text-green-600" : "text-slate-400"}`}>
          {connected ? "● Live" : "○ Connecting…"}
        </span>
      </div>
      {error && <p className="text-sm text-red-600">{error}</p>}
      <div className="h-64 overflow-y-auto border rounded-xl p-3 space-y-2 bg-gray-50">
        {messages.length === 0 && <p className="text-sm text-slate-400">No messages yet - say hi to your group.</p>}
        {messages.map((m) => (
          <div key={m.id} className="text-sm">
            <span className="font-semibold">{m.senderName}: </span>
            <span>{m.content}</span>
          </div>
        ))}
        <div ref={bottomRef} />
      </div>
      <div className="flex gap-2">
        <input
          value={draft}
          onChange={(e) => setDraft(e.target.value)}
          onKeyDown={(e) => e.key === "Enter" && send()}
          placeholder="Message your group…"
          className="flex-1 border rounded-xl px-3 py-2"
        />
        <button className="btn-primary" onClick={send} disabled={!connected}>Send</button>
      </div>
    </div>
  );
}
