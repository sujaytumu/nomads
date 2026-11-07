// Minimal service worker: makes NOMAD installable and shows a friendly page when offline.
// It never caches API calls or pages (trip and payment data must always be fresh).
// The app's own scripts, styles and fonts under /_next/static/ have a new file name on every release, so they
// are kept on the device and open instantly the next time.
const CACHE = "nomad-shell-v5";
const STATIC = "nomad-static-v1";
const MAX_STATIC = 160;
const OFFLINE = "/offline.html";
self.addEventListener("install", (e) => {
  e.waitUntil(caches.open(CACHE).then((c) => c.addAll([OFFLINE, "/icon-192.png"])));
  self.skipWaiting();
});
self.addEventListener("activate", (e) => {
  e.waitUntil(
    caches.keys().then((keys) => Promise.all(keys.filter((k) => k !== CACHE && k !== STATIC).map((k) => caches.delete(k))))
      .then(() => self.clients.claim())
  );
});
async function remember(req, res) {
  const cache = await caches.open(STATIC);
  await cache.put(req, res);
  const keys = await cache.keys();
  if (keys.length > MAX_STATIC) await Promise.all(keys.slice(0, keys.length - MAX_STATIC + 30).map((k) => cache.delete(k)));
}

self.addEventListener("fetch", (e) => {
  const req = e.request;
  if (req.mode === "navigate") {
    e.respondWith(fetch(req).catch(() => caches.match(OFFLINE)));
    return;
  }
  if (req.method !== "GET") return;
  const url = new URL(req.url);
  if (url.origin === self.location.origin && url.pathname.startsWith("/_next/static/")) {
    e.respondWith(
      caches.match(req).then((hit) => {
        if (hit) return hit;
        return fetch(req).then((res) => {
          if (res.ok) e.waitUntil(remember(req, res.clone()));
          return res;
        });
      })
    );
  }
});

// Tapping a reminder opens (or focuses) the app on the right page.
self.addEventListener("notificationclick", (e) => {
  e.notification.close();
  const url = (e.notification.data && e.notification.data.url) || "/";
  e.waitUntil(
    self.clients.matchAll({ type: "window", includeUncontrolled: true }).then((list) => {
      for (const c of list) {
        if ("focus" in c) {
          c.navigate(url).catch(() => undefined);
          return c.focus();
        }
      }
      return self.clients.openWindow(url);
    })
  );
});

// A notification sent by the server while the app is closed.
self.addEventListener("push", (e) => {
  let d = {};
  try { d = e.data ? e.data.json() : {}; } catch (_) { d = { body: e.data ? e.data.text() : "" }; }
  const title = d.title || "NOMAD";
  e.waitUntil(
    self.registration.showNotification(title, {
      body: d.body || "",
      icon: "/icon-192.png",
      tag: d.url ? title + "|" + d.url : title,
      data: { url: d.url || "/" },
    })
  );
});
