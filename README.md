# Campus Ride Management Platform

A full-stack campus ride management web application for IIT Roorkee-style e-rickshaw coordination. The platform connects passengers and drivers through a centralized system with authentication, ride requests, driver availability, real-time updates, maps, GPS tracking, scheduling, dashboards, and feedback.

This project is designed to run locally for evaluation and development. We use a local PostgreSQL database by default because it gives faster UI/API updates, avoids hosted database network delay, and makes the project easier to reproduce on any teammate's laptop.

## Project Overview

Campus transportation often suffers from fragmented ride coordination, unclear driver availability, and delayed passenger-driver communication. This application solves the core campus ride-management workflow:

- Passengers register, log in, request rides, schedule future rides, track ride status, view maps, and rate completed rides.
- Drivers register with vehicle and verification details, go online/offline, accept or reject ride requests, update ride lifecycle status, share GPS location, and view performance analytics.
- The backend keeps ride assignment consistent so one ride can only be accepted by one driver.
- WebSocket/STOMP updates keep passenger and driver dashboards synchronized in real time.

## Technology Stack

**Backend**

- Java 21
- Spring Boot 3
- Spring Security
- JWT authentication
- Spring Data JPA / Hibernate
- WebSocket/STOMP
- PostgreSQL
- Maven wrapper

**Frontend**

- React 18
- TypeScript
- Vite
- Leaflet with OpenStreetMap
- Recharts
- Lucide React icons
- Browser Geolocation API

**Local Development Tools**

- PostgreSQL server
- pgAdmin, optional database GUI
- IntelliJ IDEA, optional backend IDE

## Setup Instructions

### 1. Install Required Software

Install these before running the project:

- Java JDK 21
- Node.js 18 or newer
- npm
- PostgreSQL

Recommended but optional:

- pgAdmin for viewing tables visually
- IntelliJ IDEA for backend development

Check versions:

```bash
java -version
node -v
npm -v
```

Important: use Node.js 18 or newer. Older Node versions can fail when starting Vite.

### 2. Install PostgreSQL Locally

On macOS with Homebrew:

```bash
brew install postgresql
brew services start postgresql
```

If you installed a versioned PostgreSQL package, the service name may look like:

```bash
brew services start postgresql@18
```

Check that PostgreSQL is running:

```bash
pg_isready
```

### 3. Create Local Database And User

Open the PostgreSQL terminal:

```bash
psql postgres
```

Run these SQL commands:

```sql
CREATE ROLE ride_user WITH LOGIN PASSWORD 'ride_password';
CREATE DATABASE ride_management_db OWNER ride_user;
GRANT ALL PRIVILEGES ON DATABASE ride_management_db TO ride_user;
```

If the role or database already exists, that is fine. You can continue using the existing local database.

Exit `psql`:

```sql
\q
```

### 4. Confirm Backend Database Settings

The backend already defaults to this local database:

```text
Database URL: jdbc:postgresql://localhost:5432/ride_management_db
Username: ride_user
Password: ride_password
```

These defaults are configured in:

```text
backend/src/main/resources/application.properties
```

You do not need a shared or hosted database for this project. If you previously created `backend/.env` for a hosted database, either delete it or change it to local values:

```properties
DATABASE_URL=jdbc:postgresql://localhost:5432/ride_management_db
DATABASE_USERNAME=ride_user
DATABASE_PASSWORD=ride_password
JWT_SECRET=local-campus-ride-secret-change-if-needed
```

`backend/.env` is ignored by Git and should not be committed.

### 5. Install Frontend Dependencies

From the project root:

```bash
cd frontend
npm install
```

If `npm run dev` ever says `vite: command not found`, run `npm install` again inside the `frontend` folder.

### 6. Optional pgAdmin Setup

pgAdmin is not required to run the app. Use it only if you want to inspect or edit database tables visually.

Register a server with:

```text
Name: Ride Management Local
Host: localhost
Port: 5432
Database: ride_management_db
Username: ride_user
Password: ride_password
```

To view campus locations:

```sql
SELECT * FROM campus_locations ORDER BY name;
```

To add a campus location:

```sql
INSERT INTO campus_locations (name, category, latitude, longitude)
VALUES ('Department of CSE', 'Academic', 29.000000, 77.000000);
```

To update coordinates:

```sql
UPDATE campus_locations
SET latitude = 29.864123,
    longitude = 77.897456
WHERE name = 'Central Library';
```

## Running The Application

Run the backend and frontend in two separate terminals.

### Terminal 1: Start Backend

From the project root:

```bash
cd backend
./mvnw spring-boot:run
```

On Windows:

```bat
cd backend
mvnw.cmd spring-boot:run
```

Backend URL:

```text
http://localhost:8099
```

Spring Boot automatically creates or updates database tables using:

```properties
spring.jpa.hibernate.ddl-auto=update
```

### Terminal 2: Start Frontend

From the project root:

```bash
cd frontend
npm run dev
```

Frontend URL:

```text
http://localhost:5173
```

Open this URL in the browser. Vite proxies API and WebSocket traffic to the backend on port `8099`.

### Build Check

Backend compile:

```bash
cd backend
./mvnw compile -DskipTests
```

Frontend build:

```bash
cd frontend
npm run build
```

## Feature List

### Authentication And Profiles

- Passenger registration and login
- Driver registration and login
- JWT-based authentication
- Role-based access control
- Passenger profile management
- Driver vehicle details
- Driver verification information
- Sign out support

### Driver Availability

- Driver can go online
- Driver can go offline
- Driver availability shown to passengers
- Driver becomes busy only when a ride actually blocks current availability
- Future scheduled rides do not block live availability until their scheduled time arrives

### Ride Request And Assignment

- Passenger can request an immediate ride
- Passenger can choose pickup and destination from campus locations
- Driver can view incoming ride requests
- Driver can accept or reject ride requests
- Latest rejection is visible to the passenger
- Ride assignment uses database locking so only one driver can accept a ride
- Passenger receives popup notification when a ride is accepted by a driver

### Ride Lifecycle

Supported ride states:

- `REQUESTED`
- `ACCEPTED`
- `IN_PROGRESS`
- `COMPLETED`
- `CANCELLED`

Drivers can:

- Accept ride
- Reject ride
- Start ride
- Complete ride
- Cancel assigned ride

Passengers can:

- Request ride
- Cancel active or future ride
- View current ride status
- Rate completed ride

### Real-Time Updates

The app uses WebSocket/STOMP for:

- New ride request notifications
- Ride accepted/rejected updates
- Ride started/completed/cancelled updates
- Driver availability changes
- Driver dashboard updates
- Passenger notifications
- Live driver location updates

### Live Map And GPS

- Leaflet map with OpenStreetMap tiles
- Passenger map shows pickup marker
- Passenger map shows destination marker
- Passenger map shows assigned driver marker after acceptance
- Driver map shows pickup and destination for rides
- Driver browser GPS tracking using Geolocation API
- Driver marker updates live when location changes

### Ride Scheduling

- Passenger can schedule rides for future time slots
- Future scheduled rides appear in upcoming bookings
- Drivers can accept or reject scheduled rides
- Driver can only start scheduled ride at or after scheduled time
- Scheduled rides reserve a 30-minute slot
- Passenger cannot create overlapping scheduled rides
- Driver cannot accept overlapping scheduled rides
- Cancelling a future ride frees that slot for both passenger and driver
- Future bookings do not interrupt current live rides until their scheduled time arrives

### Ratings And Feedback

- Passenger can rate completed rides
- Passenger can add optional written feedback
- Driver average rating is maintained
- Driver feedback and rating history are visible in dashboard

### Driver Dashboard

- Completed ride count
- Live active ride count
- Average rating
- Rating count
- Ride history
- Scheduled bookings
- Active ride controls
- Ride status chart
- Passenger feedback history

## Useful Local URLs

```text
Frontend: http://localhost:5173
Backend:  http://localhost:8099
WebSocket: ws://localhost:8099/ws
Database: localhost:5432/ride_management_db
```

## Troubleshooting

**Backend port already in use**

The backend uses port `8099`. Stop the other process using that port, or change:

```properties
server.port=${PORT:8099}
```

inside `backend/src/main/resources/application.properties`.

**Frontend says `vite: command not found`**

Run:

```bash
cd frontend
npm install
```

**Frontend fails with a crypto/getRandomValues error**

Use Node.js 18 or newer:

```bash
node -v
```

**Database connection refused**

Start PostgreSQL:

```bash
brew services start postgresql
```

or, for a versioned install:

```bash
brew services start postgresql@18
```

Then verify:

```bash
pg_isready
```

**Tables are missing**

Start the backend once. Hibernate will create/update tables automatically because `ddl-auto=update` is enabled.

## Project Structure

```text
RideManagementPlatform/
  backend/
    src/main/java/...       Spring Boot source code
    src/main/resources/     application.properties
    pom.xml                 Maven configuration
    mvnw                    Maven wrapper

  frontend/
    src/                    React TypeScript source code
    package.json            Frontend dependencies and scripts
    vite.config.ts          Vite dev server and proxy config

  README.md
```

## Notes

- Deployment is not required for the current challenge submission.
- The project is configured for local PostgreSQL by default.
- pgAdmin and IntelliJ are optional helpers, not mandatory runtime requirements.
- Browser location permission must be allowed for driver GPS tracking.
