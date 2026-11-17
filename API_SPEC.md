# REST API Specification — Habit Tracker

Base path: `/api/v1`. JSON requests/responses. Require TLS in deployed environments.

## Standard response/error
Success returns resource DTO or page envelope. Errors use:
```json
{
  "type": "VALIDATION_ERROR",
  "title": "Request validation failed",
  "status": 400,
  "detail": "One or more fields are invalid",
  "fieldErrors": [{"field":"name","message":"must not be blank"}],
  "traceId": "opaque-correlation-id"
}
```
Do not expose stack traces, SQL, tokens, or internal exception messages.

## Authentication
- `POST /auth/register` — create account.
- `POST /auth/login` — authenticate.
- `POST /auth/logout` — terminate session.
- `POST /auth/forgot-password` — generic response regardless of account existence.
- `POST /auth/reset-password` — consume single-use expiring reset token.
- `GET /users/me` — current user profile.

## Habits
- `GET /habits?status=active|archived&page=0&size=20`
- `POST /habits`
- `GET /habits/{habitId}`
- `PATCH /habits/{habitId}`
- `POST /habits/{habitId}/archive`
- `POST /habits/{habitId}/restore`
- `DELETE /habits/{habitId}`

Create example:
```json
{
  "name": "Read",
  "description": "Read for 20 minutes",
  "scheduleType": "DAILY",
  "scheduleDays": [],
  "startDate": "2026-11-16"
}
```

## Completions
- `PUT /habits/{habitId}/completions/{localDate}` — idempotently set completion.
- `DELETE /habits/{habitId}/completions/{localDate}` — remove record or set incomplete according to chosen contract; choose one and document.
- `GET /habits/{habitId}/completions?from=YYYY-MM-DD&to=YYYY-MM-DD`

## Analytics
- `GET /analytics/summary?from=&to=`
- `GET /analytics/habits/{habitId}?from=&to=`

## User data
- `GET /users/me/export?format=json|csv`
- `DELETE /users/me` — require recent re-authentication/confirmation.

## Rules
- Validate path IDs, dates, page sizes, and enum values.
- Enforce ownership on every resource operation.
- Return 404 for inaccessible resources where this avoids resource enumeration.
- Use `409 CONFLICT` for genuine state conflicts.
- Document pagination and sorting defaults.
- Add OpenAPI annotations and keep generated docs aligned with this contract.
