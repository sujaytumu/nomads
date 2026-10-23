"use client";

import { forgotPassword, login, register, resetPassword } from "@/lib/authApi";
import { useEffect, useState } from "react";
import { warmUpBackend, withWakeRetry } from "@/lib/warmup";
import UseLocationButton from "@/components/UseLocationButton";
import dynamic from "next/dynamic";

const MapPicker = dynamic(() => import("@/components/MapPicker"), { ssr: false });

export default function AuthPage() {
  const [mode, setMode] = useState<"login" | "register" | "forgot" | "reset">("login");
  const [resetToken, setResetToken] = useState("");
  const [confirm, setConfirm] = useState("");
  const [showPw, setShowPw] = useState(false);
  const [info, setInfo] = useState<string | null>(null);
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
  const [seconds, setSeconds] = useState(0);

  useEffect(() => warmUpBackend(), []);
  useEffect(() => {
    if (!submitting) return setSeconds(0);
    const t = setInterval(() => setSeconds((s) => s + 1), 1000);
    return () => clearInterval(t);
  }, [submitting]);

  // Nav "Sign up" links arrive as /auth?mode=register; the emailed reset link as /auth?reset=<token>
  useEffect(() => {
    const q = new URLSearchParams(window.location.search);
    const t = q.get("reset");
    if (t) {
      setResetToken(t);
      setMode("reset");
      return;
    }
    // Already signed in: the login page has nothing to offer (replace, so Back doesn't return here).
    if (localStorage.getItem("nomad_token")) {
      window.location.replace("/");
      return;
    }
    if (q.get("mode") === "register") setMode("register");
  }, []);

  const goMode = (m: "login" | "register" | "forgot") => {
    setMode(m);
    setError(null);
    setInfo(null);
    setShowPw(false);
  };

  // After a successful login: let the phone/browser offer to save the password, then open the app.
  const finishLogin = (token: string) => {
    localStorage.setItem("nomad_token", token);
    try { (window as any).NomadNative?.loginSuccess?.(); } catch { /* not in the Android app */ }
    try {
      const PC = (window as any).PasswordCredential;
      if (PC && navigator.credentials?.store) {
        navigator.credentials.store(new PC({ id: form.email, password: form.password, name: form.name || form.email }));
      }
    } catch { /* browser doesn't support it */ }
    // replace (not assign) so the Back button never returns to the login page
    setTimeout(() => window.location.replace("/"), 300);
  };

  const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) => {
    const { name, value } = e.target;
    setForm((prev) => ({ ...prev, [name]: value }));
  };

  const handleSubmit = async () => {
    if (submitting) return;
    setSubmitting(true);
    setError(null);
    try {
      if (mode === "forgot") {
        await withWakeRetry(() => forgotPassword(form.email.trim()));
        setInfo("If that email is registered, a reset link is on its way. Check your inbox and spam folder.");
        setSubmitting(false);
        return;
      }
      if (mode === "reset") {
        if (form.password.length < 8) throw new Error("Password must be at least 8 characters");
        if (form.password !== confirm) throw new Error("Passwords don't match");
        await withWakeRetry(() => resetPassword(resetToken, form.password));
        window.history.replaceState(null, "", "/auth");
        setForm((p) => ({ ...p, password: "" }));
        setConfirm("");
        setResetToken("");
        setMode("login");
        setInfo("Password updated. Log in with your new password.");
        setSubmitting(false);
        return;
      }
      if (mode === "register") {
        const data = await withWakeRetry(() => register({
          name: form.name,
          email: form.email,
          phoneNumber: form.phoneNumber,
          password: form.password,
          city: form.city,
          latitude: Number(form.latitude),
          longitude: Number(form.longitude),
          interestType: form.interestType,
          travelPreference: form.travelPreference,
        }));
        finishLogin(data.token);
      } else {
        const data = await withWakeRetry(() => login({ email: form.email, password: form.password }));
        finishLogin(data.token);
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
      <div className="relative mb-6 overflow-hidden rounded-[2rem] bg-gradient-to-br from-[#1B2559] via-[#26379a] to-[#4f6cff] border border-brand-500/20 p-7 text-white shadow-lift">
        <div aria-hidden="true" className="absolute -right-8 -top-10 h-40 w-40 rounded-full bg-white/15" />
        <p className="relative text-xs font-semibold uppercase tracking-[0.2em] text-white/85">Nomad</p>
        <h1 className="relative mt-1 font-display text-3xl font-semibold leading-tight">Your weekend,<br />planned in minutes.</h1>
      </div>
      <div className="card space-y-5 p-6">
        {(mode === "login" || mode === "register") ? (
          <div className="grid grid-cols-2 gap-1 rounded-full bg-slate-800 p-1" role="tablist">
            {(["login", "register"] as const).map((m) => (
              <button key={m} type="button" role="tab" aria-selected={mode === m} onClick={() => goMode(m)} className={`rounded-full py-2 text-sm font-semibold transition-colors ${mode === m ? "bg-brand-600 text-white shadow" : "text-slate-300 hover:text-slate-100"}`}>
                {m === "login" ? "Log in" : "Sign up"}
              </button>
            ))}
          </div>
        ) : (
          <div className="space-y-1">
            <button type="button" onClick={() => goMode("login")} className="text-sm font-semibold text-brand-500">← Back to log in</button>
            <h2 className="font-display text-xl font-semibold">{mode === "forgot" ? "Forgot your password?" : "Choose a new password"}</h2>
            <p className="text-sm text-slate-400">
              {mode === "forgot" ? "Enter your email. We'll send you a link to set a new one." : "At least 8 characters."}
            </p>
          </div>
        )}

        <form id="auth-form" method="post" action="/auth" className="grid gap-3 md:grid-cols-2" onSubmit={(e) => { e.preventDefault(); handleSubmit(); }}>
          {mode === "register" && (
            <input name="name" value={form.name} onChange={handleChange} placeholder="Name" autoComplete="name" className={`${input} md:col-span-2`} />
          )}
          {mode !== "reset" && (
            <input id="email" name="email" type="email" value={form.email} onChange={handleChange} placeholder="Email" autoComplete="username" inputMode="email" required className={`${input} md:col-span-2`} />
          )}
          {mode !== "forgot" && (
            <div className="relative md:col-span-2">
              <input id="password" name="password" type={showPw ? "text" : "password"} value={form.password} onChange={handleChange} placeholder={mode === "reset" ? "New password" : "Password"} autoComplete={mode === "login" ? "current-password" : "new-password"} required className={`${input} pr-16`} />
              <button type="button" onClick={() => setShowPw((v) => !v)} aria-label={showPw ? "Hide password" : "Show password"} className="absolute inset-y-0 right-3 text-xs font-semibold text-slate-400">{showPw ? "Hide" : "Show"}</button>
            </div>
          )}
          {mode === "reset" && (
            <input name="confirm" type={showPw ? "text" : "password"} value={confirm} onChange={(e) => setConfirm(e.target.value)} placeholder="Confirm new password" autoComplete="new-password" required className={`${input} md:col-span-2`} />
          )}
          {mode === "login" && (
            <button type="button" onClick={() => goMode("forgot")} className="justify-self-start text-sm font-semibold text-brand-500 md:col-span-2">Forgot password?</button>
          )}
          {mode === "register" && (
            <>
              <select name="city" value={form.city} onChange={handleChange} className={input}>
                <option value="">Select city</option>
                <option value="Bengaluru">Bengaluru</option>
                <option value="Mumbai">Mumbai</option>
                <option value="Delhi">Delhi</option>
                <option value="Hyderabad">Hyderabad</option>
                <option value="Vishakapatnam">Vishakapatnam</option>
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
            {submitting ? "Please wait…" : mode === "register" ? "Create account" : mode === "forgot" ? "Send reset link" : mode === "reset" ? "Save new password" : "Log in"}
          </button>
        </form>
        {submitting && (
          <p className="text-center text-xs text-slate-400" role="status">
            {seconds < 5 ? "Signing you in…" : `Waking up the server… ${seconds}s (first sign-in after a quiet spell can take about a minute)`}
          </p>
        )}
        {info && <p role="status" className="rounded-xl border border-green-500/20 bg-green-500/10 px-4 py-3 text-sm text-green-300">{info}</p>}
        {error && <p role="alert" className="rounded-xl border border-red-500/20 bg-red-500/10 px-4 py-3 text-sm text-red-400">{error}</p>}
      </div>
    </div>
  );
}
