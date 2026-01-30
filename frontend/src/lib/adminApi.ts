import { api } from "./api";

export type PlaceCreatePayload = {
  name: string;
  city: string;
  latitude: number;
  longitude: number;
  category: string;
  rating: number;
};

export type VehicleCreatePayload = {
  vehicleType: string;
  capacity: number;
  driverName: string;
  vehicleNumber: string;
  availabilityStatus: string;
};

export async function createPlace(payload: PlaceCreatePayload) {
  const { data } = await api.post("/api/admin/places", payload);
  return data;
}

export async function listPlaces() {
  const { data } = await api.get("/api/admin/places");
  return data as Array<any>;
}

export async function deletePlace(id: number) {
  await api.delete(`/api/admin/places/${id}`);
}

export async function createVehicle(payload: VehicleCreatePayload) {
  const { data } = await api.post("/api/admin/vehicles", payload);
  return data;
}

export async function listVehicles() {
  const { data } = await api.get("/api/admin/vehicles");
  return data as Array<any>;
}

export async function deleteVehicle(id: number) {
  await api.delete(`/api/admin/vehicles/${id}`);
}
