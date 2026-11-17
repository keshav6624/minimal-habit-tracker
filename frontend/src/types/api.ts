/** All types mirror the backend API contract exactly (base `/api/v1`). */

export type ScheduleType = "DAILY" | "WEEKDAYS" | "CUSTOM";

export interface User {
  id: string;
  email: string;
  displayName: string;
  timezone: string;
}

export interface Settings {
  timezone: string;
  /** 1 = Monday … 7 = Sunday */
  weekStartsOn: number;
  remindersEnabled: boolean;
}

export interface Habit {
  id: string;
  name: string;
  description: string | null;
  color: string | null;
  icon: string | null;
  scheduleType: ScheduleType;
  /** Empty for DAILY/WEEKDAYS; 1=Mon..7=Sun for CUSTOM */
  scheduleDays: number[];
  /** YYYY-MM-DD */
  startDate: string;
  archivedAt: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface HabitInput {
  name: string;
  description?: string | null;
  color?: string | null;
  icon?: string | null;
  scheduleType: ScheduleType;
  scheduleDays: number[];
  /** YYYY-MM-DD */
  startDate: string;
}

export interface CompletionEntry {
  /** YYYY-MM-DD */
  localDate: string;
  completed: boolean;
  completedAt: string | null;
}

export interface CompletionPutResponse {
  localDate: string;
  completed: boolean;
  completedAt: string | null;
}

export interface Page<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface HabitAnalytics {
  habitId: string;
  name: string;
  currentStreak: number;
  bestStreak: number;
  completionRate: number | null;
  scheduledCount: number;
  completedCount: number;
}

export interface WeeklyBucket {
  weekStart: string;
  completedCount: number;
  scheduledCount: number;
}

export interface HabitAnalyticsDetail extends HabitAnalytics {
  weeks: WeeklyBucket[];
}

export interface AnalyticsOverall {
  completionRate: number | null;
  scheduledCount: number;
  completedCount: number;
}

export interface AnalyticsSummary {
  from: string;
  to: string;
  habits: HabitAnalytics[];
  overall: AnalyticsOverall;
}

export interface FieldError {
  field: string;
  message: string;
}

/** Raw error envelope returned by the backend for ALL failures. */
export interface ApiErrorBody {
  type: string;
  title: string;
  status: number;
  detail?: string;
  fieldErrors?: FieldError[];
  traceId?: string;
}
