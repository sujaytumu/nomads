import axios from "axios";

export const api = axios.create({
  baseURL: process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080",
  // Render's free tier can take 30-50s to wake from a cold start after being
  // idle. 60s gives real cold starts room to finish, while still failing
  // with a clear, retryable error if something is genuinely stuck, instead
  // of a spinner that never resolves (axios has no timeout by default).
  timeout: 60000,
});

api.interceptors.request.use((config) => {
  if (typeof window !== "undefined") {
    const token = localStorage.getItem("nomad_token");
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
  }
  return config;
});
