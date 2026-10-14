"use client";

import { useEffect, useState } from "react";

import { addExpense, fetchExpenses, type ExpenseSummary } from "@/lib/expenseApi";

const inr = (n: number) =>
  new Intl.NumberFormat("en-IN", { style: "currency", currency: "INR" }).format(n);

export default function ExpenseSplitter({ groupId }: { groupId: number }) {
  const [summary, setSummary] = useState<ExpenseSummary | null>(null);
  const [description, setDescription] = useState("");
  const [amount, setAmount] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    fetchExpenses(groupId)
      .then(setSummary)
      .catch(() => setError("Couldn't load expenses"));
  }, [groupId]);

  const submit = async () => {
    const value = Number(amount);
    if (!description.trim() || !Number.isFinite(value) || value <= 0) {
      setError("Enter a description and a positive amount");
      return;
    }
    setSaving(true);
    setError(null);
    try {
      setSummary(await addExpense(groupId, description.trim(), value));
      setDescription("");
      setAmount("");
    } catch {
      setError("Couldn't add expense");
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="card p-6 space-y-4">
      <div className="flex items-center justify-between">
        <h3 className="text-lg font-semibold">Split expenses</h3>
        {summary && <span className="text-sm text-slate-400">Total {inr(summary.total)}</span>}
      </div>

      <div className="flex flex-wrap gap-3">
        <input
          value={description}
          onChange={(e) => setDescription(e.target.value)}
          placeholder="What for? (cab, dinner...)"
          maxLength={200}
          className="border rounded-xl px-4 py-2 flex-1 min-w-[10rem]"
        />
        <input
          value={amount}
          onChange={(e) => setAmount(e.target.value)}
          placeholder="Amount (₹)"
          inputMode="decimal"
          className="border rounded-xl px-4 py-2 w-32"
        />
        <button className="btn-primary" onClick={submit} disabled={saving}>
          {saving ? "Adding..." : "I paid"}
        </button>
      </div>
      {error && <p className="text-sm text-red-400">{error}</p>}

      {summary && summary.settlements.length > 0 && (
        <div className="space-y-2">
          <p className="text-sm font-semibold">Settle up</p>
          {summary.settlements.map((s, i) => (
            <p key={i} className="text-sm text-slate-300">
              {s.fromName} pays {s.toName} <span className="font-semibold">{inr(s.amount)}</span>
            </p>
          ))}
        </div>
      )}

      {summary && summary.balances.length > 0 && (
        <div className="space-y-1">
          <p className="text-sm font-semibold">Balances</p>
          {summary.balances.map((b) => (
            <p key={b.userId} className="text-sm text-slate-300">
              {b.name}: {b.net >= 0 ? `gets back ${inr(b.net)}` : `owes ${inr(-b.net)}`}
            </p>
          ))}
        </div>
      )}

      {summary && summary.expenses.length > 0 && (
        <div className="space-y-2">
          <p className="text-sm font-semibold">History</p>
          {summary.expenses.map((e) => (
            <div key={e.id} className="flex items-center justify-between border rounded-xl p-3 text-sm">
              <span>
                {e.description} <span className="text-slate-400">· {e.paidByName}</span>
              </span>
              <span>{inr(e.amount)}</span>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
