import { api } from "./api";

export async function fetchGroup(groupId: number) {
  const { data } = await api.get(`/api/groups/${groupId}`);
  return data as {
    id: number;
    city: string;
    interest: string;
    weekendType: string;
    status: string;
    size: number;
    createdAt: string;
  };
}

export async function fetchGroupMembers(groupId: number) {
  const { data } = await api.get(`/api/groups/${groupId}/members`);
  return data as Array<{
    tripRequestId: number;
    userId: number;
    name: string;
    city: string;
    interestType: string;
    tripStatus: string;
    joinedAt: string;
  }>;
}
