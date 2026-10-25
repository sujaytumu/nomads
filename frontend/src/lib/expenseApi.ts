import { api } from "./api";

export type ExpenseSummary = {
  total: number;
  expenses: Array<{
    id: number;
    paidById: number;
    paidByName: string;
    description: string;
    amount: number;
    createdAt: string;
  }>;
  balances: Array<{ userId: number; name: string; net: number }>;
  settlements: Array<{
    fromUserId: number;
    fromName: string;
    toUserId: number;
    toName: string;
    toUpiId?: string | null;
    amount: number;
  }>;
  /** Pay-backs members have recorded. */
  payments: Array<{
    id: number;
    fromUserId: number;
    fromName: string;
    toUserId: number;
    toName: string;
    amount: number;
    createdAt: string;
  }>;
};

export async function fetchExpenses(groupId: number) {
  const { data } = await api.get(`/api/groups/${groupId}/expenses`);
  return data as ExpenseSummary;
}

export async function addExpense(groupId: number, description: string, amount: number) {
  const { data } = await api.post(`/api/groups/${groupId}/expenses`, { description, amount });
  return data as ExpenseSummary;
}

/** "I paid you back": records a pay-back (the money itself moves outside NOMAD). */
export async function recordSettlement(groupId: number, toUserId: number, amount: number) {
  const { data } = await api.post(`/api/groups/${groupId}/expenses/settlements`, { toUserId, amount });
  return data as ExpenseSummary;
}

export async function undoSettlement(groupId: number, settlementId: number) {
  const { data } = await api.delete(`/api/groups/${groupId}/expenses/settlements/${settlementId}`);
  return data as ExpenseSummary;
}
