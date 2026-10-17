// Tiny fake backend so the real frontend build can be screenshotted and clicked through in CI.
const http = require("http"), fs = require("fs");
const plans = (n) => Array.from({ length: n }, (_, i) => ({ dayNumber: 1 + (i >= 2 ? 1 : 0), placeId: i + 1, placeName: "Place " + (i + 1), startTime: "09:00:00", endTime: "11:00:00", distanceFromPrevious: 2.5 }));
const t = (id, status, city, cost, p) => ({ tripRequestId: id, city, status, estimatedCost: cost, createdAt: "2026-10-1" + id + "T10:00:00", plans: p, weekendType: "TWO_DAY" });
const trips = [
  t(1, "PLANNED", "Bengaluru", 1800, plans(3)),
  t(2, "PAYMENT_PENDING", "Mysuru", 1300, plans(2)),
  t(3, "CONFIRMED", "Bengaluru", 2300, plans(4)),
  t(4, "CONFIRMED", "Goa", 4999, []),
  t(5, "CANCELLED", "Hampi", 1000, plans(2)),
];
const pdf = process.env.SAMPLE_PDF && fs.existsSync(process.env.SAMPLE_PDF) ? fs.readFileSync(process.env.SAMPLE_PDF) : Buffer.from("%PDF-1.4\n");
const reviews = [{ id: 1, rating: 4, comment: "Lovely weekend!" }];
const geo = JSON.stringify({ type: "FeatureCollection", features: [{ type: "Feature", properties: {}, geometry: { type: "LineString", coordinates: [[77.59, 12.97], [77.6, 12.98], [77.62, 12.99]] } }] });
let travel = {};
http.createServer((req, res) => {
  res.setHeader("Access-Control-Allow-Origin", "*");
  res.setHeader("Access-Control-Allow-Headers", "*");
  res.setHeader("Access-Control-Allow-Methods", "GET,POST,PUT,PATCH,DELETE,OPTIONS");
  if (req.method === "OPTIONS") { res.writeHead(204); return res.end(); }
  const u = new URL(req.url, "http://x"); const p = u.pathname;
  const json = (o, c = 200) => { res.writeHead(c, { "Content-Type": "application/json" }); res.end(JSON.stringify(o)); };
  let body = ""; req.on("data", (d) => (body += d)); req.on("end", () => {
    let m;
    if (p === "/api/auth/me") return json({ id: 7, email: "asha@example.com", name: "Asha Rao", role: process.env.ROLE || "USER", city: "Bengaluru", latitude: 12.97, longitude: 77.59, interestType: "FOOD" });
    if (p === "/api/places/nearby") return json([["Toit Brewpub","FOOD",4.6],["Lalbagh Garden","NATURE",4.7],["Bangalore Palace","CULTURE",4.4],["Church Street","SHOPPING",4.2],["Cubbon Park","NATURE",4.5],["VV Puram Food Street","FOOD",4.8]].map((x,i)=>({ id: i+1, name: x[0], city: "Bengaluru", category: x[1], rating: x[2], distanceKm: 2+i, imageUrl: null })));
    if (p === "/api/packages/homepage" || p === "/api/packages/all" || p === "/api/packages/search") return json([["Coorg Coffee Escape",4999],["Goa Beach Weekend",5999],["Hampi Heritage Trail",3999]].map((x,i)=>({ id: i+1, name: x[0], price: x[1], shortDescription: "Two days of slow mornings, local food and great views.", imageUrl: null })));
    if (p === "/api/trips/me") return json(trips);
    if ((m = p.match(/^\/api\/trips\/(\d+)\/itinerary\.pdf$/))) { res.writeHead(200, { "Content-Type": "application/pdf" }); return res.end(pdf); }
    if ((m = p.match(/^\/api\/trips\/(\d+)\/itinerary\/email$/))) return json({ sent: false });
    if (p.match(/^\/api\/trips\/\d+\/weather$/)) return json({ city: "Bengaluru", days: [{ date: "2026-10-24", maxTempC: 28, minTempC: 19, precipitationChance: 30, description: "Partly cloudy" }, { date: "2026-10-25", maxTempC: 27, minTempC: 19, precipitationChance: 60, description: "Light rain" }] });
    if ((m = p.match(/^\/api\/trips\/(\d+)$/))) { const x = trips.find((q) => q.tripRequestId == m[1]); return x ? json(x) : json({ message: "Trip not found" }, 404); }
    if ((m = p.match(/^\/api\/travel\/(\d+)$/)) && req.method === "GET") return travel[m[1]] ? json(travel[m[1]]) : json({ message: "none" }, 404);
    if (p === "/api/travel/assign") { const id = JSON.parse(body).tripRequestId; travel[id] = { pickupLocation: "Majestic Bus Stand", vehicleType: "SEDAN", vehicleNumber: "KA01 AB 1234", driverName: "Ravi", status: "ASSIGNED" }; return json(travel[id]); }
    if ((m = p.match(/^\/api\/travel\/(\d+)\/confirm$/))) { travel[m[1]].status = "CONFIRMED"; travel[m[1]].pickupTime = JSON.parse(body).pickupTime; return json(travel[m[1]]); }
    if ((m = p.match(/^\/api\/reviews\/(\d+)$/))) return json(reviews);
    if (p === "/api/reviews") { reviews.push({ id: 2, ...JSON.parse(body) }); return json({}); }
    if ((m = p.match(/^\/api\/routes\//))) return json({ geoJson: geo });
    if (p === "/api/payment/create") return json({ razorpayOrderId: "dev_order_1", amount: 1800, paymentId: 1 });
    if (p === "/api/payment/verify") { trips[0].status = "CONFIRMED"; return json({ paymentStatus: "CAPTURED" }); }
    json([]);
  });
}).listen(8081);
