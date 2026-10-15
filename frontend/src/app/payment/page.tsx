"use client";

import Link from "next/link";
import { Suspense, useEffect, useMemo, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";

import ProtectedPage from "@/components/ProtectedPage";
import TripSteps from "@/components/TripSteps";
import { api } from "@/lib/api";
import { createPayment, verifyPayment } from "@/lib/paymentApi";
import { loadRazorpayScript, RazorpayHandlerResponse } from "@/lib/razorpay";
import { fetchTrip } from "@/lib/tripApi";
import {
  formatCost,
  isPayable,
  statusMeta,
  tripKind,
  tripTitle,
  type TripLike,
} from "@/lib/tripStatus";

export default function PaymentPage() {
  return (
    <Suspense fallback={null}>
      <PaymentPageInner />
    </Suspense>
  );
}

function PaymentPageInner() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const fromQuery = searchParams?.get("tripRequestId") ?? "";

  const [trips, setTrips] = useState<TripLike[]>([]);
  const [loaded, setLoaded] = useState(false);
  const [loadError, setLoadError] = useState(false);
  const [tripId, setTripId] = useState(fromQuery);
  const [paying, setPaying] = useState(false);
  const [paid, setPaid] = useState(false);
  const [orderId, setOrderId] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const keyId = (process as any)?.env?.NEXT_PUBLIC_RAZORPAY_KEY_ID || "";

  // Load the user's trips. If we arrived with ?tripRequestId=X (from the
  // planner, a package enrollment or My Trips) make sure that exact trip is
  // present with its real status and cost, then select it.
  useEffect(() => {
    let active = true;
    (async () => {
      let list: TripLike[] = [];
      let failed = false;
      try {
        const { data } = await api.get("/api/trips/me");
        list = data || [];
      } catch {
        failed = true;
      }
      if (fromQuery && !list.some((t) => String(t.tripRequestId) === fromQuery)) {
        try {
          list = [await fetchTrip(Number(fromQuery)), ...list];
          failed = false;
        } catch {
          // Not found or not ours - the page falls back to the picker below.
        }
      }
      if (!active) return;
      setTrips(list);
      setLoadError(failed && list.length === 0);
      setTripId(fromQuery);
      setLoaded(true);
    })();
    return () => {
      active = false;
    };
  }, [fromQuery]);

  const payable = useMemo(() => trips.filter((t) => isPayable(t.status)), [trips]);
  const selected = useMemo(
    () => trips.find((t) => String(t.tripRequestId) === tripId) ?? null,
    [trips, tripId]
  );
  const selectedPayable = !!selected && isPayable(selected.status);
  const hasCost = selected?.estimatedCost !== null && selected?.estimatedCost !== undefined;

  const markStatus = (id: number, status: string) =>
    setTrips((prev) => prev.map((t) => (Number(t.tripRequestId) === id ? { ...t, status } : t)));

  // Payment is done: the trip is booked, so take the user to My Trips, where
  // it now appears under "Booked" (the banner there confirms which trip).
  const finish = (id: number) => {
    markStatus(id, "CONFIRMED");
    setPaid(true);
    setPaying(false);
    router.replace(`/bookings?paid=${id}`);
  };

  const handlePay = async () => {
    setError(null);
    if (!selected || !selectedPayable) {
      setError("Choose a trip to pay for first.");
      return;
    }
    const id = Number(selected.tripRequestId);
    setPaying(true);

    let data;
    try {
      data = await createPayment({ tripRequestId: id });
    } catch (e: any) {
      setError(e?.response?.data?.message || e?.message || "Couldn't start the payment");
      setPaying(false);
      return;
    }

    setOrderId(data.razorpayOrderId);
    markStatus(id, "PAYMENT_PENDING");

    // Dev-mode fake order (backend NOMAD_DEV_PAYMENTS=true) - no real Razorpay
    // involved, complete the booking directly instead of opening real checkout
    if (data.razorpayOrderId?.startsWith("dev_order_")) {
      try {
        const verified = await verifyPayment({
          razorpayOrderId: data.razorpayOrderId,
          razorpayPaymentId: "dev_payment_" + Date.now(),
          razorpaySignature: "dev_signature",
        });
        if (verified.paymentStatus === "CAPTURED") {
          finish(id);
        } else {
          setError("Payment wasn't confirmed. Please try again.");
          setPaying(false);
        }
      } catch (e: any) {
        setError(e?.response?.data?.message || e?.message || "Payment confirmation failed");
        setPaying(false);
      }
      return;
    }

    const sdkLoaded = await loadRazorpayScript();
    if (!sdkLoaded) {
      setError("The payment window couldn't load. Check your connection and try again.");
      setPaying(false);
      return;
    }
    if (!keyId) {
      setError("Payments aren't configured (missing NEXT_PUBLIC_RAZORPAY_KEY_ID).");
      setPaying(false);
      return;
    }

    const options = {
      key: keyId,
      amount: Number(data.amount) * 100,
      currency: "INR",
      name: "NOMAD",
      description: `${tripTitle(selected)} (#${id})`,
      order_id: data.razorpayOrderId,
      handler: async (response: RazorpayHandlerResponse) => {
        try {
          const verified = await verifyPayment({
            razorpayOrderId: response.razorpay_order_id,
            razorpayPaymentId: response.razorpay_payment_id,
            razorpaySignature: response.razorpay_signature,
          });
          if (verified.paymentStatus === "CAPTURED") {
            finish(id);
          } else {
            setError("Payment wasn't confirmed. If you were charged, it will show up in My Trips shortly.");
            setPaying(false);
          }
        } catch (e: any) {
          setError(e?.response?.data?.message || "We couldn't confirm your payment. Check My Trips before retrying.");
          setPaying(false);
        }
      },
      modal: {
        // Closing the window without paying leaves the trip awaiting payment.
        ondismiss: () => setPaying(false),
      },
      theme: {
        color: "#4f6cff",
      },
    };

    const razorpay = new (window as any).Razorpay(options);
    razorpay.open();
  };

  const meta = statusMeta(selected?.status);

  return (
    <ProtectedPage>
      <div className="section py-12 space-y-6">
        <div className="card p-6 space-y-5">
          <TripSteps current={paid ? 4 : 3} tripId={selected?.tripRequestId ?? (tripId || null)} />
          <div>
            <h2 className="text-2xl font-bold">Payment</h2>
            <p className="text-sm text-slate-300">Pay for a trip to confirm your booking.</p>
          </div>

          {!loaded && <p className="text-sm text-slate-400">Loading your trips…</p>}

          {loaded && loadError && (
            <p className="text-sm text-red-400">
              Couldn't load your trips. Refresh the page, or open <Link href="/bookings" className="underline">My Trips</Link>.
            </p>
          )}

          {/* Success: shown only for the moment before the redirect to My Trips lands */}
          {paid && selected && (
            <div className="rounded-xl border border-green-500/30 bg-green-500/10 p-5 space-y-2" role="status">
              <p className="text-lg font-semibold text-green-300">Payment received — your trip is booked 🎉</p>
              <p className="text-sm text-slate-300">
                Taking you to <Link href="/bookings" className="underline">My Trips</Link>…
              </p>
            </div>
          )}

          {/* Arrived for a trip that can't be paid for (already booked / cancelled) */}
          {loaded && !paid && selected && !selectedPayable && (
            <div className="rounded-xl border border-slate-700 p-5 space-y-3">
              <div className="flex flex-wrap items-center gap-2">
                <p className="font-semibold">
                  {tripTitle(selected)} (#{selected.tripRequestId})
                </p>
                <span className={`px-2 py-1 rounded-full text-xs font-medium ${meta.badge}`}>{meta.label}</span>
              </div>
              <p className="text-sm text-slate-300">
                {selected.status === "CONFIRMED"
                  ? "This trip is already booked and paid — there's nothing more to pay."
                  : "This trip was cancelled, so it can't be paid for. You can plan a new one."}
              </p>
              <div className="flex flex-wrap gap-3">
                {selected.status === "CONFIRMED" ? (
                  <Link href={`/trip-summary?tripId=${selected.tripRequestId}`} className="btn-primary">View trip</Link>
                ) : (
                  <Link href="/trip-planner" className="btn-primary">Plan a new trip</Link>
                )}
                <Link href="/bookings" className="btn-outline">My trips</Link>
              </div>
            </div>
          )}

          {/* Pay form */}
          {loaded && !paid && !loadError && payable.length > 0 && (selectedPayable || !selected) && (
            <div className="space-y-4">
              <label className="block space-y-2">
                <span className="text-sm font-semibold">Trip to pay for</span>
                <select
                  value={selectedPayable ? tripId : ""}
                  onChange={(e) => {
                    setTripId(e.target.value);
                    setError(null);
                  }}
                  disabled={paying}
                  className="w-full border rounded-xl px-4 py-2"
                >
                  <option value="">-- Choose a trip --</option>
                  {payable.map((t) => (
                    <option key={t.tripRequestId} value={String(t.tripRequestId)}>
                      {`#${t.tripRequestId} — ${tripTitle(t)} · ${formatCost(t.estimatedCost)} · ${statusMeta(t.status).label}`}
                    </option>
                  ))}
                </select>
              </label>

              {selected && selectedPayable && (
                <div className="rounded-xl border border-slate-800 bg-slate-900/60 p-4 flex flex-wrap items-center justify-between gap-4">
                  <div className="space-y-1">
                    <div className="flex flex-wrap items-center gap-2">
                      <p className="font-semibold">{tripTitle(selected)}</p>
                      <span className={`px-2 py-1 rounded-full text-xs font-medium ${meta.badge}`}>{meta.label}</span>
                    </div>
                    <p className="text-sm text-slate-400">
                      #{selected.tripRequestId} · {tripKind(selected)}
                    </p>
                    <Link
                      href={`/trip-summary?tripId=${selected.tripRequestId}`}
                      className="text-sm text-brand-400 hover:underline"
                    >
                      Review trip details
                    </Link>
                  </div>
                  <div className="text-right">
                    <p className="text-xs uppercase tracking-wide text-slate-500">Amount</p>
                    <p className="text-2xl font-bold">{formatCost(selected.estimatedCost)}</p>
                  </div>
                </div>
              )}

              <div className="flex flex-wrap items-center gap-3">
                <button
                  className="btn-primary"
                  onClick={handlePay}
                  disabled={!selected || !selectedPayable || !hasCost || paying}
                >
                  {paying
                    ? "Processing…"
                    : selected && hasCost
                      ? `Pay ${formatCost(selected.estimatedCost)}`
                      : "Pay"}
                </button>
                <Link href="/bookings" className="btn-outline">Back to my trips</Link>
              </div>

              {selected && selectedPayable && !hasCost && (
                <p className="text-sm text-yellow-300">This trip has no price yet, so it can't be paid for.</p>
              )}
              {orderId && paying && <p className="text-xs text-slate-500">Order {orderId}</p>}
            </div>
          )}

          {/* Nothing to pay for */}
          {loaded && !paid && !loadError && payable.length === 0 && !selected && (
            <div className="rounded-xl border border-slate-800 p-6 text-center space-y-3">
              <p className="text-slate-300">Nothing to pay for right now.</p>
              <p className="text-sm text-slate-400">Plan a trip or pick a package, then come back here to book it.</p>
              <div className="flex justify-center gap-3">
                <Link href="/trip-planner" className="btn-primary">Plan a trip</Link>
                <Link href="/packages" className="btn-outline">View packages</Link>
                <Link href="/bookings" className="btn-outline">My trips</Link>
              </div>
            </div>
          )}

          {error && (
            <p className="text-sm text-red-400 bg-red-500/10 border border-red-500/20 rounded-xl px-4 py-3" role="alert">
              {error}
            </p>
          )}
        </div>
      </div>
    </ProtectedPage>
  );
}
