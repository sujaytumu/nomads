"use client";

import { login, register } from "@/lib/authApi";
import { useState } from "react";
import { useRouter } from "next/navigation";
import UseLocationButton from "@/components/UseLocationButton";
import dynamic from "next/dynamic";

const MapPicker = dynamic(() => import("@/components/MapPicker"), { ssr: false });

export default function AuthPage() {
  const [mode, setMode] = useState<"login" | "register">("login");
  const [token, setToken] = useState<string | null>(null);
  const [form, setForm] = useState({
    name: "",
    email: "",
    phoneNumber: "",
    password: "",
    city: "",
    latitude: "",
    longitude: "",
    interestType: "CULTURE",
    travelPreference: "SOLO",
  });
  const [error, setError] = useState<string | null>(null);
  const router = useRouter();

  const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) => {
    const { name, value } = e.target;
    setForm((prev) => ({ ...prev, [name]: value }));
  };

  const handleSubmit = async () => {
    setError(null);
    try {
      if (mode === "register") {
        const data = await register({
          name: form.name,
          email: form.email,
          phoneNumber: form.phoneNumber,
          password: form.password,
          city: form.city,
          latitude: Number(form.latitude),
          longitude: Number(form.longitude),
          interestType: form.interestType,
          travelPreference: form.travelPreference,
        });
        localStorage.setItem("nomad_token", data.token);
        setToken(data.token);
        // full navigation to home so the app reloads with auth state
        window.location.href = "/";
      } else {
        const data = await login({ email: form.email, password: form.password });
        localStorage.setItem("nomad_token", data.token);
        setToken(data.token);
        // full navigation to home so the app reloads with auth state
        window.location.href = "/";
      }
    } catch (err) {
      // try to read server message
      const message = (err as any)?.response?.data?.message || (err as any)?.message || "Auth failed";
      setError(String(message));
    }
  };

  return (
    <div className="section py-12 space-y-6">
      <div className="card p-6 space-y-4">
        <div className="flex items-center justify-between">
          <h2 className="text-2xl font-bold">Authentication</h2>
          <div className="flex gap-2">
            <button className={`btn-outline ${mode === "login" ? "bg-slate-100" : ""}`} onClick={() => setMode("login")}>Login</button>
            <button className={`btn-outline ${mode === "register" ? "bg-slate-100" : ""}`} onClick={() => setMode("register")}>Register</button>
          </div>
        </div>

        <div className="grid md:grid-cols-2 gap-4">
          {mode === "register" && (
            <input name="name" value={form.name} onChange={handleChange} placeholder="Name" className="border rounded-xl px-4 py-2" />
          )}
          <input name="email" value={form.email} onChange={handleChange} placeholder="Email" className="border rounded-xl px-4 py-2" />
          <input name="password" type="password" value={form.password} onChange={handleChange} placeholder="Password" className="border rounded-xl px-4 py-2" />
          {mode === "register" && (
            <>
              <input name="city" value={form.city} onChange={handleChange} placeholder="City" className="border rounded-xl px-4 py-2" />
              <input name="phoneNumber" value={form.phoneNumber} onChange={handleChange} placeholder="Phone Number (+91...)" className="border rounded-xl px-4 py-2" />
              <div className="col-span-2">
                <div className="flex gap-2 mb-2">
                  <UseLocationButton onSet={(c) => setForm((p) => ({ ...p, latitude: String(c.latitude), longitude: String(c.longitude) }))} />
                </div>
                <MapPicker onSet={(c) => setForm((p) => ({ ...p, latitude: String(c.latitude), longitude: String(c.longitude) }))} />
              </div>
              <input name="latitude" value={form.latitude} readOnly placeholder="Latitude" className="border rounded-xl px-4 py-2 bg-gray-50" />
              <input name="longitude" value={form.longitude} readOnly placeholder="Longitude" className="border rounded-xl px-4 py-2 bg-gray-50" />
              <select name="interestType" value={form.interestType} onChange={handleChange} className="border rounded-xl px-4 py-2">
                {["FOOD", "CULTURE", "NATURE", "ADVENTURE", "SHOPPING", "NIGHTLIFE", "RELAXATION"].map((item) => (
                  <option key={item} value={item}>{item}</option>
                ))}
              </select>
              <select name="travelPreference" value={form.travelPreference} onChange={handleChange} className="border rounded-xl px-4 py-2">
                <option value="SOLO">SOLO</option>
                <option value="GROUP">GROUP</option>
              </select>
            </>
          )}
        </div>
        <button className="btn-primary" onClick={handleSubmit}>{mode === "register" ? "Register" : "Login"}</button>
        {token && <p className="text-sm text-slate-600">Token saved (copy manually for now)</p>}
        {error && <p className="text-sm text-red-600">{error}</p>}
      </div>
    </div>
  );
}

