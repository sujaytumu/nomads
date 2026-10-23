"use client";

import { useState } from "react";
import { toast } from "@/lib/toast";

type Props = {
  onSet: (coords: { latitude: number; longitude: number }) => void;
};

export default function UseLocationButton({ onSet }: Props) {
  const [loading, setLoading] = useState(false);

  const handleClick = () => {
    if (!navigator.geolocation) {
      toast("Location isn't available on this device.", "error");
      return;
    }
    setLoading(true);
    navigator.geolocation.getCurrentPosition(
      (pos) => {
        setLoading(false);
        onSet({ latitude: pos.coords.latitude, longitude: pos.coords.longitude });
      },
      (err) => {
        setLoading(false);
        toast("Couldn't get your location. Check that location is allowed.", "error");
      },
      { enableHighAccuracy: true, timeout: 10000 }
    );
  };

  return (
    <button type="button" className="btn-outline" onClick={handleClick} disabled={loading}>
      {loading ? "Locating…" : "Use my location"}
    </button>
  );
}
