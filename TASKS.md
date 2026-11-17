# Project Task Checklist

- [x] Confirm timezone and authentication decisions.
  - Timezone: IANA identifiers, stored per user (`users.timezone`), date logic computed in the user's zone.
  - Auth: server-side sessions (12 h, HttpOnly `SESSION` cookie) + CSRF double-submit cookie (`XSRF-TOKEN` / `X-XSRF-TOKEN`).
- [x] Initialize Spring Boot + React repositories. (`backend/`, `frontend/`)
- [x] Implement registration and login. (register, login, logout, `/csrf`, `/me`, password reset token flow, account deletion with password re-verification)
- [x] Implement habit CRUD and completion. (create/get/list-page/patch/delete, archive/restore, idempotent per-day completion upsert, range query, DELETE contract)
- [x] Implement calendar and analytics. (dashboard summary, per-habit streaks, completion rates, schedule-aware counts: DAILY / WEEKDAYS / CUSTOM)
- [x] Complete P0 release gates.
  - [x] Backend: `mvn test` — 85/85 pass; `mvn -DskipTests package` — build success.
  - [x] Frontend: `npx tsc -b`, `npx eslint .`, `npx vitest run`, `npm run build` — all green.
  - [ ] Playwright e2e smoke test executed (spec exists; browsers not installed on this machine — follow-up).
- [x] Record deviations from the default stack in `DECISIONS.md` (H2 dev/test fallback, Java 26 runtime with target 21, JSON `schedule_days`, in-memory rate limiter).
