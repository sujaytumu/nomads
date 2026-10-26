import { api } from "./api";

export type Poll = {
  id: number;
  question: string;
  closed: boolean;
  createdByName: string;
  /** You created this poll, so you can close it. */
  mine: boolean;
  createdAt: string;
  totalVotes: number;
  myOptionId: number | null;
  options: Array<{ id: number; text: string; votes: number }>;
};

export async function fetchPolls(groupId: number) {
  const { data } = await api.get(`/api/groups/${groupId}/polls`);
  return data as Poll[];
}

export async function createPoll(groupId: number, question: string, options: string[]) {
  const { data } = await api.post(`/api/groups/${groupId}/polls`, { question, options });
  return data as Poll;
}

export async function votePoll(groupId: number, pollId: number, optionId: number) {
  const { data } = await api.post(`/api/groups/${groupId}/polls/${pollId}/vote`, { optionId });
  return data as Poll;
}

export async function closePoll(groupId: number, pollId: number) {
  const { data } = await api.post(`/api/groups/${groupId}/polls/${pollId}/close`);
  return data as Poll;
}
