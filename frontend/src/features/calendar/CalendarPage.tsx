import { useCallback, useEffect, useMemo, useState } from "react";
import { useHabits } from "../../hooks/useHabits";
import { useToast } from "../../app/ToastContext";
import { EmptyState } from "../../components/EmptyState";
import { ErrorPanel } from "../../components/ErrorPanel";
import { Skeleton } from "../../components/Skeleton";
import { Button } from "../../components/Button";
import { Select, type SelectOption } from "../../components/Select";
import { api } from "../../lib/api/client";
import { describeError } from "../../lib/api/errors";
import {
  buildMonthGrid,
  firstOfMonth,
  formatDateLong,
  lastOfMonth,
  monthName,
  weekdayHeaders,
} from "../../lib/dates";
import type { CompletionEntry } from "../../types/api";

/** Per-habit completion map for the visible month. */
interface HabitMonthData {
  completed: Set<string>;
}

export function CalendarPage() {
  const [viewYear, setViewYear] = useState(() => new Date().getFullYear());
  const [viewMonth, setViewMonth] = useState(() => new Date().getMonth());
  const [filterHabitId, setFilterHabitId] = useState("all");
  const [selectedDay, setSelectedDay] = useState<string | null>(null);
  const { habits, loading: habitsLoading, error: habitsError, refetch } = useHabits("active");
  const { showSuccess, showError } = useToast();

  const range = useMemo(
    () => ({ from: firstOfMonth(viewYear, viewMonth), to: lastOfMonth(viewYear, viewMonth) }),
    [viewYear, viewMonth],
  );

  const [monthData, setMonthData] = useState<Record<string, HabitMonthData>>({});
  const [loadingCompletions, setLoadingCompletions] = useState(true);
  const [completionsError, setCompletionsError] = useState<string | null>(null);
  const [reloadTick, setReloadTick] = useState(0);

  const loadCompletions = useCallback(async () => {
    if (habits.length === 0) {
      // No habits yet — nothing to fetch. setState with unchanged values
      // bails out, avoiding an infinite effect loop while habits load.
      setLoadingCompletions(false);
      setCompletionsError(null);
      return;
    }
    setLoadingCompletions(true);
    setCompletionsError(null);
    try {
      const results = await Promise.all(
        habits.map(async (habit): Promise<[string, HabitMonthData]> => {
          const payload = await api.get<{ completions: CompletionEntry[] }>(
            `/habits/${habit.id}/completions`,
            { from: range.from, to: range.to },
          );
          return [
            habit.id,
            {
              completed: new Set(
                payload.completions.filter((c) => c.completed).map((c) => c.localDate),
              ),
            },
          ];
        }),
      );
      setMonthData(Object.fromEntries(results));
    } catch (cause) {
      setCompletionsError(describeError(cause));
    } finally {
      setLoadingCompletions(false);
    }
  }, [habits, range.from, range.to]);

  // Reload when the visible month or the data tick changes.
  useEffect(() => {
    void loadCompletions();
  }, [loadCompletions, reloadTick]);

  const goToPrevMonth = () => {
    setSelectedDay(null);
    setViewMonth((m) => {
      if (m === 0) {
        setViewYear((y) => y - 1);
        return 11;
      }
      return m - 1;
    });
  };

  const goToNextMonth = () => {
    setSelectedDay(null);
    setViewMonth((m) => {
      if (m === 11) {
        setViewYear((y) => y + 1);
        return 0;
      }
      return m + 1;
    });
  };

  const weekStartsOn = 1; // Monday; Settings can override in a future iteration.
  const grid = buildMonthGrid(viewYear, viewMonth, weekStartsOn);
  const headers = weekdayHeaders(weekStartsOn);

  const visibleHabitIds = filterHabitId === "all" ? habits.map((h) => h.id) : [filterHabitId];

  const dayCounts = useMemo(() => {
    const counts: Record<string, { completed: number; scheduled: number }> = {};
    for (const cell of grid) {
      let completed = 0;
      for (const habitId of visibleHabitIds) {
        if (monthData?.[habitId]?.completed.has(cell.key)) completed += 1;
      }
      counts[cell.key] = { completed, scheduled: visibleHabitIds.length };
    }
    return counts;
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [grid, monthData, filterHabitId, habits]);

  const selectedSummary = selectedDay
    ? (() => {
        const count = dayCounts[selectedDay] ?? { completed: 0, scheduled: 0 };
        const completedHabits = visibleHabitIds.filter((id) =>
          monthData?.[id]?.completed.has(selectedDay),
        );
        const names = completedHabits
          .map((id) => habits.find((h) => h.id === id)?.name)
          .filter((n): n is string => typeof n === "string");
        return {
          count,
          completedHabits: names,
          label: formatDateLong(selectedDay),
        };
      })()
    : null;

  const habitFilterOptions: SelectOption[] = [
    { value: "all", label: "All habits" },
    ...habits.map((h) => ({ value: h.id, label: h.name })),
  ];

  return (
    <div className="flex flex-col gap-6">
      <header className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <h1 className="text-[26px] font-bold text-slate-900">Calendar</h1>
          <p className="text-sm text-slate-600">
            A month at a glance. Select a day to see exactly what was done.
          </p>
        </div>
        <Select
          label="Filter by habit"
          value={filterHabitId}
          onChange={(e) => {
            setFilterHabitId(e.target.value);
            setSelectedDay(null);
          }}
          options={habitFilterOptions}
          className="min-w-52"
        />
      </header>

      {habitsLoading ? (
        <Skeleton className="h-96 w-full rounded-xl" />
      ) : habitsError ? (
        <ErrorPanel message={habitsError} onRetry={refetch} />
      ) : habits.length === 0 ? (
        <EmptyState
          icon="📅"
          title="No habits to show yet"
          description="Your calendar fills in once you create a habit and start checking it off. Head to Habits to add your first one."
        />
      ) : (
        <section aria-label={`Month grid for ${monthName(viewYear, viewMonth)}`} className="flex flex-col gap-4">
          <div className="flex items-center justify-between gap-4">
            <Button variant="secondary" size="sm" aria-label="Previous month" onClick={goToPrevMonth}>
              ← Previous month
            </Button>
            <p aria-live="polite" className="text-lg font-semibold text-slate-900">
              {monthName(viewYear, viewMonth)}
            </p>
            <Button variant="secondary" size="sm" aria-label="Next month" onClick={goToNextMonth}>
              Next month →
            </Button>
          </div>

          {completionsError ? (
            <ErrorPanel
              message={completionsError}
              onRetry={() => setReloadTick((t) => t + 1)}
            />
          ) : null}

          {loadingCompletions && !completionsError ? (
            <div role="status" aria-label="Loading month data">
              <Skeleton className="h-80 w-full rounded-xl" />
            </div>
          ) : (
            <div className="overflow-x-auto rounded-xl border border-slate-200 bg-white p-3">
              <table className="w-full min-w-[360px] border-collapse">
                <caption className="sr-only">
                  Completion calendar for {monthName(viewYear, viewMonth)}. Each day shows how many
                  habits were completed.
                </caption>
                <thead>
                  <tr>
                    {headers.map((header) => (
                      <th
                        key={header}
                        scope="col"
                        className="pb-2 text-center text-xs font-medium text-slate-500"
                      >
                        {header}
                      </th>
                    ))}
                  </tr>
                </thead>
                <tbody>
                  {Array.from({ length: 6 }, (_, week) => (
                    <tr key={week}>
                      {grid.slice(week * 7, week * 7 + 7).map((cell) => {
                        const count = dayCounts[cell.key] ?? { completed: 0, scheduled: 0 };
                        const isCompleteDay =
                          count.scheduled > 0 && count.completed === count.scheduled;
                        const isPartial = count.completed > 0 && !isCompleteDay;
                        const isSelected = selectedDay === cell.key;
                        return (
                          <td key={cell.key} className="p-0.5 text-center align-middle">
                            <button
                              type="button"
                              onClick={() => setSelectedDay(cell.key)}
                              aria-pressed={isSelected}
                              aria-label={`${formatDateLong(cell.key)}, ${count.completed} of ${
                                count.scheduled
                              } habits completed`}
                              className={`relative mx-auto flex size-10 flex-col items-center justify-center rounded-lg text-sm transition-colors
                                focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-indigo-600
                                ${
                                  cell.inMonth
                                    ? "text-slate-800 hover:bg-slate-100"
                                    : "text-slate-300 hover:bg-slate-50"
                                }
                                ${isSelected ? "ring-2 ring-indigo-600 ring-offset-1" : ""}
                                ${isCompleteDay ? "bg-emerald-100" : isPartial ? "bg-indigo-50" : ""}`}
                            >
                              {cell.dayOfMonth}
                              {isCompleteDay || isPartial ? (
                                <span
                                  aria-hidden="true"
                                  className={`absolute bottom-0.5 text-[9px] ${
                                    isCompleteDay ? "text-emerald-700" : "text-indigo-600"
                                  }`}
                                >
                                  {isCompleteDay ? "✓" : "•"}
                                </span>
                              ) : null}
                            </button>
                          </td>
                        );
                      })}
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}

          {/* Textual summary for the selected day — never a tooltip-only affordance. */}
          {selectedDay && selectedSummary ? (
            <div
              role="status"
              className="rounded-xl border border-slate-200 bg-white p-4 text-sm text-slate-700"
            >
              <p className="font-medium text-slate-900">
                {selectedSummary.label}: {selectedSummary.count.completed} of{" "}
                {selectedSummary.count.scheduled} habits completed.
              </p>
              {selectedSummary.completedHabits.length > 0 ? (
                <p className="mt-1">
                  Completed: {selectedSummary.completedHabits.join(", ")}.
                </p>
              ) : (
                <p className="mt-1">No habits were completed on this day.</p>
              )}
              <button
                type="button"
                onClick={() => {
                  void (async () => {
                    try {
                      await Promise.all(
                        visibleHabitIds.map((habitId) =>
                          api.delete(`/habits/${habitId}/completions/${selectedDay}`),
                        ),
                      );
                      showSuccess("Completion removed for this day.");
                      setReloadTick((t) => t + 1);
                    } catch (cause) {
                      showError(describeError(cause));
                    }
                  })();
                }}
                className="mt-2 text-sm font-medium text-red-700 hover:underline"
              >
                Remove completions for this day
              </button>
            </div>
          ) : null}
        </section>
      )}
    </div>
  );
}
