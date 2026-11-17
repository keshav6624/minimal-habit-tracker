import { useCallback, useState } from "react";
import { useHabits } from "../../hooks/useHabits";
import { useHabitMutations } from "../../hooks/useHabitMutations";
import { useToast } from "../../app/ToastContext";
import { Button } from "../../components/Button";
import { EmptyState } from "../../components/EmptyState";
import { ErrorPanel } from "../../components/ErrorPanel";
import { SkeletonRows } from "../../components/Skeleton";
import { ConfirmDialog } from "../../components/ConfirmDialog";
import { ApiError, describeError } from "../../lib/api/errors";
import type { Habit, HabitInput } from "../../types/api";
import { HabitFormDialog } from "./HabitFormDialog";
import { HabitCard } from "./HabitCard";

type Tab = "active" | "archived";

export function HabitsPage() {
  const [tab, setTab] = useState<Tab>("active");
  const { habits, loading, error, refetch } = useHabits(tab);
  const { createHabit, updateHabit, archiveHabit, restoreHabit, deleteHabit } =
    useHabitMutations();
  const { showSuccess, showError } = useToast();

  const [dialogOpen, setDialogOpen] = useState(false);
  const [editingHabit, setEditingHabit] = useState<Habit | null>(null);
  const [formServerError, setFormServerError] = useState<ApiError | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [pendingDelete, setPendingDelete] = useState<Habit | null>(null);
  const [deleteBusy, setDeleteBusy] = useState(false);

  const openCreate = useCallback(() => {
    setEditingHabit(null);
    setFormServerError(null);
    setDialogOpen(true);
  }, []);

  const openEdit = useCallback((habit: Habit) => {
    setEditingHabit(habit);
    setFormServerError(null);
    setDialogOpen(true);
  }, []);

  const closeDialog = useCallback(() => setDialogOpen(false), []);

  const handleSubmit = useCallback(
    async (input: HabitInput): Promise<void> => {
      setSubmitting(true);
      setFormServerError(null);
      try {
        if (editingHabit) {
          const updated = await updateHabit(editingHabit.id, input);
          setDialogOpen(false);
          showSuccess(`Habit "${updated.name}" updated.`);
          refetch();
        } else {
          const created = await createHabit(input);
          setDialogOpen(false);
          showSuccess(`Habit "${created.name}" created.`);
          refetch();
        }
      } catch (cause) {
        if (cause instanceof ApiError) {
          // Inline fieldErrors are rendered by the dialog; keep it open.
          setFormServerError(cause);
        } else {
          showError(describeError(cause));
        }
        throw cause;
      } finally {
        setSubmitting(false);
      }
    },
    [editingHabit, createHabit, updateHabit, refetch, showSuccess, showError],
  );

  const handleArchive = useCallback(
    async (habit: Habit) => {
      try {
        await archiveHabit(habit.id);
        showSuccess(`Habit "${habit.name}" archived.`);
        refetch();
      } catch (cause) {
        showError(describeError(cause));
      }
    },
    [archiveHabit, refetch, showSuccess, showError],
  );

  const handleRestore = useCallback(
    async (habit: Habit) => {
      try {
        await restoreHabit(habit.id);
        showSuccess(`Habit "${habit.name}" restored.`);
        refetch();
      } catch (cause) {
        showError(describeError(cause));
      }
    },
    [restoreHabit, refetch, showSuccess, showError],
  );

  const handleDeleteConfirm = useCallback(async () => {
    if (!pendingDelete) return;
    setDeleteBusy(true);
    try {
      await deleteHabit(pendingDelete.id);
      showSuccess(`Habit "${pendingDelete.name}" permanently deleted.`);
      setPendingDelete(null);
      refetch();
    } catch (cause) {
      showError(describeError(cause));
    } finally {
      setDeleteBusy(false);
    }
  }, [pendingDelete, deleteHabit, refetch, showSuccess, showError]);

  return (
    <div className="flex flex-col gap-6">
      <header className="flex flex-wrap items-center justify-between gap-4">
        <div>
          <h1 className="text-[26px] font-bold text-slate-900">Habits</h1>
          <p className="text-sm text-slate-600">
            Define the small, repeatable actions you want to keep.
          </p>
        </div>
        <Button onClick={openCreate}>+ New habit</Button>
      </header>

      <div role="tablist" aria-label="Habit status filter" className="flex gap-2">
        {(["active", "archived"] as Tab[]).map((value) => (
          <button
            key={value}
            role="tab"
            aria-selected={tab === value}
            onClick={() => setTab(value)}
            className={`min-h-9 rounded-lg px-3 text-sm font-medium capitalize transition-colors
              focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-indigo-600
              ${
                tab === value
                  ? "bg-indigo-600 text-white"
                  : "border border-slate-300 bg-white text-slate-700 hover:bg-slate-100"
              }`}
          >
            {value}
          </button>
        ))}
      </div>

      {loading ? (
        <SkeletonRows rows={3} />
      ) : error ? (
        <ErrorPanel message={error} onRetry={refetch} />
      ) : habits.length === 0 ? (
        <EmptyState
          icon="🌱"
          title={tab === "active" ? "No habits yet" : "No archived habits"}
          description={
            tab === "active"
              ? 'Start by creating your first habit — for example "Read 20 pages" every day. Then check it off from the Overview or here.'
              : "Habits you archive will appear here. Archiving keeps history without cluttering your daily view."
          }
          action={
            tab === "active" ? (
              <Button onClick={openCreate}>Create your first habit</Button>
            ) : undefined
          }
        />
      ) : (
        <ul className="grid grid-cols-1 gap-4 md:grid-cols-2 xl:grid-cols-3">
          {habits.map((habit) => (
            <li key={habit.id}>
              <HabitCard
                habit={habit}
                onEdit={openEdit}
                onArchive={tab === "active" ? handleArchive : undefined}
                onRestore={tab === "archived" ? handleRestore : undefined}
                onDelete={setPendingDelete}
              />
            </li>
          ))}
        </ul>
      )}

      <HabitFormDialog
        open={dialogOpen}
        habit={editingHabit}
        submitting={submitting}
        serverError={formServerError}
        onSubmit={handleSubmit}
        onClose={closeDialog}
      />

      <ConfirmDialog
        open={pendingDelete !== null}
        title="Delete this habit?"
        confirmLabel="Delete permanently"
        variant="danger"
        busy={deleteBusy}
        onConfirm={() => void handleDeleteConfirm()}
        onCancel={() => setPendingDelete(null)}
      >
        <p>
          Deleting <strong>{pendingDelete?.name}</strong> removes the habit and{" "}
          <strong>all of its completion history</strong>. This action is destructive and cannot be
          undone. Consider archiving instead to keep history.
        </p>
      </ConfirmDialog>
    </div>
  );
}
