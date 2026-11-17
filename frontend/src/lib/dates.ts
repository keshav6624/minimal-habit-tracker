/** Local-date helpers. All date keys are "YYYY-MM-DD" strings in the user's local timezone. */

export type DateKey = string;

const DATE_KEY_RE = /^\d{4}-\d{2}-\d{2}$/;

export function isDateKey(value: unknown): value is DateKey {
  return typeof value === "string" && DATE_KEY_RE.test(value);
}

/** Format a Date as "YYYY-MM-DD" in local time (never UTC — avoids off-by-one days). */
export function toLocalDateKey(date: Date): DateKey {
  const y = date.getFullYear();
  const m = String(date.getMonth() + 1).padStart(2, "0");
  const d = String(date.getDate()).padStart(2, "0");
  return `${y}-${m}-${d}`;
}

export function todayLocal(): DateKey {
  return toLocalDateKey(new Date());
}

/** Parse "YYYY-MM-DD" as a local date (noon-ish to dodge DST edges). */
export function fromDateKey(key: DateKey): Date {
  const [y, m, d] = key.split("-").map((part) => Number.parseInt(part, 10));
  return new Date(y, (m ?? 1) - 1, d ?? 1, 12, 0, 0, 0);
}

export function isLeapYear(year: number): boolean {
  return (year % 4 === 0 && year % 100 !== 0) || year % 400 === 0;
}

const DAYS_IN_MONTH = [31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31];

export function daysInMonth(year: number, monthIndex0: number): number {
  if (monthIndex0 === 1 && isLeapYear(year)) return 29;
  return DAYS_IN_MONTH[monthIndex0] ?? 30;
}

export function addDays(key: DateKey, days: number): DateKey {
  const date = fromDateKey(key);
  date.setDate(date.getDate() + days);
  return toLocalDateKey(date);
}

/** ISO weekday: 1 = Monday … 7 = Sunday. */
export function isoWeekday(key: DateKey): number {
  const day = fromDateKey(key).getDay(); // 0 = Sunday
  return day === 0 ? 7 : day;
}

export function firstOfMonth(year: number, monthIndex0: number): DateKey {
  return toLocalDateKey(new Date(year, monthIndex0, 1, 12, 0, 0, 0));
}

export function lastOfMonth(year: number, monthIndex0: number): DateKey {
  return toLocalDateKey(new Date(year, monthIndex0, daysInMonth(year, monthIndex0), 12, 0, 0, 0));
}

export interface CalendarDay {
  key: DateKey;
  /** Day-of-month number to display. */
  dayOfMonth: number;
  inMonth: boolean;
}

/**
 * Build the 6-week grid for a month. Leading/trailing cells belong to
 * neighbouring months; weeks start on the given ISO weekday (1=Mon..7=Sun).
 */
export function buildMonthGrid(
  year: number,
  monthIndex0: number,
  weekStartsOn: number,
): CalendarDay[] {
  const firstKey = firstOfMonth(year, monthIndex0);
  const firstIso = isoWeekday(firstKey);
  const offset = (firstIso - weekStartsOn + 7) % 7;
  const gridStart = addDays(firstKey, -offset);
  const cells: CalendarDay[] = [];
  for (let i = 0; i < 42; i++) {
    const key = addDays(gridStart, i);
    const date = fromDateKey(key);
    cells.push({
      key,
      dayOfMonth: date.getDate(),
      inMonth: date.getMonth() === monthIndex0,
    });
  }
  return cells;
}

const LONG_MONTHS = [
  "January", "February", "March", "April", "May", "June",
  "July", "August", "September", "October", "November", "December",
];

const SHORT_MONTHS = ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"];

export function monthName(year: number, monthIndex0: number, short = false): string {
  const names = short ? SHORT_MONTHS : LONG_MONTHS;
  return `${names[monthIndex0] ?? ""} ${year}`;
}

/** "November 16, 2026" style, using Intl with the given timezone. */
export function formatDateLong(key: DateKey, timezone?: string): string {
  return new Intl.DateTimeFormat("en-US", {
    year: "numeric",
    month: "long",
    day: "numeric",
    ...(timezone ? { timeZone: timezone } : {}),
  }).format(fromDateKey(key));
}

/** Short weekday labels for the grid header, ordered by weekStartsOn. */
export function weekdayHeaders(weekStartsOn: number): string[] {
  // ["Mon","Tue",...] indexed by ISO weekday - 1
  const base = ["Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"];
  const start = Math.min(Math.max(weekStartsOn, 1), 7) - 1;
  return [...base.slice(start), ...base.slice(0, start)];
}

export const WEEKDAY_LABELS: Record<number, string> = {
  1: "Monday",
  2: "Tuesday",
  3: "Wednesday",
  4: "Thursday",
  5: "Friday",
  6: "Saturday",
  7: "Sunday",
};

/** "Mon, Wed, Fri" summary of scheduleDays. */
export function scheduleDaysLabel(scheduleDays: number[]): string {
  return [...scheduleDays]
    .sort((a, b) => a - b)
    .map((d) => WEEKDAY_LABELS[d]?.slice(0, 3) ?? String(d))
    .join(", ");
}

/** Human summary for a habit's schedule. */
export function scheduleSummary(habit: { scheduleType: string; scheduleDays: number[] }): string {
  switch (habit.scheduleType) {
    case "DAILY":
      return "Every day";
    case "WEEKDAYS":
      return "Weekdays (Mon–Fri)";
    case "CUSTOM":
      return scheduleDaysLabel(habit.scheduleDays) || "Custom days";
    default:
      return "Custom schedule";
  }
}

/**
 * Which of the next `lookaheadDays` days (starting at `from`) is this habit
 * scheduled on? Used by the dashboard/calendar to compute scheduled counts
 * without a dedicated backend endpoint.
 */
export function isScheduledOn(habit: { scheduleType: string; scheduleDays: number[] }, key: DateKey): boolean {
  switch (habit.scheduleType) {
    case "DAILY":
      return true;
    case "WEEKDAYS":
      return isoWeekday(key) <= 5;
    case "CUSTOM":
      return habit.scheduleDays.includes(isoWeekday(key));
    default:
      return false;
  }
}

/** Inclusive list of date keys from..to (caller guarantees from <= to). */
export function dateRangeKeys(from: DateKey, to: DateKey): DateKey[] {
  const keys: DateKey[] = [];
  let cursor = from;
  while (cursor <= to) {
    keys.push(cursor);
    cursor = addDays(cursor, 1);
  }
  return keys;
}

/** Default analytics window: last N days ending today (N >= 1). */
export function lastNDaysRange(days: number): { from: DateKey; to: DateKey } {
  const to = todayLocal();
  return { from: addDays(to, -(Math.max(1, days) - 1)), to };
}
