"use client";

import L from "leaflet";
import "leaflet/dist/leaflet.css";
import "@/lib/leafletIconFix";
import { useCallback, useEffect, useRef, useState } from "react";

import { fetchLive, shareLive, stopLive, type SharedPosition } from "@/lib/liveApi";
import { errorText, toast } from "@/lib/toast";

const SEND_EVERY_MS = 20000;
const POLL_EVERY_MS = 15000;

const ago = (s: number) => (s < 45 ? "just now" : s < 3600 ? `${Math.round(s / 60)} min ago` : `${Math.round(s / 3600)} h ago`);

/** Opt-in live positions for the group. Nothing is shared until you press Share, and Stop removes it at once. */
export default function LiveGroupMap({ groupId }: { groupId: number }) {
  const box = useRef<HTMLDivElement | null>(null);
  const map = useRef<L.Map | null>(null);
  const layer = useRef<L.LayerGroup | null>(null);
  const fitted = useRef(false);
  const watch = useRef<number | null>(null);
  const lastSent = useRef(0);
  const [sharing, setSharing] = useState(false);
  const [people, setPeople] = useState<SharedPosition[]>([]);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(() => {
    fetchLive(groupId).then(setPeople).catch(() => undefined);
  }, [groupId]);

  useEffect(() => {
    load();
    const t = window.setInterval(load, POLL_EVERY_MS);
    return () => window.clearInterval(t);
  }, [load]);

  // Stop watching the phone's position when leaving the tab.
  useEffect(() => {
    return () => {
      if (watch.current != null) navigator.geolocation.clearWatch(watch.current);
    };
  }, []);

  useEffect(() => {
    if (!box.current || map.current) return;
    const m = L.map(box.current).setView([20.59, 78.96], 4);
    L.tileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png", {
      attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors',
      maxZoom: 19,
    }).addTo(m);
    layer.current = L.layerGroup().addTo(m);
    map.current = m;
    return () => {
      m.remove();
      map.current = null;
    };
  }, []);

  useEffect(() => {
    const g = layer.current;
    const m = map.current;
    if (!g || !m) return;
    g.clearLayers();
    const pts: L.LatLngTuple[] = [];
    people.forEach((p) => {
      pts.push([p.lat, p.lng]);
      L.marker([p.lat, p.lng]).bindPopup(`<b>${p.me ? "You" : p.name.replace(/[<>&]/g, "")}</b><br/>${ago(p.secondsAgo)}`).addTo(g);
    });
    if (pts.length && !fitted.current) {
      fitted.current = true;
      if (pts.length === 1) m.setView(pts[0], 15);
      else m.fitBounds(L.latLngBounds(pts), { padding: [40, 40], maxZoom: 16 });
    }
  }, [people]);

  const start = () => {
    setError(null);
    if (!("geolocation" in navigator)) {
      setError("This device can't share its location.");
      return;
    }
    setSharing(true);
    watch.current = navigator.geolocation.watchPosition(
      (pos) => {
        const now = Date.now();
        if (now - lastSent.current < SEND_EVERY_MS) return;
        lastSent.current = now;
        shareLive(groupId, pos.coords.latitude, pos.coords.longitude, pos.coords.accuracy)
          .then(load)
          .catch((e) => setError(errorText(e, "Couldn't share your position.")));
      },
      (err) => {
        setError(err.code === 1 ? "Location is blocked. Allow it in your browser or phone settings." : "Couldn't find your position.");
        stop(false);
      },
      { enableHighAccuracy: true, maximumAge: 10000, timeout: 30000 }
    );
  };

  const stop = (tell = true) => {
    if (watch.current != null) navigator.geolocation.clearWatch(watch.current);
    watch.current = null;
    lastSent.current = 0;
    setSharing(false);
    if (tell) {
      stopLive(groupId).then(() => { toast("You stopped sharing your location.", "success"); load(); }).catch(() => toast("Couldn't stop sharing. Try again.", "error"));
    }
  };

  return (
    <div className="card space-y-4 p-6">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h3 className="text-lg font-semibold">Live location</h3>
          <p className="text-sm text-slate-400">Only your group sees it. Off until you press Share, and it fades after 15 minutes.</p>
        </div>
        {sharing ? (
          <button type="button" className="btn btn-secondary" onClick={() => stop()}>Stop sharing</button>
        ) : (
          <button type="button" className="btn btn-primary" onClick={start}>Share my location</button>
        )}
      </div>
      {sharing && <p className="text-sm text-emerald-300" aria-live="polite">Sharing your location with the group. Keep this page open.</p>}
      {error && <p role="alert" className="text-sm text-red-400">{error}</p>}
      <div ref={box} className="h-72 w-full overflow-hidden rounded-2xl border border-slate-800" />
      {people.length === 0 ? (
        <p className="text-sm text-slate-400">Nobody is sharing right now.</p>
      ) : (
        <ul className="space-y-1.5 text-sm text-slate-300">
          {people.map((p) => (
            <li key={p.userId} className="flex justify-between"><span>{p.me ? "You" : p.name}</span><span className="text-slate-400">{ago(p.secondsAgo)}</span></li>
          ))}
        </ul>
      )}
    </div>
  );
}
