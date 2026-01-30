# Nomad

A full-stack travel package booking platform built with Spring Boot (Java) for the backend and Next.js (React/TypeScript) for the frontend.

---

## Features
- User authentication and registration
- Explore travel packages
- Enroll and pay for packages (Razorpay integration)
- View and manage your bookings
- Map/location features (Mapbox)
- Admin and user roles

---

## Requirements

### Backend
- Java 17+
- Maven
- PostgreSQL (or compatible database)
- (Optional) Docker (for DB or deployment)

### Frontend
- Node.js 18+
- npm or yarn

---

## Setup Instructions

### 1. Clone the repository
```sh
git clone <your-repo-url>
cd nomad
```

### 2. Backend Setup (Spring Boot)
1. Configure your database in `src/main/resources/application.properties`:
   - Set DB URL, username, password, etc.
2. Build and run the backend:
```sh
mvn clean install
mvn spring-boot:run
```
- The backend will start on `http://localhost:8080` by default.

### 3. Frontend Setup (Next.js)
1. Go to the frontend directory:
```sh
cd frontend
```
2. Install dependencies:
```sh
npm install
```
3. Create a `.env.local` file in `frontend/` with the following (example):
```
NEXT_PUBLIC_API_BASE_URL=http://localhost:8080
NEXT_PUBLIC_RAZORPAY_KEY_ID=your_razorpay_key
NEXT_PUBLIC_MAPBOX_TOKEN=your_mapbox_token
```
4. Start the frontend:
```sh
npm run dev
```
- The frontend will start on `http://localhost:3001` (or `3000`).

---

## Running Tests
- Backend: `mvn test`
- Frontend: `npm test` (if tests are present)

---

## Notes
- Make sure PostgreSQL is running and accessible.
- For Razorpay/Mapbox, get your API keys and add them to `.env.local`.
- For production, set up environment variables and secure credentials.

---

## License
MIT

---

## Contact
For any issues, open an issue on GitHub or contact the maintainer.
