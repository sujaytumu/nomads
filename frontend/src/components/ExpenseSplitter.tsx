"use client";

import { useEffect, useState } from "react";

import Link from "next/link";
import { api } from "@/lib/api";
import { useAuth } from "@/lib/auth-context";
import { addExpense, fetchExpenses, recordSettlement, undoSettlement, type ExpenseSummary } from "@/lib/expenseApi";
import { errorText, toast } from "@/lib/toast";

const inr = (n: number) =>
  new Intl.NumberFormat("en-IN", { style: "currency", currency: "INR" }).format(n);

export default function ExpenseSplitter({ groupId }: { groupId: number }) {
  const [summary, setSummary] = useState<ExpenseSummary | null>(null);
  const [description, setDescription] = useState("");
  const [amount, setAmount] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);
  const { user } = useAuth();
  const me = user?.id;
  const [myUpi, setMyUpi] = useState<string | null | undefined>(undefined);
  const [busyId, setBusyId] = useState<string | null>(null);

  // Read fresh (not from the shared cache) so a UPI ID you just saved shows up here.
  useEffect(() => {
    api.get("/api/auth/me").then((r) => setMyUpi(r.data?.upiId ?? null)).catch(() => undefined);
  }, []);

  const upiLink = (to: string, name: string, amount: number) =>
    `upi://pay?pa=${encodeURIComponent(to)}&pn=${encodeURIComponent(name)}&am=${amount.toFixed(2)}&cu=INR&tn=${encodeURIComponent("NOMAD trip split")}`;

  const markPaid = async (toUserId: number, toName: string, amount: number) => {
    if (!confirm(`Mark ${inr(amount)} as paid to ${toName}? Only do this after the money has been sent.`)) return;
    setBusyId(`${toUserId}`);
    try {
      setSummary(await recordSettlement(groupId, toUserId, amount));
      toast(`Recorded: you paid ${toName} ${inr(amount)}`, "success");
    } catch (e) {
      toast(errorText(e, "Couldn't record that payment."), "error");
    } finally {
      setBusyId(null);
    }
  };

  const undo = async (id: number) => {
    if (!confirm("Remove this payment? The amount will be owed again.")) return;
    try {
      setSummary(await undoSettlement(groupId, id));
    } catch (e) {
      toast(errorText(e, "Couldn't remove that payment."), "error");
    }
  };

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
          {summary.settlements.map((s) => {
            const iPay = me != null && s.fromUserId === me;
            const owedToMe = me != null && s.toUserId === me;
            return (
              <div key={`${s.fromUserId}-${s.toUserId}`} className="space-y-2 rounded-xl border border-slate-800 bg-slate-900/60 p-3">
                <p className="text-sm text-slate-300">
                  {iPay ? "You" : s.fromName} {iPay ? "pay" : "pays"} {owedToMe ? "you" : s.toName} <span className="font-semibold text-slate-100">{inr(s.amount)}</span>
                </p>
                {iPay && (
                  <div className="flex flex-wrap items-center gap-2">
                    {s.toUpiId ? (
                      <>
                        <a href={upiLink(s.toUpiId, s.toName, s.amount)} className="btn btn-primary btn-sm">Pay {inr(s.amount)} via UPI</a>
                        <button type="button" className="btn btn-secondary btn-sm" onClick={() => { navigator.clipboard?.writeText(s.toUpiId || ""); toast("UPI ID copied", "success"); }}>Copy UPI ID</button>
                      </>
                    ) : (
                      <span className="text-xs text-slate-400">{s.toName} hasn&apos;t added a UPI ID yet. Pay them any way you like, then mark it paid.</span>
                    )}
                    <button type="button" className="btn btn-secondary btn-sm" disabled={busyId === `${s.toUserId}`} data-loading={busyId === `${s.toUserId}`} onClick={() => markPaid(s.toUserId, s.toName, s.amount)}>I&apos;ve paid</button>
                  </div>
                )}
                {owedToMe && myUpi === null && (
                  <p className="text-xs text-slate-400">Add your UPI ID in <Link href="/profile" className="font-semibold text-brand-300 underline">Profile</Link> so {s.fromName} can pay you in one tap.</p>
                )}
              </div>
            );
          })}
        </div>
      )}

      {summary && summary.settlements.length === 0 && summary.expenses.length > 0 && (
        <p className="rounded-xl border border-emerald-400/25 bg-emerald-500/5 p-3 text-sm text-emerald-300">All settled. Nobody owes anything.</p>
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
    
      {summary && summary.payments && summary.payments.length > 0 && (
        <div className="space-y-2">
          <p className="text-sm font-semibold">Payments made</p>
          {summary.payments.map((p) => {
            const mine = me != null && (p.fromUserId === me || p.toUserId === me);
            return (
              <div key={p.id} className="flex items-center justify-between gap-3 rounded-xl border border-slate-800 p-3 text-sm">
                <span className="min-w-0">
                  {p.fromName} paid {p.toName} <span className="text-slate-400">· {new Date(p.createdAt).toLocaleDateString("en-IN", { day: "numeric", month: "short" })}</span>
                </span>
                <span className="flex shrink-0 items-center gap-2">
                  <span className="font-semibold">{inr(p.amount)}</span>
                  {mine && <button type="button" aria-label="Remove this payment" className="btn btn-ghost btn-sm" onClick={() => undo(p.id)}>Undo</button>}
                </span>
              </div>
            );
          })}
        </div>
      )}
</div>
  );
}
