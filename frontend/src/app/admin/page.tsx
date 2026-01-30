"use client";

import ProtectedPage from "@/components/ProtectedPage";
import {
    createPlace,
    createVehicle,
    deletePlace,
    deleteVehicle,
    listPlaces,
    listVehicles,
} from "@/lib/adminApi";
import { useEffect, useState } from "react";

export default function AdminPage() {
  const [places, setPlaces] = useState<any[]>([]);
  const [vehicles, setVehicles] = useState<any[]>([]);
  const [error, setError] = useState<string | null>(null);

  const [placeForm, setPlaceForm] = useState({
    name: "",
    city: "",
    latitude: "",
    longitude: "",
    category: "CULTURE",
    rating: "4.5",
  });

  const [vehicleForm, setVehicleForm] = useState({
    vehicleType: "CAB",
    capacity: "3",
    driverName: "",
    vehicleNumber: "",
    availabilityStatus: "AVAILABLE",
  });

  const loadData = async () => {
    setError(null);
    try {
      const [placesData, vehiclesData] = await Promise.all([listPlaces(), listVehicles()]);
      setPlaces(placesData);
      setVehicles(vehiclesData);
    } catch (err) {
      setError("Admin data load failed");
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  const handlePlaceChange = (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) => {
    const { name, value } = e.target;
    setPlaceForm((prev) => ({ ...prev, [name]: value }));
  };

  const handleVehicleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) => {
    const { name, value } = e.target;
    setVehicleForm((prev) => ({ ...prev, [name]: value }));
  };

  const handleCreatePlace = async () => {
    setError(null);
    try {
      await createPlace({
        name: placeForm.name,
        city: placeForm.city,
        latitude: Number(placeForm.latitude),
        longitude: Number(placeForm.longitude),
        category: placeForm.category,
        rating: Number(placeForm.rating),
      });
      await loadData();
    } catch (err) {
      setError("Create place failed");
    }
  };

  const handleCreateVehicle = async () => {
    setError(null);
    try {
      await createVehicle({
        vehicleType: vehicleForm.vehicleType,
        capacity: Number(vehicleForm.capacity),
        driverName: vehicleForm.driverName,
        vehicleNumber: vehicleForm.vehicleNumber,
        availabilityStatus: vehicleForm.availabilityStatus,
      });
      await loadData();
    } catch (err) {
      setError("Create vehicle failed");
    }
  };

  return (
    <ProtectedPage requiredRole="ADMIN">
      <div className="section py-12 space-y-8">
        <div>
          <h2 className="text-2xl font-bold">Admin Console</h2>
          <p className="text-slate-600">Manage Places and Vehicles (admin role required).</p>
        </div>

        {error && <p className="text-sm text-red-600">{error}</p>}

        <div className="grid lg:grid-cols-2 gap-8">
          <div className="card p-6 space-y-4">
            <h3 className="text-lg font-semibold">Create Place</h3>
            <div className="grid gap-3">
              <input
                name="name"
                value={placeForm.name}
                onChange={handlePlaceChange}
                placeholder="Name"
                className="border rounded-xl px-4 py-2"
              />
              <input
                name="city"
                value={placeForm.city}
                onChange={handlePlaceChange}
                placeholder="City"
                className="border rounded-xl px-4 py-2"
              />
              <input
                name="latitude"
                value={placeForm.latitude}
                onChange={handlePlaceChange}
                placeholder="Latitude"
                className="border rounded-xl px-4 py-2"
              />
              <input
                name="longitude"
                value={placeForm.longitude}
                onChange={handlePlaceChange}
                placeholder="Longitude"
                className="border rounded-xl px-4 py-2"
              />
              <select
                name="category"
                value={placeForm.category}
                onChange={handlePlaceChange}
                className="border rounded-xl px-4 py-2"
              >
                {[
                  "FOOD",
                  "CULTURE",
                  "NATURE",
                  "ADVENTURE",
                  "SHOPPING",
                  "NIGHTLIFE",
                  "RELAXATION",
                ].map((item) => (
                  <option key={item} value={item}>
                    {item}
                  </option>
                ))}
              </select>
              <input
                name="rating"
                value={placeForm.rating}
                onChange={handlePlaceChange}
                placeholder="Rating"
                className="border rounded-xl px-4 py-2"
              />
            </div>
            <button className="btn-primary" onClick={handleCreatePlace}>
              Create Place
            </button>
          </div>

          <div className="card p-6 space-y-4">
            <h3 className="text-lg font-semibold">Create Vehicle</h3>
            <div className="grid gap-3">
              <select
                name="vehicleType"
                value={vehicleForm.vehicleType}
                onChange={handleVehicleChange}
                className="border rounded-xl px-4 py-2"
              >
                <option value="CAB">CAB</option>
                <option value="MINI_BUS">MINI_BUS</option>
                <option value="BUS">BUS</option>
              </select>
              <input
                name="capacity"
                value={vehicleForm.capacity}
                onChange={handleVehicleChange}
                placeholder="Capacity"
                className="border rounded-xl px-4 py-2"
              />
              <input
                name="driverName"
                value={vehicleForm.driverName}
                onChange={handleVehicleChange}
                placeholder="Driver Name"
                className="border rounded-xl px-4 py-2"
              />
              <input
                name="vehicleNumber"
                value={vehicleForm.vehicleNumber}
                onChange={handleVehicleChange}
                placeholder="Vehicle Number"
                className="border rounded-xl px-4 py-2"
              />
              <select
                name="availabilityStatus"
                value={vehicleForm.availabilityStatus}
                onChange={handleVehicleChange}
                className="border rounded-xl px-4 py-2"
              >
                <option value="AVAILABLE">AVAILABLE</option>
                <option value="ASSIGNED">ASSIGNED</option>
                <option value="UNAVAILABLE">UNAVAILABLE</option>
              </select>
            </div>
            <button className="btn-primary" onClick={handleCreateVehicle}>
              Create Vehicle
            </button>
          </div>
        </div>

        <div className="grid lg:grid-cols-2 gap-8">
          <div className="card p-6 space-y-4">
            <h3 className="text-lg font-semibold">Places</h3>
            <div className="grid gap-3">
              {places.map((place) => (
                <div key={place.id} className="flex items-center justify-between border rounded-xl p-3">
                  <div>
                    <p className="font-semibold">{place.name}</p>
                    <p className="text-xs text-slate-500">
                      {place.city} · {place.category}
                    </p>
                  </div>
                  <button
                    className="btn-outline"
                    onClick={async () => {
                      await deletePlace(place.id);
                      await loadData();
                    }}
                  >
                    Delete
                  </button>
                </div>
              ))}
            </div>
          </div>

          <div className="card p-6 space-y-4">
            <h3 className="text-lg font-semibold">Vehicles</h3>
            <div className="grid gap-3">
              {vehicles.map((vehicle) => (
                <div
                  key={vehicle.id}
                  className="flex items-center justify-between border rounded-xl p-3"
                >
                  <div>
                    <p className="font-semibold">
                      {vehicle.vehicleType} · {vehicle.vehicleNumber}
                    </p>
                    <p className="text-xs text-slate-500">
                      Driver: {vehicle.driverName} · {vehicle.availabilityStatus}
                    </p>
                  </div>
                  <button
                    className="btn-outline"
                    onClick={async () => {
                      await deleteVehicle(vehicle.id);
                      await loadData();
                    }}
                  >
                    Delete
                  </button>
                </div>
              ))}
            </div>
          </div>
        </div>
      </div>
    </ProtectedPage>
  );
}
