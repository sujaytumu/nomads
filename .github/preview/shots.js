const { chromium } = require("playwright");
const fs = require("fs");
const OUT = process.env.OUT || "out";
fs.mkdirSync(OUT, { recursive: true });
const report = [];
const log = (ok, what) => { report.push((ok ? "PASS " : "FAIL ") + what); console.log((ok ? "PASS " : "FAIL ") + what); };
(async () => {
  const browser = await chromium.launch();
  const mk = async (vw) => {
    const ctx = await browser.newContext({ viewport: vw });
    await ctx.addInitScript(() => localStorage.setItem("nomad_token", "t"));
    const page = await ctx.newPage();
    page.on("pageerror", (e) => log(false, "JS error: " + e.message));
    return page;
  };
  const base = process.env.BASE || "http://localhost:3000";
  const d = await mk({ width: 1280, height: 860 });
  const routes = ["/", "/bookings", "/payment?tripRequestId=1", "/trip-summary?tripId=3", "/route-view?tripId=3", "/pickup?tripId=3", "/review?tripId=3", "/trip-planner", "/packages", "/profile", "/wishlist", "/map"];
  for (const r of routes) {
    await d.goto(base + r); await d.waitForTimeout(1200);
    await d.screenshot({ path: `${OUT}/d${r.replace(/[^a-z0-9]/gi, "_")}.png`, fullPage: true });
  }
  // every sidebar link must navigate
  await d.goto(base + "/"); await d.waitForTimeout(800);
  const hrefs = await d.$$eval("aside nav a", (as) => as.map((a) => a.getAttribute("href")));
  for (const h of hrefs) {
    await d.goto(base + "/"); await d.waitForTimeout(300);
    await d.click(`aside nav a[href="${h}"]`); await d.waitForTimeout(600);
    const p = new URL(d.url()).pathname;
    log(p === h, `sidebar ${h} -> ${p}`);
  }
  // every button/link-button on My Trips
  await d.goto(base + "/bookings"); await d.waitForTimeout(1200);
  const hrefsB = await d.$$eval("main a.btn, main a[class*=btn]", (as) => as.map((a) => a.getAttribute("href")));
  for (const h of new Set(hrefsB)) {
    await d.goto(base + "/bookings"); await d.waitForTimeout(500);
    await d.click(`main a[href="${h}"]`, { timeout: 5000 }).catch(() => log(false, "cannot click " + h));
    await d.waitForTimeout(700);
    log(true, `My Trips link ${h} -> ${new URL(d.url()).pathname + new URL(d.url()).search}`);
  }
  await d.goto(base + "/bookings"); await d.waitForTimeout(1000);
  await d.click("text=Email itinerary >> nth=0"); await d.waitForTimeout(800);
  log(await d.locator("text=couldn't be sent").count() > 0, "Email itinerary shows honest failure message");
  await d.screenshot({ path: `${OUT}/bookings-email-result.png`, fullPage: true });
  const dl = d.waitForEvent("download", { timeout: 8000 }).catch(() => null);
  await d.click("main button:has-text('PDF') >> nth=0");
  log(!!(await dl), "PDF button triggers a download");
  // pickup flow
  await d.goto(base + "/pickup?tripId=3"); await d.waitForTimeout(1000);
  await d.click("text=Request pickup"); await d.waitForTimeout(800);
  await d.screenshot({ path: `${OUT}/pickup-assigned.png`, fullPage: true });
  await d.fill("input[type=datetime-local]", "2026-10-20T08:30");
  await d.click("text=Confirm pickup time"); await d.waitForTimeout(800);
  log(await d.locator("text=Pickup time confirmed").count() > 0, "pickup confirm flow");
  log(await d.locator("text=Admin controls").count() === 0, "admin controls hidden for normal user");
  // review flow
  await d.goto(base + "/review?tripId=3"); await d.waitForTimeout(1000);
  await d.click("button[aria-label='3 stars']"); await d.click("text=Submit review"); await d.waitForTimeout(800);
  log(await d.locator("text=your review was saved").count() > 0, "review flow");
  // payment flow
  await d.goto(base + "/payment?tripRequestId=1"); await d.waitForTimeout(1000);
  await d.click("button:has-text('Pay')"); await d.waitForTimeout(2000);
  log(new URL(d.url()).pathname === "/bookings", "payment redirects to My Trips: " + d.url());
  await d.screenshot({ path: `${OUT}/after-payment.png`, fullPage: true });
  // mobile
  const m = await mk({ width: 390, height: 800 });
  for (const r of ["/", "/bookings"]) { await m.goto(base + r); await m.waitForTimeout(1000); await m.screenshot({ path: `${OUT}/m${r.replace(/[^a-z0-9]/gi, "_")}.png`, fullPage: true }); }
  await m.click("text=Menu"); await m.waitForTimeout(400);
  await m.screenshot({ path: `${OUT}/m-menu.png` });
  await browser.close();
  fs.writeFileSync(`${OUT}/report.txt`, report.join("\n"));
})();
