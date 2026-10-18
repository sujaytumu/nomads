"use client";

import { createContext, useContext } from "react";
import type { Me } from "@/lib/authApi";

export type AuthValue = { state: "loading" | "out" | "in"; user: Me | null };

/** Filled in once by AppShell, so pages don't each re-check the login with the server. */
export const AuthContext = createContext<AuthValue>({ state: "loading", user: null });
export const useAuth = () => useContext(AuthContext);
