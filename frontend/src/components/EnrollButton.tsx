"use client";

import React, { useState } from "react";
import { useRouter } from "next/navigation";
import { api } from "../lib/api";

type Props = { packageId: number | string; amount: number | string };

export default function EnrollButton({ packageId, amount }: Props) {
  const [loading, setLoading] = useState(false);
  const router = useRouter();

  async function handleEnroll() {
    setLoading(true);
    try {
      const { data } = await api.post(`/api/packages/${packageId}/enroll`, { amount: Number(amount) });
      // The enrollment already created the trip; the payment page loads its
      // real price and status from the server using just the trip id.
      router.push(`/payment?tripRequestId=${data.tripRequestId}`);
      setLoading(false);
      return;
    } catch (e: any) {
      if (e?.response?.data) {
        alert('Enroll failed: ' + JSON.stringify(e.response.data));
      } else {
        alert('Enroll failed: ' + (e?.message || 'unknown error'));
      }
      setLoading(false);
      return;
    }
  }

  return (
    <button onClick={handleEnroll} className="btn-primary w-full" disabled={loading}>
      {loading ? 'Enrolling…' : 'Enroll & Pay'}
    </button>
  );
}
