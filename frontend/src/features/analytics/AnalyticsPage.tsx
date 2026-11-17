import { useState } from "react";
import { useAnalytics } from "../../hooks/useAnalytics";
import { ErrorPanel } from "../../components/ErrorPanel";
import { Skeleton, SkeletonRows } from "../../components/Skeleton";
import { Select, type SelectOption } from "../../components/Select";
import { formatDateLong, lastNDaysRange } from "../../lib/dates";

const RANGE_OPTIONS: SelectOption[] = [
  { value: "7", label: "Last 7 days" },
  { value: "30", label: "Last 30 days" },
  { value: "90", label: "Last 90 days" },
];

function pct(rate: number | null): string {
  if (rate === null) return "—";
  // Rates arrive as 0..1 fractions (null when nothing was scheduled).
  return `${Math.round(rate * 100)}%`;
}

/** Streak badge pairing number with icon + label (never color alone). */
function StreakBadge({ icon, value, label }: { icon: string; value: number; label: string }) {
  return (
    <span className="inline-flex items-baseline gap-1.5">
      <span aria-hidden="true">{icon}</span>
      <span className="text-2xl font-bold text-slate-900">{value}</span>
      <span className="text-xs text-slate-500">{label}</span>
    </span>
  );
}

export function AnalyticsPage() {
  const [days, setDays] = useState("30");
  const { from, to } = lastNDaysRange(Number.parseInt(days, 10) || 30);
  const { summary, loading, error, refetch } = useAnalytics(from, to);

  const habits = summary?.habits ?? [];
  const overall = summary?.overall ?? null;

  return (
    <div className="flex flex-col gap-6">
      <header className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <h1 className="text-[26px] font-bold text-slate-900">Analytics</h1>
          <p className="text-sm text-slate-600">
            Streaks and completion rates for {formatDateLong(from)} – {formatDateLong(to)}.
          </p>
        </div>
        <Select
          label="Date range"
          value={days}
          onChange={(e) => setDays(e.target.value)}
          options={RANGE_OPTIONS}
          className="min-w-44"
        />
      </header>

      {loading ? (
        <>
          <Skeleton className="h-28 w-full rounded-xl" />
          <SkeletonRows rows={3} />
        </>
      ) : error ? (
        <ErrorPanel message={error} onRetry={refetch} />
      ) : !summary || habits.length === 0 ? (
        <ErrorPanel
          title="No data yet"
          message="Create a habit and check it off for a few days — analytics will appear here."
        />
      ) : (
        <div className="flex flex-col gap-6">
          {/* Explanation of the denominator, required by the contract. */}
          <p className="rounded-lg bg-slate-100 px-4 py-3 text-sm text-slate-600">
            Rate counts only <strong>scheduled</strong> dates from your habit's start through{" "}
            {formatDateLong(to)}; future dates are excluded. Days where the habit wasn't scheduled
            never count against you.
          </p>

          <section
            aria-labelledby="overall-heading"
            className="rounded-xl border border-slate-200 bg-white p-5"
          >
            <h2 id="overall-heading" className="text-base font-semibold text-slate-800">
              Overall
            </h2>
            <div className="mt-3 flex flex-wrap gap-x-8 gap-y-3">
              <span className="inline-flex items-baseline gap-1.5">
                <span className="text-3xl font-bold text-slate-900">{pct(overall?.completionRate ?? null)}</span>
                <span className="text-xs text-slate-500">completion rate</span>
              </span>
              <span className="inline-flex items-baseline gap-1.5">
                <span className="text-3xl font-bold text-slate-900">{overall?.completedCount ?? 0}</span>
                <span className="text-xs text-slate-500">completed</span>
              </span>
              <span className="inline-flex items-baseline gap-1.5">
                <span className="text-3xl font-bold text-slate-900">{overall?.scheduledCount ?? 0}</span>
                <span className="text-xs text-slate-500">scheduled</span>
              </span>
            </div>
          </section>

          <section aria-labelledby="per-habit-heading" className="flex flex-col gap-3">
            <h2 id="per-habit-heading" className="text-lg font-semibold text-slate-800">
              Per habit
            </h2>
            <div className="grid grid-cols-1 gap-4 xl:grid-cols-2">
              {habits.map((habit) => {
                const ratePct =
                  habit.completionRate === null
                    ? null
                    : Math.round(habit.completionRate * 100);
                return (
                  <article
                    key={habit.habitId}
                    aria-label={`Analytics for ${habit.name}`}
                    className="flex flex-col gap-3 rounded-xl border border-slate-200 bg-white p-5"
                  >
                    <div className="flex items-center justify-between gap-3">
                      <h3 className="truncate font-medium text-slate-900">{habit.name}</h3>
                      <span className="text-sm font-semibold text-slate-700">
                        {pct(habit.completionRate)}
                      </span>
                    </div>

                    <div
                      role="progressbar"
                      aria-valuenow={ratePct ?? 0}
                      aria-valuemin={0}
                      aria-valuemax={100}
                      aria-label={`${habit.name} completion rate: ${pct(habit.completionRate)}`}
                      className="h-2.5 w-full overflow-hidden rounded-full bg-slate-100"
                    >
                      <div
                        className="h-full rounded-full bg-indigo-600"
                        style={{ width: `${ratePct ?? 0}%` }}
                      />
                    </div>

                    <div className="flex flex-wrap items-center gap-x-6 gap-y-2">
                      <StreakBadge icon="🔥" value={habit.currentStreak} label="current streak" />
                      <StreakBadge icon="🏆" value={habit.bestStreak} label="best streak" />
                      <span className="text-xs text-slate-500">
                        {habit.completedCount} of {habit.scheduledCount} scheduled days completed
                      </span>
                    </div>

                    {habit.scheduledCount < 5 ? (
                      <p className="rounded-lg bg-amber-50 px-3 py-2 text-xs text-amber-800">
                        Small sample: only {habit.scheduledCount} scheduled day(s) in this range —
                        rates and streaks may not be meaningful yet.
                      </p>
                    ) : null}
                  </article>
                );
              })}
            </div>
          </section>
        </div>
      )}
    </div>
  );
}
