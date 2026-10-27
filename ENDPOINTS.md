# Nomad API Endpoints

This file lists all REST endpoints implemented in the backend with example requests.

- **AdminController** — base `/api/admin`
  - `POST /api/admin/places`
  - `GET  /api/admin/places`
  - `DELETE /api/admin/places/{id}`
  - `POST /api/admin/vehicles`
  - `GET  /api/admin/vehicles`
  - `DELETE /api/admin/vehicles/{id}`

- **PackageController** (additions) — base `/api/packages`
  - `GET /api/packages/search?q=&minPrice=&maxPrice=&sort=price_asc|price_desc|name`

- **WishlistController** — base `/api/wishlist` (auth required)
  - `GET    /api/wishlist` (saved packages)
  - `GET    /api/wishlist/ids`
  - `POST   /api/wishlist/{packageId}` (idempotent)
  - `DELETE /api/wishlist/{packageId}` (idempotent)

- **PlaceSearchController / PlaceDetailController** — base `/api/places` (public)
  - `GET /api/places/search?q=&city=&category=&minRating=&sort=relevance|rating|name|distance&lat=&lon=&page=&size=`
    returns `items`, paging totals and `facets` (`categories`, `cities` with counts; each facet ignores its own filter)
  - `GET /api/places/{id}`

- **RecommendationController** — base `/api/recommendations` (auth required)
  - `GET /api/recommendations/places?limit=10&interest=&maxKm=` (scored on taste, rating, closeness and novelty, with variety; each item lists its `reasons`)

- **StatsController** — base `/api/me` (auth required)
  - `GET /api/me/stats` (trips, places, cities, km, spend, XP level, badges with progress, next badge)

- **InboxController** — base `/api/inbox` (auth required, scoped to the caller)
  - `GET  /api/inbox?unreadOnly=false&page=0&size=20`
  - `GET  /api/inbox/unread-count`
  - `POST /api/inbox/{id}/read`
  - `POST /api/inbox/read-all`
  - Items are added automatically when a booking is confirmed, a payment fails, or a trip is cancelled.

- **UserController** — base `/api/users`
  - `POST /api/users` (create user)
  - `GET  /api/users/{id}`
  - `PUT  /api/users/{id}`

- **AuthController** — base `/api/auth`
  - `POST /api/auth/register`
  - `POST /api/auth/login`
  - `GET  /api/auth/me`

- **TripController** — base `/api/trips`
  - `POST /api/trips/create`
  - `GET  /api/trips/{id}`
  - `GET  /api/trips/user/{userId}`
  - `GET  /api/trips/me` — the signed-in user's trips
  - `GET  /api/trips/{id}/itinerary/options` — places that can be chosen for the trip, current picks marked, allowed range and pricing rule
  - `PUT  /api/trips/{id}/itinerary` — body `{"placeIds":[1,2,3]}`; rebuilds the day-by-day plan and price. Only while the trip is `PLANNED` (before payment starts)
  - `GET  /api/trips/{id}/itinerary.pdf` — the itinerary as a PDF download (trips with a day-by-day itinerary)

- **TripRouteController** — base `/api/routes`
  - `GET /api/routes/{tripRequestId}`

- **TripGroupController** — base `/api/groups`
  - `GET /api/groups/{groupId}`
  - `GET /api/groups/{groupId}/members`

- **ShareController** — base `/api/share`
  - `GET /api/share/{token}`

- **TravelController** — base `/api/travel`
  - `POST /api/travel/assign`
  - `GET  /api/travel/{tripId}`
  - `PUT  /api/travel/{tripId}`
  - `PUT  /api/travel/{tripId}/confirm`

- **PlaceController** — base `/api/places`
  - `GET /api/places/nearby`

- **ReviewController** — base `/api/reviews`
  - `POST /api/reviews`
  - `GET  /api/reviews/{tripId}`

- **PaymentController** — base `/api/payment`
  - `POST /api/payment/create`
  - `POST /api/payment/verify`
  - `POST /api/payment/webhook`


## How to run smoke tests locally

From repository root:

```bash
# Start backend (uses application.properties defaults unless you override env vars)
mvn -DskipTests spring-boot:run

# Example quick checks (replace placeholders):
curl -i http://localhost:8081/actuator/health
curl -i http://localhost:8080/api/auth/me
curl -i http://localhost:8080/api/trips/1
```


Generated on: 2026-01-27

## More endpoints

| Method | Path | Who | What |
|---|---|---|---|
| GET | `/api/places/{id}` | public | One place |
| GET/POST | `/api/places/{placeId}/ratings` | GET public, POST signed in | Blended rating summary and reviews; add or change your rating |
| GET/POST | `/api/trips/{tripId}/notes` | trip members | Trip journal notes |
| GET | `/api/trips/{tripId}/notes/export` | trip members | Journal as a text export |
| GET | `/api/admin/analytics?days=30` | admin | Totals, funnel, daily series, top cities, interest mix |
| GET | `/api/me/saved-places` | signed in | Saved places, newest first |
| GET | `/api/me/saved-places/ids` | signed in | Ids only, for filling hearts |
| POST/DELETE | `/api/me/saved-places/{placeId}` | signed in | Save or unsave (idempotent) |

Trip reminders run on a schedule (`nomad.reminders.enabled`) and post to the inbox; there is no endpoint for them.
