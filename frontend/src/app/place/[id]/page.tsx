"use client";

import React, { useEffect, useState } from "react";
import { useParams } from "next/navigation";
import { api } from "@/lib/api";
import { toast } from "@/lib/toast";
import { PlaceSkeleton } from "@/components/Skeleton";
import SavedPlaceHeart from "@/components/SavedPlaceHeart";
import { fetchSavedPlaceIds } from "@/lib/savedPlacesApi";
import PlaceRatingsSection from "@/components/PlaceRatings";

export default function PlaceDetailsPage() {
  const params = useParams();
  const id = params?.id;
  const [place, setPlace] = useState<any | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [saved, setSaved] = useState(false);

  useEffect(() => {
    if (!id || !localStorage.getItem("nomad_token")) return;
    fetchSavedPlaceIds().then((ids) => setSaved(ids.includes(Number(id)))).catch(() => {});
  }, [id]);

  useEffect(() => {
    if (!id) return;
    let cancelled = false;
    async function load() {
      setLoading(true);
      try {
        const res = await api.get(`/api/places/${id}`);
        if (!cancelled) setPlace(res.data);
      } catch (e: any) {
        if (!cancelled) setError(e?.response?.data?.message || e.message || "Failed to load place");
      } finally {
        if (!cancelled) setLoading(false);
      }
    }
    load();
    return () => { cancelled = true; };
  }, [id]);

  function handleAdd() {
    if (!place) return;
    try {
      const raw = localStorage.getItem("nomad_tour");
      const arr = raw ? JSON.parse(raw) : [];
      if (!arr.find((p: any) => p.id === place.id)) {
        arr.push({ id: place.id, name: place.name });
        localStorage.setItem("nomad_tour", JSON.stringify(arr));
        toast(`${place.name} added to your tour`, "success");
      } else {
        toast(`${place.name} is already in your tour`);
      }
    } catch (err) {
      console.error(err);
      toast("Couldn't add this place. Please try again.", "error");
    }
  }

  if (loading) return <PlaceSkeleton />;
  if (error) return <div className="text-red-400">{error}</div>;
  if (!place) return <div>Place not found.</div>;

  return (
    <div className="section py-8">
      <div className="grid md:grid-cols-3 gap-6">
        <div className="col-span-2">
          <img src={place.imageUrl || "/images/place-placeholder.svg"} alt={place.name} className="w-full h-72 object-cover rounded-md" />
          <h2 className="text-2xl font-bold mt-4">{place.name}</h2>
          <p className="text-slate-300 mt-2">{place.description || place.shortDescription}</p>
        </div>
        <div className="card p-4">
          <div className="space-y-3">
            <div>
              <h4 className="font-semibold">Details</h4>
              <p className="text-sm text-slate-300 mt-1">{place.address || "Address not available"}</p>
            </div>
            <div>
              <h4 className="font-semibold">Timings</h4>
              <p className="text-sm text-slate-300 mt-1">{place.timing || "Open daily"}</p>
            </div>
            <div className="flex gap-2">
              <button className="btn-primary" onClick={handleAdd}>Add to tour</button>
              <SavedPlaceHeart placeId={place.id} saved={saved} onChange={(_, on) => setSaved(on)} />
            </div>
          </div>
        </div>
      </div>
      <PlaceRatingsSection placeId={place.id} />
    </div>
  );
}
