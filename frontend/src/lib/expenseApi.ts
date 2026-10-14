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
    amount: number;
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
