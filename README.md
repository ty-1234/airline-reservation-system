# Airline Reservation System

Spring Boot 3 / Java 17 starter for a multi-user airline reservation platform with:

- JWT-based registration and login
- flight search and seat lookup
- concurrency-safe seat holds and booking confirmation
- cancellation and refund rules
- notification persistence
- admin management endpoints for airports, aircraft, and flights
- a lightweight browser UI served from `src/main/resources/static`

## Demo accounts

- `admin@airline.com` / `Admin@123`
- `customer@airline.com` / `Customer@123`
<img width="1908" height="899" alt="image" src="https://github.com/user-attachments/assets/3b4e0690-423d-4748-819b-a86b403efd1d" />


## Stack

- Java 17
- Spring Boot
- Spring Security
- Spring Data JPA / Hibernate
- H2 for local development
- PostgreSQL driver included for production wiring

## Run locally

1. Install Java 17+ and Maven.
2. Start the app:

```bash
mvn spring-boot:run
```

3. Open `http://localhost:8080/`
4. H2 console: `http://localhost:8080/h2-console`

## Main API routes

- `POST /api/auth/register`
- `POST /api/auth/login`
- `GET /api/flights/search`
- `GET /api/flights/{id}`
- `GET /api/flights/{id}/seats`
- `POST /api/bookings/hold`
- `POST /api/bookings/{pnr}/confirm`
- `GET /api/bookings/{pnr}`
- `POST /api/bookings/{pnr}/cancel`
- `GET /api/users/{id}/bookings`
- `POST /api/admin/airports`
- `POST /api/admin/aircraft`
- `POST /api/admin/flights`
- `PUT /api/admin/flights/{id}`

## Notes

- Seat inventory is tracked per flight, not just per aircraft.
- Holds expire automatically on a scheduled cleanup job.
- Booking confirmation marks held seats as booked only after successful payment.
- Setting `paymentReference` to `FAIL` is a simple way to simulate a payment failure.
