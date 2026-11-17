# AI Agent / Contributor Instructions

## Mission
Implement this project according to the documents in this repository. Treat `PRD.md`, `ACCEPTANCE_CRITERIA.md`, `SECURITY.md`, and `ARCHITECTURE.md` as authoritative. If they conflict, stop and report the conflict rather than silently choosing.

## Non-negotiable workflow
1. Read `HARNESS.md` and the project documents relevant to the task before editing.
2. Inspect the existing repository. Do not overwrite working code or invent files without checking.
3. Restate the task, affected modules, assumptions, and acceptance criteria before making broad changes.
4. Implement one vertical slice at a time: UI/API or extension UI/content script, persistence, validation, tests, and documentation.
5. Run the relevant tests, lint, type-check, and build commands. Never claim a check passed unless it was run and passed.
6. Review the diff for secrets, unsafe permissions, duplicated logic, dead code, and unrelated changes.
7. Update docs when behavior, APIs, schema, setup, or decisions change.

## Coding rules
- Prefer small, readable, cohesive modules and explicit types.
- Validate all untrusted input at system boundaries.
- Keep business logic out of UI components and HTTP controllers.
- Handle loading, empty, success, and error states.
- Avoid hard-coded secrets, credentials, tokens, and environment-specific URLs.
- Use migrations for database schema changes; do not rely on automatic destructive schema updates.
- Add tests for bug fixes and meaningful behavior.
- Do not add dependencies without explaining why they are needed.
- Do not disable security checks or weaken tests to make a build pass.
- Avoid premature microservices, overengineering, and speculative features.

## Manual-learning mode
The human developer writes the implementation. The agent should primarily:
- explain concepts and trade-offs;
- propose pseudocode and small examples;
- review code and identify bugs;
- suggest test cases;
- explain compiler/runtime errors;
- ask before generating a complete feature implementation.
When explicitly asked to implement, make the smallest complete change and explain every file changed.

## Definition of done
A task is done only when its acceptance criteria are met, tests/checks are reported accurately, relevant documentation is updated, and the final response lists changed files, commands run, results, and remaining limitations.
