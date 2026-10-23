"use client";

import React from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState } from "react";
import { api } from "../lib/api";
import WishlistHeart from "./WishlistHeart";
import Cover from "./Cover";
import { errorText, toast } from "@/lib/toast";

type Props = {
  pkg: any;
  // Optional: when provided, a wishlist heart is shown on the card.
  saved?: boolean;
  onSavedChange?: (packageId: number, saved: boolean) => void;
};

export default function PackageCard({ pkg, saved, onSavedChange }: Props) {
  const router = useRouter();
  const [loading, setLoading] = useState(false);

  async function handleEnroll() {
    setLoading(true);
    try {
      const { data } = await api.post(`/api/packages/${pkg.id}/enroll`, { amount: Number(pkg.price) });
      router.push(`/payment?tripRequestId=${data.tripRequestId}`);
      setLoading(false);
      return;
    } catch (e: any) {
      toast(errorText(e, "Couldn't enroll. Please try again."), "error");
      setLoading(false);
      return;
    }
  }

  return (
    <div className="card group overflow-hidden transition-all hover:-translate-y-0.5 hover:shadow-lift">
      <div className="relative">
        <Cover src={pkg.imageUrl} alt={pkg.name} kind="PACKAGE" className="h-48" />
        <span className="absolute bottom-3 left-3 rounded-full bg-black/55 backdrop-blur ring-1 ring-white/10 px-3 py-1 text-sm font-bold text-white shadow-sm">₹{pkg.price}</span>
        {onSavedChange && (
          <span className="absolute right-3 top-3 grid h-9 w-9 place-items-center rounded-full bg-black/55 backdrop-blur ring-1 ring-white/10 text-rose-400 shadow-sm">
            <WishlistHeart packageId={pkg.id} saved={!!saved} onChange={onSavedChange} />
          </span>
        )}
      </div>
      <div className="space-y-3 p-4">
        <div>
          <h3 className="font-display text-lg font-semibold leading-snug">{pkg.name}</h3>
          <p className="mt-1 line-clamp-2 text-sm text-slate-400">{pkg.shortDescription}</p>
        </div>
        <div className="flex gap-2">
          <Link href={`/packages/${pkg.id}`} className="btn btn-secondary btn-sm flex-1">Explore</Link>
          <button className="btn btn-primary btn-sm flex-1" onClick={handleEnroll} disabled={loading} data-loading={loading}>{loading ? 'Enrolling…' : 'Enroll & Pay'}</button>
        </div>
      </div>
    </div>
  );
}
