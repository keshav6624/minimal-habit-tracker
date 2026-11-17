# UI / UX Specification — Habit Tracker

## Design direction
Calm, focused, low-friction. Use a neutral surface, one primary accent, restrained success color, generous spacing, and clear typography. Avoid streak-shaming, excessive gamification, and visual clutter.

## Design tokens
- Spacing scale: 4, 8, 12, 16, 24, 32, 48 px.
- Radius: 8 px controls, 12 px cards, 16 px dialogs.
- Typography: system/Inter-like sans serif; 14–16 px body, 24–32 px page title.
- Semantic colors: text, muted text, surface, border, primary, success, warning, danger. Pair colors with icons/text.
- Layout: max content width 1200 px; 12-column desktop grid; single-column mobile.

## Navigation
- Desktop: left sidebar — Overview, Habits, Calendar, Analytics, Settings.
- Mobile: compact top bar and bottom navigation or accessible menu.
- Account menu: profile, export data, sign out, delete account.

## Screens
### 1. Sign up / Sign in
- Email, password, show-password control, validation, password reset link.
- Inline errors; do not reveal whether an email exists during recovery.
- Loading and disabled submit state.

### 2. Overview
- Greeting and local date.
- “Today's habits” list with completion control.
- Progress summary (completed / scheduled).
- Current streak summary and small recent activity preview.
- Primary CTA: Add habit.
- Empty state explains how to create the first habit.

### 3. Habits
- Active and archived tabs.
- Habit card: name, schedule, next due date, completion status, overflow actions.
- Create/edit dialog or dedicated page.
- Confirm destructive delete; archive is preferred for reversible removal.

### 4. Calendar
- Month navigation with accessible previous/next buttons.
- Heatmap or month grid; each date has textual completion summary.
- Filter by habit.
- Tooltip is supplemental, never the only way to access information.

### 5. Analytics
- Weekly/monthly completion rate.
- Current and best streak.
- Per-habit trend.
- Explain calculation range and denominator.
- Avoid misleading charts when sample size is small.

### 6. Settings
- Timezone, week-start preference, account settings, data export, delete account.
- Clearly explain timezone changes and their effect on future date interpretation.

## Components
Button, TextField, PasswordField, Select, Checkbox, DatePicker, HabitCard, CompletionToggle, StreakMetric, ProgressRing/Bar, CalendarCell, EmptyState, Skeleton, Toast, ConfirmDialog, ErrorPanel.

## Interaction states
Every data-driven screen must include loading, empty, populated, validation error, network error, and retry states. Buttons need hover, focus-visible, active, disabled, and loading states.

## Accessibility
- Semantic landmarks and heading order.
- All controls have accessible names.
- Keyboard-operable calendar and dialogs.
- Focus trapping and restoration in modal dialogs.
- Reduced-motion support.
- Minimum 4.5:1 text contrast for normal text.
- Announce completion changes to assistive technology.

## Responsive behavior
- 360 px: one-column cards, compact calendar, no horizontal page scroll.
- 768 px: two-column overview where useful.
- 1280 px+: sidebar and multi-column analytics.
