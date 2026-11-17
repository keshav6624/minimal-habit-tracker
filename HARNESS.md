# Project Harness

## Purpose
This repository is the engineering contract for this project. It defines product behavior, user experience, architecture, data/API contracts, security, implementation order, and quality gates.

## Document authority
1. `PRD.md` — user problem, personas, scope, functional requirements.
2. `ACCEPTANCE_CRITERIA.md` — observable pass/fail conditions.
3. `SECURITY.md` — mandatory security constraints.
4. `ARCHITECTURE.md`, `DATABASE.md`, `API_SPEC.md` — technical contracts.
5. `UI_UX.md` — visual and interaction contract.
6. `IMPLEMENTATION_PLAN.md` — recommended execution order.
7. `AGENTS.md`, `CODING_STANDARDS.md`, `TESTING.md` — engineering workflow.

If a requirement is ambiguous, record the question in `DECISIONS.md` and ask the product owner. Do not invent sensitive behavior or silently expand scope.

## How to work
- Start with one user journey and implement it end-to-end.
- Keep a `TASKS.md` checklist and mark work only after verification.
- Make small commits with descriptive messages.
- Add/update tests with each feature.
- Keep migrations and API documentation in sync.
- Use feature flags or staged rollout for risky functionality.
- Do not call a prototype production-ready until security, reliability, accessibility, and deployment checks pass.

## Suggested task loop
1. Select a small task from `IMPLEMENTATION_PLAN.md`.
2. Read the related requirement and acceptance criteria.
3. Define inputs, outputs, failure modes, and tests.
4. Implement manually.
5. Run format, lint, type-check, tests, and build.
6. Review UX states and security implications.
7. Update `TASKS.md`, docs, and decision log.
8. Commit.

## Repository quality gates
- No secrets in source control.
- No unhandled promise/API errors.
- No critical accessibility blockers in primary flows.
- No unvalidated user-controlled data.
- No undocumented schema/API breaking changes.
- No tests skipped without a documented reason.
- Setup instructions work from a clean clone.

## Manual developer rule
The developer should be able to explain each module, query, endpoint, browser permission, and major trade-off without relying on generated code.
