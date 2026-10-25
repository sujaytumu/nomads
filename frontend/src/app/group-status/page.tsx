"use client";

import dynamic from "next/dynamic";
import { Suspense, useEffect, useState } from "react";

import ExpenseSplitter from "@/components/ExpenseSplitter";
import GroupPolls from "@/components/GroupPolls";
import PageHeader from "@/components/PageHeader";
import ProtectedPage from "@/components/ProtectedPage";
import TripPicker from "@/components/TripPicker";
import { fetchGroup, fetchGroupMembers } from "@/lib/groupApi";
import { statusMeta } from "@/lib/tripStatus";
import { useTripSelection } from "@/lib/useTripSelection";

const GroupChat = dynamic(() => import("@/components/GroupChat"), { ssr: false });

export default function GroupPage() {
  return (
    <Suspense fallback={null}>
      <GroupInner />
    </Suspense>
  );
}

function GroupInner() {
  // Only trips that were matched into a group have anything to show here.
  const sel = useTripSelection((t: any) => !!t.groupId && t.status !== "CANCELLED");
  const groupId = (sel.selected as any)?.groupId as number | undefined;
  const [group, setGroup] = useState<any>(null);
  const [members, setMembers] = useState<any[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [tab, setTab] = useState<"people" | "chat" | "polls" | "split">("people");

  useEffect(() => {
    setGroup(null);
    setMembers([]);
    setError(null);
    if (!groupId) return;
    Promise.all([fetchGroup(groupId), fetchGroupMembers(groupId)])
      .then(([g, m]) => {
        setGroup(g);
        setMembers(m);
      })
      .catch(() => setError("Couldn't load this group."));
  }, [groupId]);

  return (
    <ProtectedPage>
      <div className="section space-y-6 py-8">
        <PageHeader
          title="Group"
          subtitle="Travelling with others? See who's in your group, chat, and split costs. Groups form automatically when people pick the same city, interest and weekend."
        />
        <div className="card space-y-5 p-6">
          <TripPicker trips={sel.trips} selected={sel.selected} onSelect={sel.select} loading={sel.loading} error={sel.error} onRetry={sel.reload} emptyText="None of your trips is in a group yet. When someone else plans the same city, interest and weekend, you'll be grouped together." />
          {error && <p role="alert" className="text-sm text-red-400">{error}</p>}
        </div>

        {group && (
          <div className="space-y-4">
            <div className="flex flex-wrap items-center gap-2 text-sm text-slate-300">
              <span className="badge">{group.city}</span>
              <span className="badge">{String(group.interest).toLowerCase()}</span>
              <span className="badge">{String(group.weekendType).replace("_", " ").toLowerCase()}</span>
              <span className="text-slate-400">{group.size} {group.size === 1 ? "traveller" : "travellers"}</span>
            </div>
            <div className="flex gap-2">
              {([["people", "People"], ["chat", "Chat"], ["polls", "Polls"], ["split", "Split costs"]] as const).map(([k, label]) => (
                <button key={k} type="button" className={`btn btn-sm ${tab === k ? "btn-primary" : "btn-secondary"}`} onClick={() => setTab(k)}>{label}</button>
              ))}
            </div>

            {tab === "people" && (
              <div className="card space-y-3 p-6">
                {members.length === 0 ? (
                  <p className="text-sm text-slate-400">No members found.</p>
                ) : (
                  members.map((m) => (
                    <div key={m.tripRequestId} className="flex items-center justify-between rounded-xl border border-slate-800 p-3">
                      <div>
                        <p className="font-semibold">{m.name}</p>
                        <p className="text-xs text-slate-400">{m.city} · {String(m.interestType).toLowerCase()}</p>
                      </div>
                      <span className={`rounded-full px-2 py-1 text-xs font-medium ${statusMeta(m.tripStatus).badge}`}>{statusMeta(m.tripStatus).label}</span>
                    </div>
                  ))
                )}
              </div>
            )}
            {tab === "chat" && <GroupChat groupId={group.id} />}
            {tab === "polls" && <GroupPolls groupId={group.id} />}
            {tab === "split" && <ExpenseSplitter groupId={group.id} />}
          </div>
        )}
      </div>
    </ProtectedPage>
  );
}
