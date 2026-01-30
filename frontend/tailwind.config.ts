import type { Config } from "tailwindcss";

const config: Config = {
  content: ["./src/**/*.{ts,tsx}"] ,
  theme: {
    extend: {
      colors: {
        brand: {
          50: "#f5f7ff",
          100: "#eef2ff",
          200: "#dfe7ff",
          300: "#c1d0ff",
          400: "#9bb1ff",
          500: "#6f8cff",
          600: "#4f6cff",
          700: "#3d55f0",
          800: "#2f41c2",
          900: "#26379a"
        }
      }
    }
  },
  plugins: []
};

export default config;
