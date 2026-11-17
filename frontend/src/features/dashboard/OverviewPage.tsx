import { useCallback, useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { useAuth } from "../../app/AuthContext";
import { useToast } from "../../app/ToastContext";
import { useHabits } from "../../hooks/useHabits";
import { useCompletions } from "../../hooks/useCompletions";
import { useAnalytics } from "../../hooks/useAnalytics";
import { CompletionToggle } from "../habits/CompletionToggle";
import { EmptyState } from "../../components/EmptyState";
import { ErrorPanel } from "../../components/ErrorPanel";
import { Skeleton, SkeletonRows } from "../../components/Skeleton";
import { describeError } from "../../lib/api/errors";
import {
  formatDateLong,
  isScheduledOn,
  lastNDaysRange,
  todayLocal,
} from "../../lib/dates";
import type { Habit } from "../../types/api";

function greetingFor(hour: number): string {
  if (hour < 5) return "Good night";
  if (hour < 12) return "Good morning";
  if (hour < 18) return "Good afternoon";
  return "Good evening";
}

interface TodayHabitRowProps {
  habit: Habit;
  today: string;
  /** Reports today's completion state up for the progress bar. */
  onCompletedChange: (habitId: string, completed: boolean) => void;
}

/** One habit row on the dashboard; owns its own completion state (optimistic). */
function TodayHabitRow({ habit, today, onCompletedChange }: TodayHabitRowProps) {
  const { completedDates, setCompleted } = useCompletions(habit.id, today, today);
  const { showSuccess, showError } = useToast();
  const completed = completedDates.has(today);

  useEffect(() => {
    onCompletedChange(habit.id, completed);
  }, [habit.id, completed, onCompletedChange]);

  const handleToggle = useCallback(
    async (_habitId: string, date: string, next: boolean) => {
      try {
        await setCompleted(date, next);
        showSuccess(next ? `"${habit.name}" marked complete.` : `"${habit.name}" unchecked.`);
      } catch (cause) {
        showError(describeError(cause));
        throw cause; // CompletionToggle rolls back its optimistic display.
      }
    },
    [setCompleted, habit.name, showSuccess, showError],
  );

  return (
    <li className="flex items-center justify-between gap-4 rounded-xl border border-slate-200 bg-white p-4">
      <div className="flex min-w-0 items-center gap-3">
        <span
          aria-hidden="true"
          className="flex size-9 shrink-0 items-center justify-center rounded-lg"
          style={{ backgroundColor: habit.color ? `${habit.color}22` : "#eef2ff" }}
        >
          {habit.icon ?? "🎯"}
        </span>
        <div className="min-w-0">
          <p className="truncate font-medium text-slate-900">{habit.name}</p>
          {habit.description ? (
            <p className="truncate text-xs text-slate-500">{habit.description}</p>
          ) : null}
        </div>
      </div>
      <CompletionToggle
        habitId={habit.id}
        habitName={habit.name}
        date={today}
        completed={completed}
        onToggle={handleToggle}
      />
    </li>
  );
}

/** Small streak badge pairing a number with icon + label (never color alone). */
function StreakBadge({ icon, value, label }: { icon: string; value: number; label: string }) {
  return (
    <span className="inline-flex items-baseline gap-1.5">
      <span aria-hidden="true">{icon}</span>
      <span className="text-2xl font-bold text-slate-900">{value}</span>
      <span className="text-xs text-slate-500">{label}</span>
    </span>
  );
}

export function OverviewPage() {
  const { user } = useAuth();
  const { habits, loading, error, refetch } = useHabits("active");
  const today = todayLocal();
  const { from, to } = lastNDaysRange(30);
  const {
    summary,
    loading: analyticsLoading,
    error: analyticsError,
    refetch: refetchAnalytics,
  } = useAnalytics(from, to);

  const [doneHabitIds, setDoneHabitIds] = useState<Set<string>>(new Set());

  const handleCompletedChange = useCallback((habitId: string, completed: boolean) => {
    setDoneHabitIds((previous) => {
      const next = new Set(previous);
      if (completed) next.add(habitId);
      else next.delete(habitId);
      return next;
    });
  }, []);

  const scheduledToday = habits.filter((habit) => isScheduledOn(habit, today));
  const doneToday = scheduledToday.filter((habit) => doneHabitIds.has(habit.id)).length;
  const progressPercent =
    scheduledToday.length === 0
      ? 0
      : Math.round((doneToday / scheduledToday.length) * 100);

  const activeAnalytics = summary?.habits.filter((h) => h.scheduledCount > 0) ?? [];
  const currentStreak = activeAnalytics.reduce((max, h) => Math.max(max, h.currentStreak), 0);
  const bestStreak = activeAnalytics.reduce((max, h) => Math.max(max, h.bestStreak), 0);
  const overallScheduled = summary?.overall.scheduledCount ?? 0;

  const combinedError = error ?? analyticsError;
  const retryAll = () => {
    refetch();
    refetchAnalytics();
  };

  const greeting = `${greetingFor(new Date().getHours())}${
    user?.displayName ? `, ${user.displayName}` : ""
  }`;

  return (
    <div className="flex flex-col gap-6">
      <header>
        <h1 className="text-[26px] font-bold text-slate-900">{greeting}</h1>
        <p className="text-sm text-slate-600">
          {formatDateLong(today, user?.timezone)} · {doneToday} of {scheduledToday.length} done today
        </p>
      </header>

      {loading || analyticsLoading ? (
        <div className="flex flex-col gap-4">
          <Skeleton className="h-24 w-full rounded-xl" />
          <SkeletonRows rows={3} />
        </div>
      ) : combinedError ? (
        <ErrorPanel message={combinedError} onRetry={retryAll} />
      ) : habits.length === 0 ? (
        <EmptyState
          icon="🌱"
          title="Let's set up your first habit"
          description='Nothing to track yet. Create a habit like "Read 20 pages" or "Drink water" — it takes under a minute, then it shows up here every day.'
          action={
            <Link
              to="/habits"
              className="inline-flex min-h-10 items-center rounded-lg bg-indigo-600 px-4 text-[15px] font-medium text-white hover:bg-indigo-700 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-indigo-600"
            >
              Add a habit
            </Link>
          }
        />
      ) : scheduledToday.length === 0 ? (
        <EmptyState
          icon="🛋️"
          title="Nothing scheduled today"
          description="None of your habits are scheduled for today. Enjoy the rest day, or adjust schedules from the Habits page."
          action={
            <Link
              to="/habits"
              className="inline-flex min-h-10 items-center rounded-lg border border-slate-300 bg-white px-4 text-[15px] font-medium text-slate-700 hover:bg-slate-50 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-indigo-600"
            >
              View habits
            </Link>
          }
        />
      ) : (
        <div className="grid grid-cols-1 gap-6 lg:grid-cols-3">
          <section aria-labelledby="today-habits-heading" className="flex flex-col gap-3 lg:col-span-2">
            <h2 id="today-habits-heading" className="text-lg font-semibold text-slate-800">
              Today's habits
            </h2>
            <ul className="flex flex-col gap-3">
              {scheduledToday.map((habit) => (
                <TodayHabitRow
                  key={habit.id}
                  habit={habit}
                  today={today}
                  onCompletedChange={handleCompletedChange}
                />
              ))}
            </ul>
          </section>

          <aside className="flex flex-col gap-4">
            <section
              aria-labelledby="progress-heading"
              className="rounded-xl border border-slate-200 bg-white p-5"
            >
              <h2 id="progress-heading" className="text-base font-semibold text-slate-800">
                Today's progress
              </h2>
              <p className="mt-2 text-sm text-slate-600">
                <span className="text-3xl font-bold text-slate-900">{progressPercent}%</span>{" "}
                ({doneToday} of {scheduledToday.length})
              </p>
              <div
                role="progressbar"
                aria-valuenow={progressPercent}
                aria-valuemin={0}
                aria-valuemax={100}
                aria-label={`Today's progress: ${doneToday} of ${scheduledToday.length} habits completed`}
                className="mt-3 h-2.5 w-full overflow-hidden rounded-full bg-slate-100"
              >
                <div
                  className="h-full rounded-full bg-emerald-600 transition-[width]"
                  style={{ width: `${progressPercent}%` }}
                />
              </div>
            </section>

            <section
              aria-labelledby="streak-heading"
              className="rounded-xl border border-slate-200 bg-white p-5"
            >
              <h2 id="streak-heading" className="text-base font-semibold text-slate-800">
                Streaks (last 30 days)
              </h2>
              <div className="mt-3 flex flex-col gap-3">
                <StreakBadge icon="🔥" value={currentStreak} label="current best streak (days)" />
                <StreakBadge icon="🏆" value={bestStreak} label="best streak in range (days)" />
              </div>
              {overallScheduled > 0 && overallScheduled < 5 ? (
                <p className="mt-3 rounded-lg bg-amber-50 px-3 py-2 text-xs text-amber-800">
                  Small sample: only {overallScheduled} scheduled day(s) in this range, so streaks
                  may not reflect long-term patterns.
                </p>
              ) : null}
            </section>
          </aside>
        </div>
      )}
    </div>
  );
}
