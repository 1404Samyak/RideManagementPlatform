# Campus Ride Management Platform

A full-stack ride management web app for IIT Roorkee-style campus e-rickshaw coordination. Passengers can request rides, drivers can manage availability and ride requests, and both sides receive real-time ride updates through WebSockets.

## Technology Stack

- Backend: Java 21, Spring Boot, Spring Security, Spring Data JPA, WebSocket/STOMP
- Database: PostgreSQL
- Frontend: React, TypeScript, Vite, Leaflet, OpenStreetMap
- UI: CSS modules-style global styling, Lucide icons, Recharts

## Mandatory Features Implemented

- Passenger and driver registration/login
- JWT authentication and role-based backend access
- Passenger profile management
- Driver vehicle and verification information
- Driver online/offline/busy availability
- Available driver listing for passengers
- Ride request creation with pickup and destination
- Driver incoming request list
- Accept/reject ride workflow
- Database-locked ride acceptance so only one driver can be assigned
- Ride lifecycle: `REQUESTED`, `ACCEPTED`, `IN_PROGRESS`, `COMPLETED`, `CANCELLED`
- Real-time driver availability, ride request, ride status, and assignment updates
- Driver dashboard with completed rides, active rides, history, ratings, and chart
- Passenger ratings and written feedback for completed rides
- Average driver rating and feedback history

## Optional Features Implemented

- Live map integration with Leaflet and OpenStreetMap
- Campus pickup/destination coordinates seeded into PostgreSQL
- Passenger map with pickup, destination, and assigned driver marker
- Driver map with pickup and destination for active/incoming rides
- Driver GPS tracking through the browser Geolocation API
- Real-time driver location updates through WebSocket events

## Project Structure

```text
RideManagementPlatform/
  backend/    Spring Boot API and WebSocket server
  frontend/   React Vite client
  README.md
```

## Prerequisites

Teammates need these installed locally:

- Java 21 JDK
- Node.js and npm

For local-only setup:

- PostgreSQL server

For team shared-database setup:

- A hosted PostgreSQL database URL, username, and password

Recommended but optional:

- IntelliJ IDEA for backend development
- pgAdmin for viewing PostgreSQL tables visually

IntelliJ is not strictly required to run the app because the backend includes the Maven wrapper:

```bash
cd backend
./mvnw spring-boot:run
```

pgAdmin is also not required to run the app. It is only a database GUI. The required database dependency is either a local PostgreSQL server or a shared hosted PostgreSQL database.

## Database Setup

PostgreSQL should be running locally on port `5432`.

The backend is configured for:

```text
Database: ride_management_db
Username: ride_user
Password: ride_password
```

If needed, create them with:

```sql
CREATE ROLE ride_user WITH LOGIN PASSWORD 'ride_password';
CREATE DATABASE ride_management_db OWNER ride_user;
GRANT ALL PRIVILEGES ON DATABASE ride_management_db TO ride_user;
```

On macOS with Homebrew, PostgreSQL can be started with:

```bash
brew services start postgresql@18
```

Then create the database/user using `psql` or pgAdmin Query Tool.

Tables are created/updated automatically by Spring Boot JPA using:

```properties
spring.jpa.hibernate.ddl-auto=update
```

## Shared Database Setup

Use this setup when all teammates should see the same registered users, drivers, rides, ratings, and campus location edits.

Everyone still runs the backend and frontend locally, but every backend connects to the same hosted PostgreSQL database:

```text
Teammate A backend -> shared hosted PostgreSQL
Teammate B backend -> shared hosted PostgreSQL
Teammate C backend -> shared hosted PostgreSQL
```

Do not commit database passwords or hosted URLs to GitHub. Share them privately with teammates.

The backend reads these values from `backend/.env` if that file exists:

```text
DATABASE_URL
DATABASE_USERNAME
DATABASE_PASSWORD
```

For a Render PostgreSQL database, use the external connection details for local laptops, not the internal hostname. The JDBC URL should look like:

```text
jdbc:postgresql://<external-host>:5432/ride_management_db?sslmode=require
```

Create the local env file once:

```bash
cd backend
cp .env.example .env
```

Then edit `backend/.env`:

```properties
DATABASE_URL=jdbc:postgresql://<external-host>:5432/ride_management_db?sslmode=require
DATABASE_USERNAME=ride_user
DATABASE_PASSWORD=<shared-database-password>
JWT_SECRET=<shared-jwt-secret>
```

After that, start the backend normally:

Mac/Linux:

```bash
./mvnw spring-boot:run
```

Windows:

```bat
mvnw.cmd spring-boot:run
```

`backend/.env` is ignored by Git, so do not commit it. Share the real values privately with teammates. When these values are set, the backend ignores the local database defaults and uses the shared database. If the shared database is empty, Spring Boot creates the tables automatically on startup.

## pgAdmin Setup

pgAdmin is optional, but it is the easiest way to view tables and manually edit campus map coordinates.

1. Download pgAdmin from:

```text
https://www.pgadmin.org/download/
```

2. Open pgAdmin and create a master password if it asks.

3. Register the local PostgreSQL server:

```text
Right click Servers -> Register -> Server
```

General tab:

```text
Name: Ride Management Local
```

Connection tab:

```text
Host name/address: localhost
Port: 5432
Maintenance database: ride_management_db
Username: ride_user
Password: ride_password
```

4. Open the query tool:

```text
Servers -> Ride Management Local -> Databases -> ride_management_db -> Tools -> Query Tool
```

5. Add a new campus location:

```sql
INSERT INTO campus_locations (name, category, latitude, longitude)
VALUES ('Department of CSE', 'Academic', 29.000000, 77.000000);
```

6. Update an existing campus location:

```sql
UPDATE campus_locations
SET latitude = 29.864123,
    longitude = 77.897456
WHERE name = 'Central Library';
```

7. View all campus locations:

```sql
SELECT * FROM campus_locations ORDER BY name;
```

Each teammate's local PostgreSQL database is separate. Data is shared between teammates only if everyone connects to the same hosted/shared PostgreSQL database.

## Quick Start

1. Start PostgreSQL locally.

2. Create the database and user:

```sql
CREATE ROLE ride_user WITH LOGIN PASSWORD 'ride_password';
CREATE DATABASE ride_management_db OWNER ride_user;
GRANT ALL PRIVILEGES ON DATABASE ride_management_db TO ride_user;
```

3. Start the backend.

Mac/Linux:

```bash
cd backend
./mvnw spring-boot:run
```

Windows:

```bat
cd backend
mvnw.cmd spring-boot:run
```

4. Start the frontend in a second terminal.

```bash
cd frontend
npm install
npm run dev
```

5. Open the app:

```text
http://localhost:5173
```

The frontend development server proxies `/api` and `/ws` requests to the backend on `8099`.

## Run Backend

Mac/Linux:

```bash
cd backend
./mvnw spring-boot:run
```

Windows:

```bat
cd backend
mvnw.cmd spring-boot:run
```

Backend runs at:

```text
http://localhost:8099
```

## Run Frontend

```bash
cd frontend
npm install
npm run dev
```

Frontend runs at:

```text
http://localhost:5173
```

During local development, Vite proxies frontend `/api` and `/ws` traffic to the Spring Boot backend on `8099`.

## Build And Test

Backend:

```bash
cd backend
./mvnw test
```

Frontend:

```bash
cd frontend
npm run build
```

## Core API Overview

```text
POST /api/auth/register/passenger
POST /api/auth/register/driver
POST /api/auth/login

GET  /api/users/me
PUT  /api/users/me

POST /api/drivers/availability/online
POST /api/drivers/availability/offline
GET  /api/drivers/available
GET  /api/drivers/requests
GET  /api/drivers/dashboard
POST /api/drivers/location

GET  /api/locations

POST /api/rides
GET  /api/rides/my
GET  /api/rides/{rideId}
POST /api/rides/{rideId}/accept
POST /api/rides/{rideId}/reject
POST /api/rides/{rideId}/start
POST /api/rides/{rideId}/complete
POST /api/rides/{rideId}/cancel

POST /api/ratings
```

## WebSocket Topics

The frontend connects to:

```text
ws://localhost:8099/ws
```

Live topics used:

```text
/topic/drivers/availability
/topic/drivers/location
/topic/rides/requests
/topic/rides/{rideId}
/topic/rides/{rideId}/driver-location
/topic/users/{userId}/notifications
/topic/drivers/{driverId}/dashboard
```

## Notes

- Deployment is intentionally out of scope because it was not required in the challenge.
- Drivers must allow browser location permission after going online for live GPS tracking to update.
- Bonus features such as route lines, scheduling, payments, analytics, and forecasting can be added later without changing the core structure.
