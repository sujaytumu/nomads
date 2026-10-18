import { api, getCached } from "./api";

export async function register(payload: {
  name: string;
  email: string;
  phoneNumber?: string;
  password: string;
  city: string;
  latitude: number;
  longitude: number;
  interestType: string;
  travelPreference: string;
}) {
  const { data } = await api.post("/api/auth/register", payload);
  return data;
}

export async function login(payload: { email: string; password: string }) {
  const { data } = await api.post("/api/auth/login", payload);
  return data;
}

export type Me = {
    id: number;
    email: string;
    name?: string;
    phoneNumber?: string;
    city?: string;
    latitude?: number;
    longitude?: number;
    interestType?: string;
    travelPreference?: string;
    role?: string;
};

/** Who am I. Shared and briefly cached so the shell, guards and pages don't each refetch it. */
export function fetchMe(): Promise<Me> {
  return getCached<Me>("/api/auth/me", { ttl: 60000, persist: true });
}
