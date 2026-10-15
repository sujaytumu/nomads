import { api } from "./api";
import { saveBlob } from "./download";

export type TripCreatePayload = {
  userId: number;
  city?: string;
  weekendType?: string;
  interest?: string;
  travelMode?: string;
  pickupRequired?: boolean;
  groupSize?: number;
  userLatitude?: number;
  userLongitude?: number;
};

export async function createTrip(payload: TripCreatePayload) {
  const { data } = await api.post("/api/trips/create", payload);
  return data;
}

export async function fetchTrip(tripId: number) {
  const { data } = await api.get(`/api/trips/${tripId}`);
  return data;
}

export type ItineraryOption = {
  placeId: number;
  name: string;
  category: string;
  rating: number | null;
  imageUrl?: string | null;
  distanceFromHomeKm?: number | null;
  selected: boolean;
};

export type ItineraryOptions = {
  tripRequestId: number;
  editable: boolean;
  minPlaces: number;
  maxPlaces: number;
  costPerPlace: number;
  pickupCost: number;
  pickupRequired: boolean;
  options: ItineraryOption[];
};

/** Places the traveller can choose from for this trip, with the current picks marked. */
export async function fetchItineraryOptions(tripId: number): Promise<ItineraryOptions> {
  const { data } = await api.get(`/api/trips/${tripId}/itinerary/options`);
  return data;
}

/** Rebuilds the trip's day-by-day plan and price from the chosen places. */
export async function saveItinerary(tripId: number, placeIds: number[]) {
  const { data } = await api.put(`/api/trips/${tripId}/itinerary`, { placeIds });
  return data;
}

// With responseType "blob" an error body also arrives as a Blob, so the
// server's message has to be read out of it.
async function readBlobMessage(data: unknown): Promise<string | null> {
  if (typeof Blob === "undefined" || !(data instanceof Blob)) return null;
  try {
    const parsed = JSON.parse(await data.text());
    return typeof parsed?.message === "string" ? parsed.message : null;
  } catch {
    return null;
  }
}

/** Downloads the itinerary PDF. Goes through axios so the login token is sent. */
export async function downloadItineraryPdf(tripId: number): Promise<void> {
  try {
    const res = await api.get(`/api/trips/${tripId}/itinerary.pdf`, { responseType: "blob" });
    saveBlob(res.data, `nomad-itinerary-${tripId}.pdf`);
  } catch (err: any) {
    const message = await readBlobMessage(err?.response?.data);
    throw new Error(message ?? "Couldn't create the PDF. Please try again.");
  }
}
