"use client";

import { fetchMe } from "@/lib/authApi";
import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";

type ProtectedPageProps = {
  children: React.ReactNode;
  requiredRole?: "ADMIN" | "USER";
};

export default function ProtectedPage({ children, requiredRole }: ProtectedPageProps) {
  const router = useRouter();
  const [allowed, setAllowed] = useState(false);

  useEffect(() => {
    const token = localStorage.getItem("nomad_token");
    if (!token) {
      router.replace("/auth");
      return;
    }

    fetchMe()
      .then((me) => {
        if (requiredRole && me.role !== requiredRole) {
          router.replace("/");
          return;
        }
        setAllowed(true);
      })
      .catch(() => router.replace("/auth"));
  }, [router, requiredRole]);

  if (!allowed) {
    return (
      <div className="section py-12">
        <div className="card p-6 text-slate-600">Checking access…</div>
      </div>
    );
  }

  return <>{children}</>;
}