export type PackItem = { id: string; label: string };
export type PackSection = { title: string; items: PackItem[] };

export type PackInput = {
  interest?: string | null;
  weekendType?: string | null;
  travelMode?: string | null;
  groupSize?: number | null;
  pickupRequired?: boolean | null;
  /** ISO date (yyyy-mm-dd) of the trip, used for the season. */
  tripDate?: string | null;
};

const item = (label: string): PackItem => ({ id: label.toLowerCase().replace(/[^a-z0-9]+/g, "-").replace(/^-|-$/g, ""), label });

const BY_INTEREST: Record<string, string[]> = {
  FOOD: ["Antacid / digestive tablets", "Wet wipes", "Small cash for street stalls"],
  CULTURE: ["Modest clothes for temples", "Socks (shoes come off at temples)", "Cloth bag for souvenirs"],
  NATURE: ["Insect repellent", "Hat or cap", "Binoculars or a good phone zoom", "Small first-aid kit"],
  ADVENTURE: ["Sturdy trail shoes", "Spare pair of socks", "First-aid kit", "Quick-dry clothes", "Energy snacks"],
  SHOPPING: ["Foldable extra bag", "Cash and a spare UPI app", "Comfortable slip-on shoes"],
  NIGHTLIFE: ["Light jacket for late nights", "Portable charger", "Booked ride home saved offline"],
  RELAXATION: ["Swimwear", "Book or headphones", "Light, loose clothes"],
};

/** Month (0-11) to a season for India: rain, hot or cool. Unknown dates give none. */
export function seasonOf(iso?: string | null): "rain" | "hot" | "cool" | null {
  if (!iso) return null;
  const m = Number(iso.slice(5, 7));
  if (!m || m < 1 || m > 12) return null;
  if (m >= 6 && m <= 9) return "rain";
  if (m >= 3 && m <= 5) return "hot";
  return "cool";
}

/** A packing list that fits the trip: essentials, then what the days, season, interest and group call for. Same input always gives the same list. */
export function packingPlan(input: PackInput): PackSection[] {
  const sections: PackSection[] = [];
  const two = input.weekendType === "TWO_DAY";
  const group = input.travelMode === "GROUP" || (input.groupSize ?? 1) > 1;

  sections.push({
    title: "Essentials",
    items: ["Government photo ID", "Phone + charger / power bank", "Cash / UPI ready", "Water bottle", "This itinerary (PDF saved offline)"].map(item),
  });

  const clothes = ["Comfortable walking shoes", two ? "2 sets of clothes + 1 spare" : "1 spare t-shirt"];
  if (two) clothes.push("Toiletries kit", "Night wear", "Slippers");
  const season = seasonOf(input.tripDate);
  if (season === "rain") clothes.push("Umbrella or raincoat", "Waterproof pouch for phone");
  else if (season === "hot") clothes.push("Sunscreen", "Sunglasses", "Cap or scarf", "Extra water or ORS");
  else if (season === "cool") clothes.push("Light jacket or sweater");
  else clothes.push("Sunscreen or umbrella");
  sections.push({ title: two ? "Clothes and overnight" : "Clothes and weather", items: clothes.map(item) });

  const extra = BY_INTEREST[input.interest ?? ""];
  if (extra) sections.push({ title: "For your plans", items: extra.map(item) });

  const together: string[] = [];
  if (group) together.push("Agree a meeting point", "Share live location with the group", "Carry a few spare snacks");
  if (input.pickupRequired) together.push("Pickup details saved offline");
  if (together.length) sections.push({ title: "Together", items: together.map(item) });

  // An item should only appear once, even if two sections suggest it.
  const seen = new Set<string>();
  return sections
    .map((s) => ({ ...s, items: s.items.filter((i) => (seen.has(i.id) ? false : (seen.add(i.id), true))) }))
    .filter((s) => s.items.length > 0);
}
