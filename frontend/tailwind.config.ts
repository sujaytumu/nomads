import type { Config } from "tailwindcss";

// Midnight theme: the original blue/black look. Tailwind's own slate scale is used as-is
// (950 = near-black page, 900 = surface, 100 = text) with the electric-blue brand scale.
const config: Config = {
  content: ["./src/**/*.{ts,tsx}"],
  theme: {
    extend: {
      fontFamily: {
        sans: ["var(--font-body)", "system-ui", "sans-serif"],
        display: ["var(--font-display)", "Georgia", "serif"],
      },
      boxShadow: {
        soft: "0 1px 2px rgba(0,0,0,.35), 0 10px 30px -8px rgba(0,0,0,.45)",
        lift: "0 2px 4px rgba(0,0,0,.4), 0 18px 40px -10px rgba(79,108,255,.35)",
        glow: "0 0 0 1px rgba(111,140,255,.35), 0 8px 28px -6px rgba(79,108,255,.55)",
      },
      keyframes: {
        "heart-pop": {
          "0%": { transform: "scale(1)" },
          "30%": { transform: "scale(1.45) rotate(-8deg)" },
          "60%": { transform: "scale(0.92) rotate(4deg)" },
          "100%": { transform: "scale(1)" },
        },
        "ripple-out": {
          "0%": { transform: "scale(0)", opacity: "0.45" },
          "100%": { transform: "scale(1)", opacity: "0" },
        },
        "spin-slow": { to: { transform: "rotate(360deg)" } },
        "pop-in": {
          "0%": { transform: "translateY(4px) scale(.96)", opacity: "0" },
          "100%": { transform: "none", opacity: "1" },
        },
      },
      animation: {
        "heart-pop": "heart-pop .45s cubic-bezier(.22,1,.36,1)",
        "pop-in": "pop-in .2s ease-out both",
      },
      colors: {
        // Greys follow the theme (dark by default, light when chosen) through CSS variables in globals.css.
        slate: {
          50: "rgb(var(--s-50) / <alpha-value>)",
          100: "rgb(var(--s-100) / <alpha-value>)",
          200: "rgb(var(--s-200) / <alpha-value>)",
          300: "rgb(var(--s-300) / <alpha-value>)",
          400: "rgb(var(--s-400) / <alpha-value>)",
          500: "rgb(var(--s-500) / <alpha-value>)",
          600: "rgb(var(--s-600) / <alpha-value>)",
          700: "rgb(var(--s-700) / <alpha-value>)",
          800: "rgb(var(--s-800) / <alpha-value>)",
          900: "rgb(var(--s-900) / <alpha-value>)",
          950: "rgb(var(--s-950) / <alpha-value>)",
        },
        emerald: { 300: "rgb(var(--c-emerald-300) / <alpha-value>)", 400: "rgb(var(--c-emerald-400) / <alpha-value>)" },
        rose: { 300: "rgb(var(--c-rose-300) / <alpha-value>)", 400: "rgb(var(--c-rose-400) / <alpha-value>)" },
        red: { 300: "rgb(var(--c-red-300) / <alpha-value>)", 400: "rgb(var(--c-red-400) / <alpha-value>)" },
        green: { 300: "rgb(var(--c-green-300) / <alpha-value>)", 400: "rgb(var(--c-green-400) / <alpha-value>)" },
        amber: { 300: "rgb(var(--c-amber-300) / <alpha-value>)" },
        yellow: { 300: "rgb(var(--c-yellow-300) / <alpha-value>)" },
        sky: { 300: "rgb(var(--c-sky-300) / <alpha-value>)" },
        brand: {
          50: "#f5f7ff",
          100: "#eef2ff",
          200: "rgb(var(--c-brand-200) / <alpha-value>)",
          300: "rgb(var(--c-brand-300) / <alpha-value>)",
          400: "rgb(var(--c-brand-400) / <alpha-value>)",
          500: "#6f8cff",
          600: "#4f6cff",
          700: "#3d55f0",
          800: "#2f41c2",
          900: "#26379a",
        },
        // Cool cyan secondary accent that sits well on dark blue.
        ocean: { 50: "#ecfeff", 100: "#cffafe", 300: "rgb(var(--c-ocean-300) / <alpha-value>)", 400: "#22d3ee", 500: "#06b6d4", 600: "#0891b2", 700: "#0e7490" },
      },
    },
  },
  plugins: [],
};

export default config;
