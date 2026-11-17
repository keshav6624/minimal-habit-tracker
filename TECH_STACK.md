# Technology Stack — Habit Tracker

## Required
- Java 21 LTS (or current supported LTS chosen at project start).
- Spring Boot 3.x compatible with selected Java version.
- Spring Web, Spring Security, Validation, Spring Data JPA.
- PostgreSQL 16+ (pin a supported version).
- Flyway migrations.
- React + TypeScript + Vite.
- Tailwind CSS or a consistent component system.
- JUnit 5, Mockito, Testcontainers for PostgreSQL integration tests.
- Vitest + React Testing Library; Playwright for critical browser journeys.
- OpenAPI/Swagger.

## Engineering choices
- Pin dependency versions using Maven/Gradle lock/version catalog where possible.
- Use DTO validation at API boundaries.
- Use Testcontainers for tests that depend on PostgreSQL semantics.
- Avoid adding Redis, Kafka, microservices, or Kubernetes to the MVP without a measured need.
- Use a managed secret store or deployment environment variables; never commit `.env`.

## Environment variables (example names)
`DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD`, `APP_BASE_URL`, `AUTH_COOKIE_SECURE`, `MAIL_PROVIDER_API_KEY`.
Provide `.env.example` with fake placeholders only.
