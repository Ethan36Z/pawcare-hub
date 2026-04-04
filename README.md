# PawCare Hub

PawCare Hub is a full-stack pet clinic booking and care management project. It includes a Vue-based client, a Spring Boot backend, and a MySQL database, with real persistence across users, pets, bookings, services, and admin views.

The project is no longer an early prototype. V1 is complete, V2 is in late-stage polish, and the current codebase already supports a solid set of end-to-end flows for both users and admins.

## Tech Stack

- Frontend: Vue 3, Vite, Vue Router, Pinia, Element Plus, Axios
- Backend: Java 17, Spring Boot 3, Spring Web, Spring Data JPA
- Database: MySQL
- Testing: JUnit 5, Spring Boot Test, MockMvc, H2 (test profile)

## User-Facing Features

- Real registration and login backed by the database
- Password hashing for stored credentials
- Automatic migration of legacy plain-text passwords to hashed passwords on login
- Profile page with real persisted profile fields and communication preferences
- Change password flow
- Logout flow
- Soft delete / account deactivation flow:
  - deactivated users are logged out immediately
  - deactivated users can no longer log in
  - account history remains in the database
- Pet management with real persistence:
  - list pets
  - create pet
  - edit pet
  - delete pet
  - view pet details
- Data-driven pet status badges
- Booking management with real persistence:
  - list current user bookings
  - create booking
  - view booking details
  - reschedule booking
  - cancel booking
- Active services page backed by real service records from the backend
- Minimal service-to-booking handoff:
  - `Book Now` on Services routes into the booking flow
  - the selected service is carried into the booking form
- First step toward a formal booking-to-service relationship:
  - new bookings can reference a real service record
  - backward compatibility is preserved for older bookings

## Admin Features

- Admin dashboard with real database-backed summary stats
- Admin users:
  - real user list
  - search by name or email
  - role filter
  - active/deactivated filter
  - real user details view
  - clearer inactive/deactivated user visibility in the UI
- Admin bookings:
  - real bookings list
  - confirm booking
  - cancel booking
  - search/filter/sort controls
- Admin services:
  - real services list
  - create service
  - edit service
  - enable / disable service availability
  - search/filter/sort controls

## Backend, Data, and Testing

- Spring Data JPA entities and repositories for users, pets, bookings, and services
- MySQL-backed persistence for the main application flows
- Header-based lightweight current-user resolution is still used instead of full Spring Security
- Service availability drives what appears on the public services page
- Booking creation now prefers real service records from the backend when available
- Minimal backend happy-path integration test suite is in place and passing

Current backend test coverage includes:

- register
- login
- create pet
- create booking
- admin confirm booking

Run backend tests with:

```bash
cd backend
mvn test
```

## Project Stage

### V1

V1 is complete. The project already has real database-backed CRUD-style flows for the main user and admin surfaces, along with working authentication, password hashing, profile management, service management, and booking management.

### V2

V2 is nearly complete and is currently in late-stage polish. The remaining work is mostly about tightening relationships between existing data models, improving UI clarity, adding more test coverage, and smoothing edge cases rather than building the core application from scratch.

### V3

V3 should be treated as future-facing expansion rather than current scope. It is the right place for deeper clinic operations, richer authorization, and more advanced product features.

## Future Improvements (V3 Direction)

- Doctor and staff management as first-class records instead of loose staff strings
- Real scheduling, availability, and calendar-based booking constraints
- Pet medical record direction:
  - visit notes
  - vaccination history
  - diagnoses
  - medications
- Richer dashboard reporting and charting
- Finer-grained permissions beyond the current lightweight user/admin split
- Better authentication and authorization architecture:
  - Spring Security
  - token/session handling
  - stronger ownership enforcement
- Location, map, membership, or payment-related extensions if the project grows in that direction

## Notes

- This is a serious portfolio project with real persistence and real CRUD-oriented flows, but it is not presented as production-ready clinic software.
- Some architecture choices are intentionally lightweight for learning and iteration speed, especially around authentication/session handling and authorization depth.
- The project summary above reflects what is actually implemented now, not an aspirational roadmap.
