import { api } from "./api";

export type ChatTurn = { role: "user" | "assistant"; content: string };
export type AssistantAnswer = { answer: string; mode: "ai" | "basic" | "limit" };

export async function askTripAssistant(tripId: number | string, question: string, history: ChatTurn[]): Promise<AssistantAnswer> {
  const { data } = await api.post(`/api/trips/${tripId}/assistant`, { question, history: history.slice(-6) });
  return data;
}
