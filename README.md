# Habit Tracker

A privacy-conscious habit tracking application built with React, TypeScript, Spring Boot, and PostgreSQL.

## Status

MVP implemented. All backend checks pass (`85 tests`, build green); all frontend checks pass (type-check, lint, unit tests, production build). The Playwright e2e spec exists but has not been executed because Playwright browsers are not installed on this machine — see "Known limitations" below.

## Prerequisites

- Java 21+ (developed and tested on Java 26; bytecode target is 21)
- Maven 3.9+ (`mvn` on PATH, or use a local distribution)
- Node.js LTS (frontend)
- PostgreSQL 15+ — optional for local development (the dev/test profiles use in-memory H2 in PostgreSQL compatibility mode; see `DECISIONS.md` ADR-001)

## Local setup (zero-setup dev mode)

Backend (in-memory H2, no database install needed):

```powershell
cd backend
mvn spring-boot:run          # starts on http://localhost:8080 (profile "dev")
```

Frontend (Vite dev server proxies `/api` to `http://localhost:8080`):

```powershell
cd frontend
npm install
npm run dev                  # starts on http://localhost:5173
```

Open http://localhost:5173 and register a test account.

## Running the checks

Backend (from `backend/`):

```powershell
mvn test                     # unit + integration tests (85 tests, H2 test profile)
mvn -DskipTests package      # executable boot jar: backend/target/habit-tracker-backend-0.1.0.jar
```

Frontend (from `frontend/`):

```powershell
npm install
npx tsc -b                   # type-check
npx eslint .                 # lint
npx vitest run               # unit/component tests
npm run build                # production build
```

## PostgreSQL

The Flyway migrations (V1–V4) are written for PostgreSQL. To run against a real database, set the environment variables from `.env.example` and activate the `postgres` profile — it is never the silent default:

```powershell
$env:SPRING_PROFILES_ACTIVE = "postgres"
$env:DATABASE_URL = "jdbc:postgresql://localhost:5432/habit_tracker"
$env:DATABASE_USERNAME = "app"
$env:DATABASE_PASSWORD = "..."
mvn spring-boot:run
```

Set `AUTH_COOKIE_SECURE=true` when serving over HTTPS, and `APP_CORS_ALLOWED_ORIGINS` to the deployed frontend origin. Never commit real `.env` values.

## API & docs

- Swagger UI: `http://localhost:8080/swagger-ui.html` (OpenAPI at `/v3/api-docs`)
- Contract docs: `API_SPEC.md`, `DATABASE.md`, `ARCHITECTURE.md`, `SECURITY.md`, `ACCEPTANCE_CRITERIA.md`

## Known limitations

- Reminders setting is stored but no scheduler/notifications ship in this MVP.
- Password reset tokens are logged to the console instead of emailed (`app.auth.dev-expose-reset-token`, dev only).
- Rate limiting is in-memory and single-node only (see `DECISIONS.md` ADR-005).
- E2E spec (`frontend/e2e/`) unrun — Playwright browsers not installed.
- Calendar week starts on Monday; the `weekStartsOn` setting is honored for analytics aggregation but the calendar grid is Monday-first.

## Security

Report vulnerabilities privately to the repository maintainer. Never publish credentials or real user exports in issues.
