# PawCare Hub

PawCare Hub is a pet clinic booking and care management project with a Vue frontend and a Spring Boot backend. The current version already supports a working user flow for registration, login, profile display, and user-specific pets and bookings pages, while some data handling is still intentionally lightweight for early development.

## Tech Stack

- Frontend: Vue 3, Vite, Vue Router, Pinia, Element Plus, Axios
- Backend: Java 17, Spring Boot 3, Spring Web, Spring Data JPA
- Database plan: MySQL later, but not required for the current local setup

## Implemented

- Frontend login and registration pages connected to real backend auth endpoints
- Backend auth endpoints:
  - `POST /api/auth/register`
  - `POST /api/auth/login`
- Health endpoint:
  - `GET /api/health`
- Profile page showing the current logged-in user
- Pets page backed by the backend through:
  - `GET /api/pets/me`
- My Bookings page backed by the backend through:
  - `GET /api/bookings/me`
- Basic user/admin route separation in the current lightweight role flow
- Local development CORS setup for the frontend on `http://localhost:5173`

## Temporary / In-Memory Parts

- Auth is not using JWT or Spring Security yet
- Passwords are not hashed yet
- Current user context for pets/bookings is temporarily resolved through a lightweight frontend-to-backend header-based approach
- Pets and bookings are still generated in memory on the backend rather than stored in a database
- Role handling is still simple and mock-oriented

## Not Implemented Yet

- Real database persistence for users, pets, bookings, and admin data
- Full Spring Security setup
- JWT or session-based authentication
- Password hashing
- Real booking CRUD
- Real pet CRUD
- Admin backend management flows
- Production-ready authorization, validation, and error handling

## Next Steps

- Move auth from in-memory logic to persistent user storage
- Add password hashing and real authentication/session handling
- Replace temporary pets and bookings generation with database-backed entities and repositories
- Add proper user ownership checks on backend business data
- Expand admin flows beyond the current lightweight role-based separation

## Status

This project is functional as a developer portfolio prototype, but it is not production-ready yet. The current focus is on building a clean full-stack structure first, then replacing temporary in-memory pieces with real persistence and security.
