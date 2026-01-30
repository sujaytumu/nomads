import { api } from "./api";

export type UserResponse = {
  id: number;
  name: string;
  email: string;
  city: string;
  latitude: number;
  longitude: number;
  interestType: string;
  travelPreference: string;
};

export async function fetchUser(userId: number) {
  const { data } = await api.get<UserResponse>(`/api/users/${userId}`);
  return data;
}
