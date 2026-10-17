import type { Config } from "tailwindcss";

// "Daylight" theme. The old dark UI was written with the slate scale (950 = page,
// 900 = surface, 100 = text). Rather than touch every page, the scale is mirrored
// here so those same class names now give a warm light theme: slate-950 is the
// sand-coloured page, slate-900 white surfaces, slate-100 deep-navy ink.
const config: Config = {
  content: ["./src/**/*.{ts,tsx}"],
  theme: {
    extend: {
      fontFamily: {
        sans: ["var(--font-body)", "system-ui", "sans-serif"],
        display: ["var(--font-display)", "Georgia", "serif"],
      },
      boxShadow: {
        soft: "0 1px 2px rgba(18,38,58,.05), 0 10px 30px -8px rgba(18,38,58,.12)",
        lift: "0 2px 4px rgba(18,38,58,.06), 0 18px 40px -10px rgba(18,38,58,.22)",
      },
      colors: {
        slate: {
          50: "#0B1A2B",
          100: "#12263A",
          200: "#24364A",
          300: "#3C4C5C",
          400: "#5B6B7B",
          500: "#7A8794",
          600: "#C4BAA8",
          700: "#E3DCCD",
          800: "#F1ECE2",
          900: "#FFFFFF",
          950: "#FBF8F3",
        },
        // Sunset coral: calls to action and highlights.
        brand: {
          50: "#FFF4EF",
          100: "#FFE6DB",
          200: "#FFCDB8",
          300: "#B63A17",
          400: "#C2410C",
          500: "#F2653A",
          600: "#DE4A22",
          700: "#BF3B18",
          800: "#9A2F12",
          900: "#7A250F",
        },
        // Ocean teal: secondary accent.
        ocean: { 50: "#ECFAF8", 100: "#CFF1EC", 500: "#14A097", 600: "#0E8077", 700: "#0B6560" },
        // Text-on-tint shades must stay dark on a light page.
        green: { 300: "#15803D", 400: "#16A34A" },
        yellow: { 300: "#A16207", 400: "#CA8A04" },
        amber: { 300: "#B45309", 400: "#D97706" },
        red: { 300: "#B91C1C", 400: "#DC2626" },
        sky: { 300: "#0369A1", 400: "#0284C7" },
        blue: { 300: "#1D4ED8", 400: "#2563EB", 600: "#2563EB" },
      },
    },
  },
  plugins: [],
};

export default config;
