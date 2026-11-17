# Architecture — Habit Tracker

## System context
Browser SPA → HTTPS REST API → Spring Boot application → PostgreSQL. Optional email provider for account recovery; optional job scheduler only after reminder requirements are approved.

## Backend layers
- `controller`: HTTP mapping, request/response DTOs, status codes.
- `service`: business rules and transaction boundaries.
- `repository`: Spring Data persistence.
- `domain`: entities and domain enums.
- `dto`: validated API contracts.
- `security`: authentication, authorization, password handling.
- `config`: CORS, OpenAPI, rate limits, serialization.
- `exception`: centralized error mapping.

Keep controllers thin. Never expose JPA entities directly in API responses.

## Frontend modules
- `app`: routing and application providers.
- `features/auth`, `features/habits`, `features/calendar`, `features/analytics`, `features/settings`.
- `components`: shared UI primitives.
- `lib/api`: typed API client and error normalization.
- `hooks`: reusable client behavior.
- `types`: API/domain types.

## Authentication
Use Spring Security. Prefer secure, HttpOnly, SameSite cookies for browser sessions where deployment topology permits; if using JWT, define short access-token lifetime, refresh rotation, revocation strategy, CSRF posture, and secure storage. Never store long-lived bearer tokens in localStorage.

## Data flow
1. UI submits validated request.
2. Controller validates DTO and authenticated principal.
3. Service checks ownership and domain invariants.
4. Repository performs query/write inside transaction.
5. Response DTO is returned with stable error format.

## Reliability
- Add request correlation IDs.
- Centralize exception handling.
- Set DB connection pool limits.
- Use migrations (Flyway recommended).
- Back up database and test restoration.
- Do not introduce caching until measured.

## Deployment
Separate frontend and API origins only if CORS/cookie policy is configured deliberately. Use TLS, managed PostgreSQL, environment-based configuration, health/readiness endpoint, and CI pipeline.
