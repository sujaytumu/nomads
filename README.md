# Nomad

A full-stack travel package booking platform built with Spring Boot (Java) for the backend and Next.js (React/TypeScript) for the frontend.

## Live deployment

- Frontend: https://nomads-frontend-sujays-projects-c5bb1f58.vercel.app/
- Backend API: https://nomads-backend-ea1a.onrender.com/


Hosted on render && vercel (Postgres + backend + frontend as separate services in one project).

---

## Features
- User authentication and registration
- Explore travel packages
- Enroll and pay for packages (Razorpay integration)
- View and manage your bookings
- Map/location features (OpenStreetMap via Leaflet, free/no signup)
- Admin and user roles

---

## Requirements

### Backend
- Java 17+
- Maven
- PostgreSQL (or compatible database)

### Frontend
- Node.js 18+
- npm or yarn

---

## Setup Instructions

### 1. Clone the repository
```sh
git clone https://github.com/sujaytumu/nomads.git
cd nomads
```

### 2. Backend Setup (Spring Boot)

All config is driven by environment variables (see `src/main/resources/application.properties` for the full list and defaults) — nothing sensitive is hardcoded in the repo. At minimum, set:

```sh
export DATABASE_URL=jdbc:postgresql://localhost:5432/nomad
export DATABASE_USERNAME=postgres
export DATABASE_PASSWORD=your_local_password
export JWT_SECRET=some_random_32+_char_string
export CORS_ALLOWED_ORIGINS=http://localhost:3000
```

Then build and run:
```sh
mvn clean install
mvn spring-boot:run
```
The backend starts on `http://localhost:8080` by default (`PORT` env var overrides this).

Optional env vars: `RAZORPAY_KEY_ID`, `RAZORPAY_KEY_SECRET`, `MAIL_*`, `TWILIO_*`, `NOMAD_DEV_PAYMENTS=true` (skips real Razorpay calls, useful for local dev).

### 3. Frontend Setup (Next.js)
```sh
cd frontend
npm install
```
Create `frontend/.env.local` (gitignored, never commit real keys here):
```
NEXT_PUBLIC_API_BASE_URL=http://localhost:8080
NEXT_PUBLIC_RAZORPAY_KEY_ID=your_razorpay_key
```
```sh
npm run dev
```
The frontend starts on `http://localhost:3000`.

---

## Deploying (Render && Vercel)


---

## Running Tests
- Backend: `mvn test`
- Frontend: `npm test` (if tests are present)

---

## Security notes
- Never commit real API keys, DB passwords, or JWT secrets — use environment variables everywhere, both locally (`.env.local` / exported vars, both gitignored) and in your deploy platform's variable settings.
- The `/actuator/env` endpoint is intentionally disabled — it would otherwise leak all resolved config, including secrets, over HTTP.

---

## License
MIT

---

## Contact
For any issues, open an issue on GitHub or contact the maintainer.
