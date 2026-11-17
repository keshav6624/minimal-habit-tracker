# Architecture Decision Records

Record material decisions here. Use one entry per decision.

## Template
### ADR-000: Short decision title
- **Status:** Proposed / Accepted / Superseded
- **Date:** YYYY-MM-DD
- **Context:** What problem or constraint prompted this decision?
- **Decision:** What are we choosing?
- **Alternatives:** What else was considered?
- **Consequences:** Benefits, costs, risks, and follow-up work.

## Initial decisions to confirm
- [ ] Confirm supported browsers/devices and minimum versions.
- [ ] Confirm deployment provider, regions, backups, and operational budget.
- [ ] Confirm data retention and account deletion policy.
- [ ] Confirm accessibility target (recommended: WCAG 2.2 AA).
- [ ] Confirm privacy policy and analytics approach before collecting telemetry.

## Accepted decisions

### ADR-001: H2 (PostgreSQL mode) as dev/test fallback; PostgreSQL via profile
- **Status:** Accepted
- **Date:** 2026-11-17
- **Context:** The development machine has no Docker and no PostgreSQL server. `TESTING.md` calls for Testcontainers-PostgreSQL for integration tests, which is unrunnable here.
- **Decision:** Flyway migrations are written in portable SQL that also runs on in-memory H2 in PostgreSQL compatibility mode (`MODE=PostgreSQL`). The `dev` and `test` Spring profiles use H2 (zero setup); a real PostgreSQL database is used only when the `postgres` profile is activated explicitly (`SPRING_PROFILES_ACTIVE=postgres` + `DATABASE_URL`/`DATABASE_USERNAME`/`DATABASE_PASSWORD`). `ddl-auto: validate` everywhere — Flyway owns the schema.
- **Alternatives:** Installing PostgreSQL locally; forcing Docker; skipping integration tests.
- **Consequences:** The suite runs anywhere without infrastructure, but H2 is not PostgreSQL: exotic SQL, lock behavior, and constraints can diverge. Follow-up: move integration tests to Testcontainers-PostgreSQL when Docker is available, and keep the migrations covered by `MigrationSmokeTest` so a real PostgreSQL run validates them the same way.

### ADR-002: Java 26 runtime with `maven.compiler.release=21`
- **Status:** Accepted
- **Date:** 2026-11-17
- **Context:** The only JDK installed is 26; `TECH_STACK.md` targets a Java LTS.
- **Decision:** Compile with `--release 21` so the bytecode and language level stay on Java 21 LTS while running tests/builds on the installed JDK 26.
- **Alternatives:** Installing JDK 21; targeting 26.
- **Consequences:** Builds run on this machine, and the artifact still runs on a Java 21+ runtime. CI should pin a JDK matching `release` (21) or newer.

### ADR-003: `schedule_days` stored as JSON TEXT
- **Status:** Accepted
- **Date:** 2026-11-17
- **Context:** `CUSTOM` schedules need a set of ISO day numbers (1=Mon..7=Sun). PostgreSQL would use `SMALLINT[]`, but H2 (dev/test) does not support PG array semantics the same way.
- **Decision:** Store `schedule_days` as a TEXT column containing a JSON array, mapped with a JPA `AttributeConverter` (`List<Integer>` ⇄ JSON string).
- **Alternatives:** `SMALLINT[]` (PG-only); CSV string; a join table.
- **Consequences:** Portable across H2 and PostgreSQL; loses PG-native array indexing/queries (not needed — schedule evaluation happens in the service layer). Follow-up: if analytics ever needs to filter by scheduled day in SQL, revisit.

### ADR-004: BCrypt with cost factor 12
- **Status:** Accepted
- **Date:** 2026-11-17
- **Context:** `SECURITY.md` requires a modern adaptive hash for passwords.
- **Decision:** `BCryptPasswordEncoder(12)`.
- **Alternatives:** Argon2id (stronger, extra dependency); cost 10 (faster, weaker).
- **Consequences:** ~250 ms/hash on dev hardware — acceptable for login/register/reset flows; hashes remain interoperable if the cost changes later (existing hashes keep their embedded cost).

### ADR-005: In-memory fixed-window rate limiter for auth endpoints
- **Status:** Accepted
- **Date:** 2026-11-17
- **Context:** `SECURITY.md` requires brute-force protection on auth endpoints; no external cache (Redis) is available in the MVP.
- **Decision:** A servlet filter counts requests per IP+endpoint in a JVM-local `ConcurrentHashMap` with a 1-minute fixed window (10 req/min); the 11th request gets a 429 `RATE_LIMITED` envelope. State is resettable for tests.
- **Alternatives:** Bucket4j; Redis-backed limiter; Spring Security's throttling.
- **Consequences:** Protects a single node only — horizontal deployments need a shared store. Documented as an MVP limitation.

### ADR-006: `DELETE /habits/{id}/completions/{date}` removes the row entirely
- **Status:** Accepted
- **Date:** 2026-11-17
- **Context:** Two viable contracts: hard-delete the completion row, or keep a `completed=false` tombstone. `PUT` with `completed=false` already stores an explicit false row.
- **Decision:** DELETE removes the record entirely and returns 404 `NOT_FOUND` when there is nothing to delete; `PUT {completed:false}` is the way to mark a scheduled day as explicitly skipped (kept for auditability).
- **Alternatives:** DELETE always 204 and creates a false tombstone.
- **Consequences:** Simple and idempotent-by-404; the calendar treats "no row" as not-completed, and an explicit false row distinguishes "skipped" from "no data".

### ADR-007: `PUT /users/me/settings` returns the saved state, not the session principal's view
- **Status:** Accepted
- **Date:** 2026-11-17
- **Context:** The authenticated principal is a detached copy of the `User` loaded from the session. After a timezone change the DB is updated but the principal's in-memory copy is stale, so rendering the response from the principal shows the old value.
- **Decision:** `UserService.updateSettings` re-loads a managed user, syncs `users.timezone` and `user_settings`, and returns the saved `SettingsDto` from the managed entities; the controller returns it as-is.
- **Alternatives:** Evicting/re-authenticating the session after a settings change.
- **Consequences:** The API always echoes persisted truth; date logic (completions, analytics) immediately uses the new timezone on the next request.
