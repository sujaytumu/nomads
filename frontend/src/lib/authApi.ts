import { api } from "./api";

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

export async function fetchMe() {
  const { data } = await api.get("/api/auth/me");
  return data as {
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
}
