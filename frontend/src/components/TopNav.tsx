"use client";

import { fetchMe } from "@/lib/authApi";
import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";

type User = {
  id: number;
  email: string;
  role?: string;
};

export default function TopNav() {
  const [user, setUser] = useState<User | null>(null);
  const router = useRouter();

  useEffect(() => {
    const token = typeof window !== "undefined" ? localStorage.getItem("nomad_token") : null;
    if (!token) {
      setUser(null);
      return;
    }

    fetchMe()
      .then((data) => setUser({ id: data.id, email: data.email, role: data.role }))
      .catch(() => setUser(null));
  }, []);

  const logout = () => {
    localStorage.removeItem("nomad_token");
    setUser(null);
    // redirect to home and reload so app state resets
    router.push("/");
    window.location.reload();
  };

  return (
    <nav className="flex gap-4 text-sm font-semibold text-slate-600 items-center">
      <a href="/" className="hover:text-brand-700">Home</a>
      <a href="/trip-planner" className="hover:text-brand-700">Planner</a>
      <a href="/map" className="hover:text-brand-700">Map</a>
      <a href="/route-view" className="hover:text-brand-700">Route</a>
      <a href="/trip-summary" className="hover:text-brand-700">Summary</a>
      <a href="/payment" className="hover:text-brand-700">Payment</a>
      <a href="/auth" className="hover:text-brand-700">Auth</a>
      <a href="/profile" className="hover:text-brand-700">Profile</a>
      {user?.role === "ADMIN" && (
        <>
          <a href="/admin" className="hover:text-brand-700">Admin</a>
          <a href="/group-status" className="hover:text-brand-700">Groups</a>
        </>
      )}
      {user && (
        <button className="btn-outline" onClick={logout}>Logout</button>
      )}
    </nav>
  );
}
