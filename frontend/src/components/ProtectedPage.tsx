"use client";

import { useRouter } from "next/navigation";
import { useEffect } from "react";
import { useAuth } from "@/lib/auth-context";

type ProtectedPageProps = {
  children: React.ReactNode;
  requiredRole?: "ADMIN" | "USER";
};

/**
 * AppShell already verifies the login once (and signs you out on a 401), so this guard
 * no longer makes its own server call on every page. Normal pages show immediately;
 * only role-restricted pages (admin) wait until the role is known.
 */
export default function ProtectedPage({ children, requiredRole }: ProtectedPageProps) {
  const router = useRouter();
  const { state, user } = useAuth();
  const wrongRole = !!requiredRole && !!user && user.role !== requiredRole;

  useEffect(() => {
    if (state === "out") router.replace("/auth");
    else if (wrongRole) router.replace("/");
  }, [state, wrongRole, router]);

  if (state === "out" || wrongRole) return null;

  if (requiredRole && !user) {
    return (
      <div className="section py-12">
        <div className="card p-6 text-slate-300">Checking access…</div>
      </div>
    );
  }

  return <>{children}</>;
}
