# Implementation Plan — Habit Tracker

## Phase 0 — Discovery
- Confirm timezone policy, account recovery, deletion, and export semantics.
- Sketch dashboard, habit editor, calendar, analytics.
- Define API error format and database migrations.

## Phase 1 — Foundation
- Initialize backend/frontend.
- Add CI, formatting, linting, test runners.
- Configure PostgreSQL and Flyway.
- Add health endpoint and OpenAPI.

## Phase 2 — Authentication
- Register/login/logout.
- Password hashing, session/token strategy, rate limiting.
- Tests for invalid credentials, brute-force controls, authorization.

## Phase 3 — Habit lifecycle
- Create/list/get/update/archive/restore/delete.
- Ownership checks and validation.
- Frontend habit list and editor.

## Phase 4 — Daily completion
- Idempotent completion endpoint.
- Unique constraint and transaction handling.
- Dashboard today list and optimistic UI with rollback on error.

## Phase 5 — Calendar and analytics
- Date-range query.
- Correct schedule-aware streak and completion calculations.
- Tests around leap years, timezone boundaries, daylight-saving changes, and future dates.

## Phase 6 — Export, accessibility, hardening
- CSV/JSON export, account deletion.
- Keyboard and screen-reader review.
- Security review, load test, deployment smoke test.

## Release gates
Every phase must include automated tests and updated docs. Do not proceed with a failing migration or unresolved P0 security issue.
