"use client";

import { login, register } from "@/lib/authApi";
import { useEffect, useState } from "react";
import UseLocationButton from "@/components/UseLocationButton";
import dynamic from "next/dynamic";

const MapPicker = dynamic(() => import("@/components/MapPicker"), { ssr: false });

export default function AuthPage() {
  const [mode, setMode] = useState<"login" | "register">("login");
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
  const [submitting, setSubmitting] = useState(false);

  // Nav "Sign up" links arrive as /auth?mode=register
  useEffect(() => {
    if (new URLSearchParams(window.location.search).get("mode") === "register") {
      setMode("register");
    }
  }, []);

  const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) => {
    const { name, value } = e.target;
    setForm((prev) => ({ ...prev, [name]: value }));
  };

  const handleSubmit = async () => {
    if (submitting) return;
    setSubmitting(true);
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
        // full navigation to home so the app reloads with auth state
        window.location.href = "/";
      } else {
        const data = await login({ email: form.email, password: form.password });
        localStorage.setItem("nomad_token", data.token);
        // full navigation to home so the app reloads with auth state
        window.location.href = "/";
      }
    } catch (err) {
      // try to read server message
      const message = (err as any)?.response?.data?.message || (err as any)?.message || "Auth failed";
      setError(String(message));
      setSubmitting(false);
    }
  };

  const input = "w-full rounded-xl border px-4 py-3 text-base";
  return (
    <div className="mx-auto flex min-h-screen w-full max-w-lg flex-col justify-center px-5 py-10">
      <div className="mb-6 text-center">
        <p className="text-3xl font-bold tracking-wide text-brand-700">NOMAD</p>
        <p className="mt-1 text-sm text-slate-400">Smart weekend travel &amp; assistance</p>
      </div>
      <div className="card space-y-5 p-6">
        <div className="grid grid-cols-2 gap-1 rounded-xl bg-slate-800/60 p-1" role="tablist">
          {(["login", "register"] as const).map((m) => (
            <button key={m} type="button" role="tab" aria-selected={mode === m} onClick={() => setMode(m)} className={`rounded-lg py-2 text-sm font-semibold transition-colors ${mode === m ? "bg-brand-600 text-white" : "text-slate-300 hover:text-white"}`}>
              {m === "login" ? "Log in" : "Sign up"}
            </button>
          ))}
        </div>

        <form className="grid gap-3 md:grid-cols-2" onSubmit={(e) => { e.preventDefault(); handleSubmit(); }}>
          {mode === "register" && (
            <input name="name" value={form.name} onChange={handleChange} placeholder="Name" autoComplete="name" className={`${input} md:col-span-2`} />
          )}
          <input name="email" type="email" value={form.email} onChange={handleChange} placeholder="Email" autoComplete="email" className={`${input} md:col-span-2`} />
          <input name="password" type="password" value={form.password} onChange={handleChange} placeholder="Password" autoComplete={mode === "login" ? "current-password" : "new-password"} className={`${input} md:col-span-2`} />
          {mode === "register" && (
            <>
              <select name="city" value={form.city} onChange={handleChange} className={input}>
                <option value="">Select city</option>
                <option value="Bengaluru">Bengaluru</option>
                <option value="Mumbai">Mumbai</option>
                <option value="Delhi">Delhi</option>
              </select>
              <input name="phoneNumber" value={form.phoneNumber} onChange={handleChange} placeholder="Phone (+91...)" autoComplete="tel" className={input} />
              <div className="space-y-2 md:col-span-2">
                <p className="text-xs font-semibold uppercase tracking-wide text-slate-500">Your location (for pickup and nearby places)</p>
                <UseLocationButton onSet={(c) => setForm((p) => ({ ...p, latitude: String(c.latitude), longitude: String(c.longitude) }))} />
                <MapPicker onSet={(c) => setForm((p) => ({ ...p, latitude: String(c.latitude), longitude: String(c.longitude) }))} />
                {form.latitude && <p className="text-xs text-green-300">Location set ({Number(form.latitude).toFixed(3)}, {Number(form.longitude).toFixed(3)})</p>}
              </div>
              <select name="interestType" value={form.interestType} onChange={handleChange} className={input}>
                {["FOOD", "CULTURE", "NATURE", "ADVENTURE", "SHOPPING", "NIGHTLIFE", "RELAXATION"].map((item) => (
                  <option key={item} value={item}>{item.charAt(0) + item.slice(1).toLowerCase()}</option>
                ))}
              </select>
              <select name="travelPreference" value={form.travelPreference} onChange={handleChange} className={input}>
                <option value="SOLO">Solo</option>
                <option value="GROUP">Group</option>
              </select>
            </>
          )}
          <button type="submit" className="btn btn-primary w-full py-3 md:col-span-2" disabled={submitting}>
            {submitting ? "Please wait…" : mode === "register" ? "Create account" : "Log in"}
          </button>
        </form>
        {submitting && <p className="text-center text-xs text-slate-500">The server can take up to a minute to wake up the first time.</p>}
        {error && <p role="alert" className="rounded-xl border border-red-500/20 bg-red-500/10 px-4 py-3 text-sm text-red-400">{error}</p>}
      </div>
    </div>
  );
}
