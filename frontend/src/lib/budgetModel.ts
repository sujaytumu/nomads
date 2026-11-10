export type Tier = "NONE" | "BUDGET" | "COMFORT" | "PREMIUM";

export const TIERS: Tier[] = ["NONE", "BUDGET", "COMFORT", "PREMIUM"];
export const TIER_LABEL: Record<Tier, string> = { NONE: "No stay", BUDGET: "Budget", COMFORT: "Comfort", PREMIUM: "Premium" };
export const FOOD_PER_DAY: Record<Tier, number> = { NONE: 350, BUDGET: 350, COMFORT: 650, PREMIUM: 1200 };
export const DEFAULT_RATE: Record<Exclude<Tier, "NONE">, number> = { BUDGET: 1500, COMFORT: 3200, PREMIUM: 6500 };
export const CAB_PER_KM = 14;
export const ROAD_FACTOR = 1.3;

export type BudgetInput = {
  days: number;
  people: number;
  tier: Tier;
  /** Nightly room price per tier for this city; missing tiers fall back to the defaults. */
  rates?: Partial<Record<Exclude<Tier, "NONE">, number>>;
  /** Straight-line km driven in the plan, before the road factor. */
  km: number;
  nomad: number;
};

export type Budget = { nights: number; rooms: number; stay: number; food: number; travel: number; roadKm: number; cabs: number; nomad: number; total: number };

export function computeBudget(i: BudgetInput): Budget {
  const days = Math.max(1, Math.floor(i.days));
  const people = Math.max(1, Math.floor(i.people));
  const nights = Math.max(0, days - 1);
  const rooms = Math.ceil(people / 2);
  const rate = i.tier === "NONE" ? 0 : i.rates?.[i.tier] ?? DEFAULT_RATE[i.tier];
  const stay = nights * rooms * rate;
  const food = days * people * FOOD_PER_DAY[i.tier];
  const cabs = Math.ceil(people / 4);
  const roadKm = Math.max(0, i.km) * ROAD_FACTOR;
  const travel = roadKm * CAB_PER_KM * cabs;
  const nomad = Math.max(0, i.nomad);
  return { nights, rooms, stay, food, travel, roadKm, cabs, nomad, total: nomad + stay + food + travel };
}

export type Saving = { tier: Tier; saves: number; newTotal: number; fits: boolean };

export type CapAdvice = {
  state: "none" | "within" | "over";
  /** Positive when under the cap, negative when over it. */
  headroom: number;
  savings: Saving[];
  /** The least drastic switch that fits the cap, if any (highest comfort that still fits). */
  bestFit: Saving | null;
  message: string;
};

const rupees = (n: number) => `₹${Math.round(Math.abs(n)).toLocaleString("en-IN")}`;

/** Compares the total with a cap (per person per trip) and lists cheaper comfort levels with what each saves. */
export function adviseCap(i: BudgetInput, capPerPerson: number | null): CapAdvice {
  const now = computeBudget(i);
  const cap = capPerPerson && capPerPerson > 0 ? capPerPerson * Math.max(1, Math.floor(i.people)) : null;
  const idx = TIERS.indexOf(i.tier);
  const savings: Saving[] = TIERS.slice(0, idx)
    .reverse() // nearest cheaper level first
    .map((t) => {
      const b = computeBudget({ ...i, tier: t });
      return { tier: t, saves: Math.round(now.total - b.total), newTotal: b.total, fits: cap !== null && b.total <= cap };
    })
    .filter((s) => s.saves > 0);

  if (cap === null) return { state: "none", headroom: 0, savings, bestFit: null, message: "" };
  const headroom = cap - now.total;
  if (headroom >= 0) {
    return { state: "within", headroom, savings, bestFit: null, message: `Within your cap, with ${rupees(headroom)} to spare.` };
  }
  const bestFit = savings.find((s) => s.fits) ?? null;
  const message = bestFit
    ? `${rupees(headroom)} over your cap. Switching to ${TIER_LABEL[bestFit.tier]} saves ${rupees(bestFit.saves)} and fits.`
    : savings.length > 0
      ? `${rupees(headroom)} over your cap, and even ${TIER_LABEL[savings[savings.length - 1].tier]} would not fit. Try fewer stops or a smaller group.`
      : `${rupees(headroom)} over your cap. Try fewer stops or a smaller group.`;
  return { state: "over", headroom, savings, bestFit, message };
}
