# Nomad API Endpoints

This file lists all REST endpoints implemented in the backend with example requests.

- **AdminController** — base `/api/admin`
  - `POST /api/admin/places`
  - `GET  /api/admin/places`
  - `DELETE /api/admin/places/{id}`
  - `POST /api/admin/vehicles`
  - `GET  /api/admin/vehicles`
  - `DELETE /api/admin/vehicles/{id}`

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
