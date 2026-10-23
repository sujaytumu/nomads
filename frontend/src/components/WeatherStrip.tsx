"use client";

import { useEffect, useState } from "react";
import { fetchTripWeather, type DailyForecast, type WeatherForecast } from "@/lib/weatherApi";

export function weatherIcon(description?: string | null, rain = 0): string {
  const d = (description || "").toLowerCase();
  if (/thunder|storm/.test(d)) return "⛈️";
  if (/snow|sleet/.test(d)) return "❄️";
  if (/rain|shower|drizzle/.test(d) || rain >= 60) return "🌧️";
  if (/fog|mist|haze/.test(d)) return "🌫️";
  if (/partly|few clouds|scattered/.test(d)) return "⛅";
  if (/cloud|overcast/.test(d)) return "☁️";
  if (/clear|sun/.test(d)) return "☀️";
  return "🌤️";
}

const dayLabel = (iso: string) => {
  const d = new Date(`${iso}T00:00:00`);
  return Number.isNaN(d.getTime()) ? iso : d.toLocaleDateString("en-IN", { weekday: "short", day: "numeric", month: "short" });
};

/** A tip in plain words for the day, so the forecast is actually useful. */
export function weatherTip(d: DailyForecast): string {
  if (d.precipitationChance >= 60) return "Rain likely. Carry an umbrella.";
  if (d.precipitationChance >= 30) return "Chance of showers. Pack a light rain cover.";
  if (d.maxTempC >= 35) return "Hot day. Carry water and sunscreen.";
  if (d.minTempC <= 12) return "Cool. Take a jacket.";
  return "Good weather for the trip.";
}

export function WeatherDays({ days, city }: { days: DailyForecast[]; city?: string }) {
  if (!days.length) return null;
  return (
    <div className="space-y-2">
      {city && <p className="eyebrow">Weather in {city}</p>}
      <div className="grid grid-cols-2 gap-2.5 sm:grid-cols-3">
        {days.map((day) => (
          <div key={day.date} className="rounded-2xl border border-slate-800 bg-slate-900/60 p-3">
            <div className="flex items-center justify-between">
              <p className="text-xs font-semibold text-slate-400">{dayLabel(day.date)}</p>
              <span aria-hidden="true" className="text-2xl leading-none">{weatherIcon(day.description, day.precipitationChance)}</span>
            </div>
            <p className="mt-1 text-lg font-semibold">{Math.round(day.minTempC)}° – {Math.round(day.maxTempC)}°C</p>
            <p className="text-xs text-slate-400">{day.description} · {day.precipitationChance}% rain</p>
            <p className="mt-1.5 text-[11px] leading-snug text-ocean-300">{weatherTip(day)}</p>
          </div>
        ))}
      </div>
    </div>
  );
}

/** Forecast for the days of a trip (falls back to the next few days if the dates are not in range). */
export default function TripWeather({ tripId, start, days = 1 }: { tripId: number | string; start?: Date; days?: number }) {
  const [w, setW] = useState<WeatherForecast | null>(null);

  useEffect(() => {
    let live = true;
    fetchTripWeather(Number(tripId))
      .then((x) => live && setW(x))
      .catch(() => live && setW(null));
    return () => {
      live = false;
    };
  }, [tripId]);

  if (!w || !Array.isArray(w.days) || w.days.length === 0) return null;
  let shown = w.days;
  if (start) {
    const from = new Date(start);
    from.setHours(0, 0, 0, 0);
    const to = new Date(from.getTime() + Math.max(1, days) * 86_400_000);
    const inRange = w.days.filter((d) => {
      const t = new Date(`${d.date}T00:00:00`).getTime();
      return t >= from.getTime() && t < to.getTime();
    });
    if (inRange.length) shown = inRange;
  }
  return <WeatherDays days={shown.slice(0, 3)} city={w.city} />;
}
