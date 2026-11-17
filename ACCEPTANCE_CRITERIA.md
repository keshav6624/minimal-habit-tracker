# Acceptance Criteria — Habit Tracker

## P0 — Release blocking
- [ ] User can register, log in, and log out.
- [ ] Passwords are never stored in plaintext.
- [ ] User A cannot read, modify, archive, or delete User B's habits.
- [ ] User can create, edit, archive, restore, and delete a habit.
- [ ] Completion is unique per habit and local date, including concurrent requests.
- [ ] Timezone/date behavior is documented and tested.
- [ ] Dashboard shows today's scheduled habits and their completion state.
- [ ] Loading, empty, validation, and server-error states exist.
- [ ] Database migrations work from a clean database.
- [ ] Automated tests and production build pass.

## P1 — Portfolio quality
- [ ] Calendar history and habit filters work.
- [ ] Streak and completion-rate definitions match actual calculations.
- [ ] Analytics handles empty and partial ranges.
- [ ] Export produces valid CSV/JSON with correct user data only.
- [ ] Primary journeys are keyboard accessible.
- [ ] API docs and ERD are committed.
- [ ] Deployed app passes smoke tests.

## P2 — Later
- [ ] Reminders with explicit user consent.
- [ ] Import from another habit tracker.
- [ ] Optional themes and personalization.

## Demo script
Register → create “Read daily” → complete today → inspect calendar → view analytics → export data → sign out. Demonstrate cross-user authorization with automated tests, not another user's private data.
