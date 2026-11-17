# Testing Strategy — Habit Tracker

## Backend (implemented)
- **Unit tests:** schedule evaluation (`SchedulePolicyTest`), streak calculation (`StreakCalculatorTest`), completion rate (`CompletionRateCalculatorTest`), timezone/date policy (`TimezoneServiceTest`).
- **WebMvc tests:** request validation and error schema (`AuthValidationWebMvcTest`, `HabitsWebMvcTest`).
- **Integration tests** (`@SpringBootTest` + MockMvc, H2 test profile): auth flows, habit CRUD, completions (incl. concurrent duplicate PUTs), analytics, export, cross-tenant ownership (404, never 403), rate limiting, user settings/account deletion, password reset, Flyway migration smoke test.
- **Run:** `mvn test` (85 tests, green). Package: `mvn -DskipTests package`.

### Database note
Integration tests run on in-memory H2 in PostgreSQL compatibility mode because Docker/Testcontainers is unavailable on the dev machine (see `DECISIONS.md` ADR-001). The Flyway migrations are deliberately portable; `MigrationSmokeTest` asserts all four versions apply cleanly. Follow-up: switch integration tests to Testcontainers-PostgreSQL where available so real PostgreSQL semantics are exercised.

### Known spring-security-test interaction (important for new tests)
`SecurityMockMvcRequestPostProcessors.csrf()` **permanently replaces** the `CsrfFilter`'s token repository on the shared, cached `FilterChainProxy` with a session-backed `TestCsrfTokenRepository` (and never restores it). Tests that exercise the real SPA cookie flow (GET `/api/v1/auth/csrf` + `Set-Cookie: XSRF-TOKEN`) would then see no cookie. `AbstractIntegrationTest` therefore re-installs `CookieCsrfTokenRepository.withHttpOnlyFalse()` before every test; new integration tests should extend that base class rather than wiring MockMvc themselves.

## Frontend (implemented)
- **Unit/component tests (Vitest + jsdom):** form validation, completion toggle, empty/error/loading states, API client error normalization.
- **Run:** `npx vitest run` (plus `npx tsc -b`, `npx eslint .`, `npm run build`).
- **Stability note:** the SignUp component tests drive real timers through `userEvent` in jsdom; one transient failure was observed in 1 of 5 full runs (not reproduced since; the affected assertions use async `findByText` waits). If it recurs, switch the affected test to `vi.useFakeTimers()` or raise the `findBy*` timeout before treating it as a product bug.
- **E2E (Playwright):** spec exists under `frontend/e2e/` (register → create habit → complete today → calendar reflects → export) but has not been executed — Playwright browsers are not installed on this machine. Follow-up: `npx playwright install` then run the spec.

## Edge cases (covered by the above)
- Duplicate simultaneous completion requests (unique `(habit_id, local_date)` + service-level catch-and-reselect).
- User changes timezone (settings sync `users.timezone`; next requests use the new zone).
- Habit starts in the future / start date clamps analytics ranges.
- Archived habits (excluded from active lists, included in historical analytics).
- Weekday and custom schedules; empty analytics range; invalid ranges → 400.

## Quality gates
Run formatter, lint, type-check, unit/integration tests, production build, and E2E smoke tests before release. Report flaky tests; do not hide them with retries alone.
