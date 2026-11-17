import { useEffect, useState } from "react";
import { formatDateLong, todayLocal } from "../../lib/dates";

export interface CompletionToggleProps {
  habitId: string;
  habitName: string;
  date: string;
  completed: boolean;
  disabled?: boolean;
  /** Performs the API call; throws so the toggle can roll back. */
  onToggle: (habitId: string, date: string, completed: boolean) => Promise<void>;
}

/**
 * Optimistic completion toggle for one habit/day.
 * Flips immediately; rolls back the visual state if the server rejects.
 * Announces the result via a polite live region and always pairs color
 * with an icon + text so completion is never color-only.
 */
export function CompletionToggle({
  habitId,
  habitName,
  date,
  completed,
  disabled = false,
  onToggle,
}: CompletionToggleProps) {
  const [optimistic, setOptimistic] = useState<boolean | null>(null);
  const [announcement, setAnnouncement] = useState("");

  // Clear the optimistic overlay once the parent's state catches up.
  useEffect(() => {
    if (optimistic !== null && completed === optimistic) {
      setOptimistic(null);
    }
  }, [completed, optimistic]);

  const displayCompleted = optimistic ?? completed;
  const prettyDate = formatDateLong(date || todayLocal());

  const handleClick = () => {
    if (disabled) return;
    const next = !displayCompleted;
    setOptimistic(next);
    setAnnouncement(
      next
        ? `${habitName} marked complete for ${prettyDate}`
        : `${habitName} marked not complete for ${prettyDate}`,
    );
    onToggle(habitId, date, next)
      .then(() => {
        // Keep showing `next` until the parent prop confirms it.
        setAnnouncement(
          next
            ? `${habitName} marked complete for ${prettyDate}`
            : `${habitName} marked not complete for ${prettyDate}`,
        );
      })
      .catch(() => {
        setOptimistic(null);
        setAnnouncement(
          `Could not update ${habitName} for ${prettyDate}. Please try again.`,
        );
      });
  };

  return (
    <span className="inline-flex items-center gap-2">
      <button
        type="button"
        onClick={handleClick}
        disabled={disabled}
        aria-pressed={displayCompleted}
        aria-label={`${habitName} — mark ${
          displayCompleted ? "not complete" : "complete"
        } for ${prettyDate}`}
        className={`inline-flex min-h-10 items-center gap-2 rounded-lg border px-3 text-sm font-medium transition-colors
          focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-indigo-600
          disabled:cursor-not-allowed disabled:opacity-50
          ${
            displayCompleted
              ? "border-emerald-600 bg-emerald-50 text-emerald-800 hover:bg-emerald-100"
              : "border-slate-300 bg-white text-slate-700 hover:bg-slate-50"
          }`}
      >
        <span aria-hidden="true" className="text-base">
          {displayCompleted ? "✓" : "○"}
        </span>
        {displayCompleted ? "Done" : "Mark done"}
      </button>
      <span aria-live="polite" className="sr-only">
        {announcement}
      </span>
    </span>
  );
}
