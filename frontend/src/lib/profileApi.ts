import { api } from "./api";

export async function updateUser(userId: number, payload: {
  name: string;
  city: string;
  phoneNumber?: string;
  /** Empty string clears it; leaving it out keeps what is saved. */
  upiId?: string;
  latitude: number;
  longitude: number;
  interestType: string;
  travelPreference: string;
}) {
  const { data } = await api.put(`/api/users/${userId}`, payload);
  return data;
}