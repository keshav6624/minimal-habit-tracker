import { Button } from "../../components/Button";
import { CompletionToggle } from "./CompletionToggle";
import { scheduleSummary, todayLocal } from "../../lib/dates";
import type { Habit } from "../../types/api";
import type { CompletionToggleProps } from "./CompletionToggle";

export interface HabitCardProps {
  habit: Habit;
  /** Today's completion state (optional; hide toggle when omitted). */
  completedToday?: boolean;
  onToggleToday?: CompletionToggleProps["onToggle"];
  onEdit: (habit: Habit) => void;
  onArchive?: (habit: Habit) => void;
  onRestore?: (habit: Habit) => void;
  onDelete: (habit: Habit) => void;
  busy?: boolean;
}

export function HabitCard({
  habit,
  completedToday,
  onToggleToday,
  onEdit,
  onArchive,
  onRestore,
  onDelete,
  busy = false,
}: HabitCardProps) {
  const isArchived = habit.archivedAt !== null;
  return (
    <article
      aria-label={`Habit: ${habit.name}`}
      className="flex flex-col gap-3 rounded-xl border border-slate-200 bg-white p-4 shadow-sm"
    >
      <div className="flex items-start gap-3">
        <span
          aria-hidden="true"
          className="flex size-10 shrink-0 items-center justify-center rounded-lg text-lg"
          style={{ backgroundColor: habit.color ? `${habit.color}22` : "#eef2ff", color: habit.color ?? "#4f46e5" }}
        >
          {habit.icon ?? "🎯"}
        </span>
        <div className="min-w-0 flex-1">
          <h3 className="truncate text-base font-semibold text-slate-900">{habit.name}</h3>
          {habit.description ? (
            <p className="mt-0.5 line-clamp-2 text-sm text-slate-600">{habit.description}</p>
          ) : null}
          <p className="mt-1 text-xs text-slate-500">
            {scheduleSummary(habit)} · started {habit.startDate}
          </p>
          {isArchived ? (
            <p className="mt-1 inline-block rounded bg-slate-100 px-1.5 py-0.5 text-xs font-medium text-slate-600">
              Archived {habit.archivedAt ? `on ${habit.archivedAt.slice(0, 10)}` : ""}
            </p>
          ) : null}
        </div>
      </div>

      {onToggleToday && !isArchived && completedToday !== undefined ? (
        <div className="mt-auto">
          <CompletionToggle
            habitId={habit.id}
            habitName={habit.name}
            date={todayLocal()}
            completed={completedToday}
            disabled={busy}
            onToggle={onToggleToday}
          />
        </div>
      ) : null}

      <div className="mt-1 flex flex-wrap gap-2">
        {!isArchived ? (
          <Button size="sm" variant="secondary" onClick={() => onEdit(habit)}>
            Edit
          </Button>
        ) : null}
        {onArchive && !isArchived ? (
          <Button size="sm" variant="ghost" onClick={() => onArchive(habit)}>
            Archive
          </Button>
        ) : null}
        {onRestore && isArchived ? (
          <Button size="sm" variant="secondary" onClick={() => onRestore(habit)}>
            Restore
          </Button>
        ) : null}
        <Button
          size="sm"
          variant="ghost"
          className="text-red-700 hover:bg-red-50"
          onClick={() => onDelete(habit)}
        >
          Delete
        </Button>
      </div>
    </article>
  );
}
