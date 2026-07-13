# PawCareHub Backend

The PawCareHub backend is a Java 17 Spring Boot API for customer accounts, pets, bookings, services, staff scheduling, clinic operations, and role-based access control.

## Stack

- Spring Boot 3
- Spring Security with JWT authentication
- Spring Data JPA / Hibernate
- MySQL runtime database
- Maven
- JUnit 5, Spring Boot Test, MockMvc, and H2 for integration testing

## Configuration

The application reads database and runtime settings from environment variables:

| Variable | Default | Purpose |
|---|---|---|
| `DB_HOST` | `localhost` | MySQL host |
| `DB_PORT` | `3306` | MySQL port |
| `DB_NAME` | `pawcarehub` | Database name |
| `DB_USERNAME` | `root` | MySQL username |
| `DB_PASSWORD` | `password` | MySQL password |
| `SERVER_PORT` | `8080` | API port |
| `JWT_SECRET` | Development default | JWT signing secret |
| `JWT_EXPIRATION_MS` | `86400000` | JWT lifetime in milliseconds |
| `DEMO_SEED_ENABLED` | `false` | Enables controlled demo data when set to `true` |

## Run locally

```powershell
cd backend

$env:DB_USERNAME='root'
$env:DB_PASSWORD='your_mysql_password'
$env:DB_NAME='pawcarehub'

mvn spring-boot:run
```

## Demo mode

Demo seeding is disabled by default. To create the controlled walkthrough dataset, use the `demo` profile and explicitly enable it:

```powershell
cd backend

$env:DB_USERNAME='root'
$env:DB_PASSWORD='your_mysql_password'
$env:DB_NAME='pawcarehub_demo'
$env:SPRING_PROFILES_ACTIVE='demo'
$env:DEMO_SEED_ENABLED='true'

mvn spring-boot:run
```

The seeded dataset includes the `user`, `doctor`, `front_desk`, and `admin` roles; active and inactive customer accounts; services; staff availability; pets; and representative bookings. Demo accounts use the `@pawcarehub.demo` domain and the password `PawCareDemo123!`.

## Tests

```powershell
mvn clean test
```

The integration suite covers customer and booking flows, role authorization, account activation, booking schedule updates, scheduling conflicts, and opt-in demo-data seeding.
