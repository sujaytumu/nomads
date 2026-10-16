"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import { usePathname, useRouter, useSearchParams } from "next/navigation";

import { api } from "@/lib/api";
import type { TripLike } from "@/lib/tripStatus";

export type MyTrip = TripLike & { tripRequestId: number; createdAt?: string; plans?: any[] | null };

/**
 * Loads the signed-in user's trips and tracks which one is selected, keeping the
 * selection in the address (?tripId=) so every page can be deep-linked from My Trips.
 * Pages using this must be wrapped in <Suspense>.
 */
export function useTripSelection(filter: (t: MyTrip) => boolean = () => true) {
  const router = useRouter();
  const pathname = usePathname() ?? "/";
  const params = useSearchParams();
  const [all, setAll] = useState<MyTrip[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(false);

  const reload = useCallback(() => {
    setLoading(true);
    setError(false);
    api
      .get("/api/trips/me")
      .then((r) => setAll(r.data || []))
      .catch(() => setError(true))
      .finally(() => setLoading(false));
  }, []);
  useEffect(reload, [reload]);

  const trips = useMemo(
    () =>
      all
        .filter(filter)
        .sort((a, b) => new Date(b.createdAt || 0).getTime() - new Date(a.createdAt || 0).getTime()),
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [all]
  );

  const wanted = params?.get("tripId") ?? "";
  const selected = trips.find((t) => String(t.tripRequestId) === wanted) ?? null;

  const select = (id: string) => router.replace(id ? `${pathname}?tripId=${id}` : pathname);

  // With a single trip, there is nothing to choose.
  useEffect(() => {
    if (!loading && !selected && trips.length === 1) select(String(trips[0].tripRequestId));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [loading, trips.length, selected]);

  return { trips, selected, select, loading, error, reload, hasAny: all.length > 0 };
}
