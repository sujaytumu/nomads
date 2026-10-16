import { saveBlob } from "./download";

const ymd = (d: Date) => d.toISOString().slice(0, 10).replace(/-/g, "");

/** Downloads an all-day calendar event (.ics) for the trip, so it lands in any calendar app. */
export function downloadTripCalendar(trip: { tripRequestId: number | string; city?: string | null; tripDate: string; plans?: any[] | null }) {
  const days = Math.max(1, ...(trip.plans || []).map((p: any) => Number(p.dayNumber) || 1));
  const start = new Date(`${trip.tripDate}T00:00:00Z`);
  const end = new Date(start.getTime() + days * 86400000);
  const stops = (trip.plans || []).map((p: any) => `Day ${p.dayNumber} ${String(p.startTime || "").slice(0, 5)} ${p.placeName}`);
  const lines = [
    "BEGIN:VCALENDAR",
    "VERSION:2.0",
    "PRODID:-//NOMAD//Trips//EN",
    "BEGIN:VEVENT",
    `UID:nomad-trip-${trip.tripRequestId}@nomad`,
    `DTSTAMP:${new Date().toISOString().replace(/[-:]/g, "").slice(0, 15)}Z`,
    `DTSTART;VALUE=DATE:${ymd(start)}`,
    `DTEND;VALUE=DATE:${ymd(end)}`,
    `SUMMARY:NOMAD trip${trip.city ? ` to ${trip.city}` : ""}`,
    `DESCRIPTION:Booking #${trip.tripRequestId}${stops.length ? "\\n" + stops.join("\\n") : ""}`,
    "END:VEVENT",
    "END:VCALENDAR",
  ];
  saveBlob(new Blob([lines.join("\r\n")], { type: "text/calendar" }), `nomad-trip-${trip.tripRequestId}.ics`);
}
