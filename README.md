# PawCare Hub

PawCare Hub is a full-stack pet clinic booking and care management system built as a staged portfolio project. It combines real user flows, clinic/admin workflows, persistent relational data, scheduling foundations, lightweight medical-record direction, and an incremental migration toward a more standard Spring Security + JWT authentication model.

The project is not presented as an early prototype or a toy CRUD demo. V1, V2, and V3 were completed as deliberate milestones, and the current repository reflects a substantial end-to-end application with both user-facing and clinic-facing workflows.

## Project Overview

PawCare Hub is structured around two sides of a pet clinic product:

- A user-facing experience for pet owners managing pets, bookings, profiles, and appointment context
- A clinic-facing experience for staff operating bookings, services, staffing, scheduling, visit outcomes, and clinic operations

In its current state, the project includes:

- database-backed users, pets, services, bookings, staff, and scheduling data
- real booking workflows with staff selection and operational validation
- admin and clinic pages for day-to-day management tasks
- lightweight pet medical-record and visit-history direction
- dashboard and reporting-style overview data
- Spring Security + JWT migration work with JWT as the primary frontend authentication path

## Tech Stack

- Frontend: Vue 3, Vite, Vue Router, Pinia, Element Plus, Axios
- Backend: Java 17, Spring Boot 3, Spring Web, Spring Data JPA
- Database: MySQL
- Testing: JUnit 5, Spring Boot Test, MockMvc, H2 (test profile)
- Security/Auth: Spring Security + JWT

## Key Implemented Features

### User-Facing Features

- Registration and login backed by the database
- JWT-based authenticated frontend session flow
- Password hashing for stored credentials
- Legacy plain-text password upgrade to bcrypt on successful login where needed
- Profile editing and preferences management
- Change password flow
- Change email flow with current-password verification and forced re-login
- Logout flow
- Account deactivation flow
- Pet management:
  - list pets
  - create pet
  - edit pet
  - delete pet
  - view pet profile / details
- Service browsing backed by real service records
- Booking management:
  - create booking
  - view booking details
  - reschedule booking
  - cancel booking
- Structured appointment input using date picker + time-slot selection
- Future-time validation for appointments
- Real staff selection during booking and rescheduling
- Optional booking-side client message / owner note
- Read-only pet medical / visit-history visibility in the pet profile

### Admin / Clinic Features

- Admin dashboard with database-backed operational summary data
- Users management:
  - user list
  - filters
  - detail view
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
  - public-profile fields for homepage showcase
- Clinic Operations area for operational scheduling management
- Role-based clinic access boundaries for:
  - `admin`
  - `front_desk`
  - `doctor`

### Staff / Scheduling Features

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

### Pet Medical Record / Visit-History Direction

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

- Spring Security introduced as the current backend security foundation
- JWT returned on successful login
- Frontend uses `Authorization: Bearer ...` as the primary authenticated request path
- Backend resolves authenticated users through Spring Security `SecurityContext`
- Existing role-aware flows continue to work with the current security model

### Dashboard / Reporting

- Admin dashboard summary cards for core system counts
- Booking status mix overview
- Upcoming bookings snapshot
- Recent completed visits snapshot
- Additional operational insight blocks such as service usage and staff workload

### Homepage Public Staff Showcase

- Public homepage team section backed by real staff records
- Only staff who are both active and explicitly marked for public display appear there
- Admin staff management supports lightweight public-profile fields such as display name, title/specialty, short bio, photo URL, and homepage visibility

## Security / Auth Notes

- JWT is now the real primary frontend authentication path
- Backend user resolution uses Spring Security `SecurityContext`
- The earlier custom `X-User-Email` pattern has been removed from normal frontend use
- A narrow internal compatibility bridge still remains in the backend authentication filter for transitional stability, but it is intentionally contained and no longer the normal request path

## Testing

The backend includes a focused happy-path integration suite using:

- JUnit 5
- Spring Boot Test
- MockMvc
- H2 with a dedicated test profile

The current integration coverage focuses on major end-to-end flows such as:

- registration and login
- JWT token-authenticated profile access
- profile and account lifecycle flows
- pet creation, update, retrieval, and medical-note visibility
- booking creation, rescheduling, cancellation, and completion
- real staff selection
- staff availability and schedule exceptions
- admin role-protected operations
- dashboard data responses
- public staff homepage filtering

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

V2 deepened the data-backed product behavior, strengthened user/admin workflows, and moved the project beyond a basic demo into a more coherent clinic-management application.

### V3

V3 completed the major portfolio-stage expansion:

- real staff as first-class records
- booking-to-staff workflows
- scheduling foundations with weekly availability and date-specific overrides
- clinic operations tooling
- role layering for clinic users
- lightweight medical-record and visit-history direction
- visit completion and outcome capture
- richer dashboard visibility
- homepage public staff showcase
- Spring Security + JWT migration work

This staged evolution is intentional. The project reflects incremental product and architecture growth rather than a single all-at-once rewrite.

At the same time, the implementation remains pragmatic in places:

- the auth migration still retains a narrow compatibility bridge internally
- scheduling is foundation-first rather than a full calendar engine
- medical-record functionality is lightweight, not a full EMR/EHR system
- deployment, observability, and operational hardening are not yet the main focus of the repository

That balance is deliberate for a portfolio project. PawCare Hub is substantial and realistic, but it does not claim to be production-ready clinic software.

## Future Improvements

- richer scheduling and calendar UX
- deeper medical-record and visit workflow support
- stronger analytics and reporting
- deployment and production-hardening work

## Repository Structure

```text
.
|-- backend/   Spring Boot API, persistence, security, tests
|-- src/       Vue frontend pages, stores, API clients, layouts
|-- README.md
```

## Summary

PawCare Hub is a serious full-stack portfolio project centered on pet clinic booking and care management. Its current state demonstrates database-backed application design, multi-role workflows, scheduling logic, lightweight medical-record direction, operational dashboards, and incremental security modernization without overstating unimplemented features.
