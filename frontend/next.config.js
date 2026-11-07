// One id per build. The app compares it with the id the server reports, so it can tell
// when a newer version has been deployed and offer an "Update" button.
process.env.NEXT_PUBLIC_BUILD_ID =
  process.env.NEXT_PUBLIC_BUILD_ID ||
  process.env.VERCEL_GIT_COMMIT_SHA ||
  process.env.GITHUB_SHA ||
  String(Date.now());

/** @type {import('next').NextConfig} */
const nextConfig = {
  reactStrictMode: true,
  poweredByHeader: false,
  compress: true,
  images: {
    domains: [],
  },
  env: { NEXT_PUBLIC_BUILD_ID: process.env.NEXT_PUBLIC_BUILD_ID },
  async headers() {
    // Icons and pictures rarely change: let the browser keep them for a week, and show the old copy
    // while it fetches a new one in the background. The service worker must always be re-checked.
    const week = "public, max-age=604800, stale-while-revalidate=86400";
    return [
      { source: "/:file(icon-192|icon-512|icon-maskable-512|apple-touch-icon).png", headers: [{ key: "Cache-Control", value: week }] },
      { source: "/images/:path*", headers: [{ key: "Cache-Control", value: week }] },
      { source: "/sw.js", headers: [{ key: "Cache-Control", value: "no-cache" }] },
    ];
  },
};

module.exports = nextConfig;
