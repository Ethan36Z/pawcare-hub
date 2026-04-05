# PawCare Hub

PawCare Hub is a full-stack pet clinic booking and care management system built as a staged portfolio project. It combines real user flows, clinic/admin workflows, persistent relational data, scheduling foundations, lightweight medical-record direction, and an incremental migration toward a more standard Spring Security + JWT authentication model.

The project is no longer positioned as an early prototype. V1, V2, and V3 have been completed as deliberate milestones, and the current repository represents a substantial end-to-end application rather than a CRUD demo.

## Project Overview

PawCare Hub is designed around two sides of a pet clinic product:

- The user-facing side for pet owners managing pets, bookings, profiles, and appointment context
- The clinic-facing side for staff operating bookings, services, staffing, scheduling, and visit outcomes

The current V3 state includes:

- real user registration and login flows
- database-backed pets, services, bookings, staff, and clinic operations data
- scheduling foundations with weekly availability and date-specific exceptions
- lightweight pet medical-record / visit-history direction
- role-aware admin and clinic workflows
- Spring Security + JWT migration work with JWT as the primary frontend auth path

## Tech Stack

- Frontend: Vue 3, Vite, Vue Router, Pinia, Element Plus, Axios
- Backend: Java 17, Spring Boot 3, Spring Web, Spring Data JPA
- Database: MySQL
- Testing: JUnit 5, Spring Boot Test, MockMvc, H2 (test profile)
- Security/Auth: Spring Security + JWT

## Core Implemented Features

### User-Facing Features

- Registration and login backed by the database
- JWT-based authenticated frontend session flow
- Password hashing for stored credentials
- Legacy plain-text password upgrade to bcrypt on successful login where needed
- Profile and communication preferences management
- Change password flow
- Logout flow
- Account deactivation flow
- Pet management:
  - list pets
  - create pet
  - edit pet
  - delete pet
  - view profile / details
- Lightweight pet profile and clinic-style record presentation
- Service browsing backed by real service records
- Booking management:
  - create booking
  - view booking details
  - reschedule booking
  - cancel booking
- Structured appointment input using date picker + time slot selection
- Future-time validation for appointments
- Real staff selection during booking and rescheduling
- Optional client message / owner note on booking creation
- Read-only pet medical / visit-history visibility in the pet profile

### Admin / Clinic Features

- Admin dashboard with real database-backed operational stats
- Users management:
  - user list
  - filters
  - details view
  - active/deactivated visibility
- Bookings management:
  - bookings list
  - confirm
  - cancel
  - complete visit
  - visit outcome entry
- Services management:
  - list
  - create
  - edit
  - enable / disable
- Staff management:
  - list
  - create
  - edit
  - active / inactive toggle
- Clinic Operations area for scheduling management
- Role-based clinic access boundaries for:
  - `admin`
  - `front_desk`
  - `doctor`

### Scheduling / Operational Features

- Real staff records persisted in MySQL
- Active/inactive staff state
- Booking and reschedule flows use real staff records
- Weekly staff availability model
- Date-specific schedule exceptions / overrides
- Editable weekly default template availability
- Exception-first, weekly-fallback scheduling behavior
- Booking-side availability filtering based on selected staff and date
- Backend validation that enforces availability rules during create/reschedule
- Clinic Operations visibility for:
  - selected staff
  - selected date
  - weekly default availability
  - active exception / override state
  - current effective scheduling rule

### Pet Medical Record Direction

- Extended pet profile fields that move beyond a minimal pet card
- Pet medical notes / visit-history timeline
- Medical notes linked to related booking / visit context where applicable
- Visit history display includes booking-related context such as:
  - visit date
  - service
  - clinician / staff
  - visit outcome details
- Booking completion can contribute lightweight visit-history context
- Clear separation between:
  - clinic-side medical / visit notes
  - user-facing booking client messages / owner notes

### Security / Authentication

- Spring Security introduced as part of the backend security foundation
- JWT returned on successful login
- Frontend uses `Authorization: Bearer ...` as the primary authenticated request path
- Backend resolves authenticated users via Spring Security `SecurityContext`
- Role information remains usable across current app flows
- A narrow internal legacy compatibility bridge still exists in the authentication filter for transitional stability, but normal frontend requests no longer depend on the old `X-User-Email` pattern

## Testing

The backend includes a focused happy-path integration suite using:

- JUnit 5
- Spring Boot Test
- MockMvc
- H2 with a dedicated test profile

The current integration coverage focuses on major end-to-end flows such as:

- registration and login
- profile and account lifecycle flows
- pet creation, update, retrieval, and medical-note visibility
- booking creation, rescheduling, cancellation, and completion
- real staff selection
- staff availability and schedule exceptions
- admin role-protected operations
- dashboard data responses
- JWT login and token-authenticated profile access

Run backend tests:

```bash
cd backend
mvn clean test
```

Run the frontend build:

```bash
npm run build
```

## Project Stage and Architecture Notes

### V1

V1 established the initial full-stack application shape: users, pets, bookings, services, admin pages, and persistent CRUD-oriented flows.

### V2

V2 deepened the data-backed product behavior, improved user/admin workflows, and pushed the project beyond a basic demo into a more coherent clinic-management application.

### V3

V3 completed the major portfolio-stage expansion:

- real staff as first-class records
- scheduling foundations
- clinic operations workflows
- lightweight medical-record direction
- visit outcomes
- richer dashboard visibility
- role layering
- Spring Security + JWT migration work

This staged evolution is intentional. The project reflects iterative product and architecture growth rather than a single all-at-once rewrite.

At the same time, the implementation remains pragmatic in places:

- some security migration compatibility behavior is still intentionally retained internally
- scheduling is foundation-first rather than a full calendar engine
- medical-record functionality is lightweight, not a full EMR/EHR system
- deployment, observability, and production hardening are not yet the focus of this repository

That balance is deliberate for a portfolio project: the application is substantial and realistic, but it does not claim to be production-ready clinic software.

## Future Improvements

- richer scheduling and calendar UX
- deeper medical-record and visit workflow support
- stronger reporting and analytics
- full removal of remaining compatibility bridge behavior in auth
- production-hardening, deployment, and operational readiness work

## Repository Structure

```text
.
|-- backend/   Spring Boot API, persistence, security, tests
|-- src/       Vue frontend pages, stores, API clients, layouts
|-- README.md
```

## Summary

PawCare Hub is a serious full-stack portfolio project centered on pet clinic booking and care management. Its current V3 state demonstrates database-backed application design, multi-role workflows, scheduling logic, lightweight medical-record direction, and incremental security modernization without overstating unimplemented features.
