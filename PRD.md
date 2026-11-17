# Product Requirements Document — Habit Tracker

## 1. Product summary
A responsive habit-tracking web application that helps an authenticated user define repeatable habits, record daily completion, understand consistency, and review progress over time. The product prioritizes reliable date semantics, privacy, fast daily check-ins, and useful analytics over gamification.

## 2. Problem
People often abandon habit tools because daily logging is cumbersome, historical records are unreliable, and progress views do not explain consistency. This product makes check-in fast and history trustworthy.

## 3. Users and jobs-to-be-done
- **Individual user:** “When I complete a habit, I want to record it in seconds and see my progress.”
- **Returning user:** “When I miss days, I want an honest view of my consistency without losing historical records.”
- **Privacy-conscious user:** “I want my habits private and my account data exportable/deletable.”

## 4. Goals and non-goals
### Goals
- Secure account registration, login, logout, and account recovery.
- Create, edit, pause/archive, and delete habits.
- Record one completion state per habit per local date.
- Display calendar history, streaks, and weekly/monthly summaries.
- Support timezone-aware date boundaries.
- Provide accessible responsive UI and reliable exports.

### Non-goals for MVP
- Social feeds, public profiles, shared habits.
- AI coaching or medical/mental-health claims.
- Native mobile apps.
- Paid subscriptions and payment processing.
- Push notifications until consent and delivery infrastructure are designed.

## 5. Functional requirements
- FR-01: A user can register with a valid email and strong password.
- FR-02: A user can authenticate and terminate their session.
- FR-03: Users can create habits with name, optional description, color/icon, schedule, start date, and timezone policy.
- FR-04: Users can edit, archive, restore, and delete their own habits.
- FR-05: A user can mark a habit complete or incomplete for a specific local date.
- FR-06: The system prevents duplicate completion records for the same habit/date.
- FR-07: Users can view a calendar/heatmap for a selected date range.
- FR-08: Users can view current/best streak and completion rate with documented definitions.
- FR-09: Users can filter habits by active/archived and schedule.
- FR-10: Users can export their data in CSV or JSON.
- FR-11: Users can request account deletion and receive clear confirmation.
- FR-12: Empty, loading, validation, and server-error states are designed.

## 6. Business rules
- Completion is keyed by the habit's applicable local calendar date, not the server's date.
- Store instants in UTC where relevant and store the user's IANA timezone identifier (e.g. `Asia/Kolkata`).
- Enforce uniqueness at database level on `(habit_id, local_date)`.
- Archived habits remain visible in historical analytics but cannot receive new completions unless restored.
- Editing a schedule must not silently rewrite historical completion records.
- Streak definition: consecutive scheduled dates completed, not consecutive calendar days unless the habit is scheduled daily.
- Completion rate denominator includes only scheduled dates from habit start date through the selected range, excluding future dates.

## 7. UX requirements
- Daily check-in should take no more than two obvious interactions.
- Dashboard prioritizes “today” and the next actionable check-in.
- Do not use color alone to communicate completion.
- Provide keyboard operation, visible focus, labels, and screen-reader announcements.
- Responsive layout: mobile-first, tablet, desktop.

## 8. Non-functional requirements
- Passwords hashed with a modern adaptive password hash via Spring Security.
- Every user-owned resource is authorized server-side.
- API validation and rate limiting on authentication endpoints.
- Paginated history queries and indexed date lookups.
- Target WCAG 2.2 AA for primary flows.
- Basic observability without logging habit names or sensitive content.
- Initial performance target: common dashboard API p95 under 500 ms in a modest single-region deployment (validate with load testing).

## 9. MVP release criteria
All P0 acceptance criteria pass, migrations run from clean state, automated tests pass, deployment smoke test succeeds, and README setup is verified.
