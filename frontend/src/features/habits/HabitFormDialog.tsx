import { useEffect, useRef, useState } from "react";
import { Button } from "../../components/Button";
import { TextField } from "../../components/TextField";
import { Checkbox } from "../../components/Checkbox";
import { Select, type SelectOption } from "../../components/Select";
import { ApiError } from "../../lib/api/errors";
import { scheduleDaysLabel, todayLocal } from "../../lib/dates";
import type { Habit, HabitInput, ScheduleType } from "../../types/api";

const FOCUSABLE = 'button, [href], input, select, textarea, [tabindex]:not([tabindex="-1"])';

export const COLOR_PRESETS: string[] = [
  "#4f46e5", // indigo
  "#0284c7", // sky-600
  "#059669", // emerald-600
  "#d97706", // amber-600
  "#dc2626", // red-600
  "#7c3aed", // violet-600
  "#0d9488", // teal-600
  "#db2777", // pink-600
];

export const ICON_PRESETS: string[] = ["🎯", "📚", "💪", "💧", "🧘", "🏃", "✍️", "🌱", "☀️", "🌙"];

const SCHEDULE_OPTIONS: SelectOption[] = [
  { value: "DAILY", label: "Every day" },
  { value: "WEEKDAYS", label: "Weekdays (Mon–Fri)" },
  { value: "CUSTOM", label: "Custom days" },
];

const WEEKDAY_CHECKBOXES: { day: number; label: string }[] = [
  { day: 1, label: "Mon" },
  { day: 2, label: "Tue" },
  { day: 3, label: "Wed" },
  { day: 4, label: "Thu" },
  { day: 5, label: "Fri" },
  { day: 6, label: "Sat" },
  { day: 7, label: "Sun" },
];

export interface HabitFormValues {
  name: string;
  description: string;
  color: string | null;
  icon: string | null;
  scheduleType: ScheduleType;
  scheduleDays: number[];
  startDate: string;
}

export function emptyHabitForm(): HabitFormValues {
  return {
    name: "",
    description: "",
    color: COLOR_PRESETS[0],
    icon: ICON_PRESETS[0],
    scheduleType: "DAILY",
    scheduleDays: [],
    startDate: todayLocal(),
  };
}

export function habitToForm(habit: Habit): HabitFormValues {
  return {
    name: habit.name,
    description: habit.description ?? "",
    color: habit.color,
    icon: habit.icon,
    scheduleType: habit.scheduleType,
    scheduleDays: [...habit.scheduleDays],
    startDate: habit.startDate,
  };
}

function formToInput(values: HabitFormValues): HabitInput {
  return {
    name: values.name.trim(),
    description: values.description.trim() || null,
    color: values.color,
    icon: values.icon,
    scheduleType: values.scheduleType,
    scheduleDays: values.scheduleType === "CUSTOM" ? [...values.scheduleDays].sort((a, b) => a - b) : [],
    startDate: values.startDate,
  };
}

/** Client-side validation mirroring the API contract. */
export function validateHabitForm(values: HabitFormValues): Record<string, string> {
  const errors: Record<string, string> = {};
  if (!values.name.trim()) errors.name = "Name is required.";
  else if (values.name.trim().length > 80) errors.name = "Name must be 80 characters or fewer.";
  if (!values.startDate) {
    errors.startDate = "Start date is required.";
  } else if (values.startDate > todayLocal()) {
    errors.startDate = "Start date cannot be in the future.";
  }
  if (values.scheduleType === "CUSTOM") {
    if (values.scheduleDays.length === 0) {
      errors.scheduleDays = "Choose at least one day of the week.";
    } else if (values.scheduleDays.some((d) => d < 1 || d > 7)) {
      errors.scheduleDays = "Days must be between Monday (1) and Sunday (7).";
    }
  }
  return errors;
}

export interface HabitFormDialogProps {
  open: boolean;
  /** Existing habit when editing, null when creating. */
  habit: Habit | null;
  submitting: boolean;
  serverError: ApiError | null;
  onSubmit: (input: HabitInput) => Promise<void>;
  onClose: () => void;
}

/** Create/edit habit modal: focus trap, Esc to close, inline fieldErrors. */
export function HabitFormDialog({
  open,
  habit,
  submitting,
  serverError,
  onSubmit,
  onClose,
}: HabitFormDialogProps) {
  const [values, setValues] = useState<HabitFormValues>(emptyHabitForm);
  const [errors, setErrors] = useState<Record<string, string>>({});
  const dialogRef = useRef<HTMLDivElement>(null);
  const previouslyFocused = useRef<HTMLElement | null>(null);

  useEffect(() => {
    if (open) {
      setValues(habit ? habitToForm(habit) : emptyHabitForm());
      setErrors({});
    }
  }, [open, habit]);

  useEffect(() => {
    if (!open) return;
    previouslyFocused.current = document.activeElement as HTMLElement | null;
    dialogRef.current?.querySelector<HTMLElement>(FOCUSABLE)?.focus();
    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === "Escape") {
        event.preventDefault();
        onClose();
        return;
      }
      if (event.key === "Tab" && dialogRef.current) {
        const focusable = Array.from(
          dialogRef.current.querySelectorAll<HTMLElement>(FOCUSABLE),
        ).filter((el) => !el.hasAttribute("disabled"));
        if (focusable.length === 0) return;
        const first = focusable[0];
        const last = focusable[focusable.length - 1];
        if (event.shiftKey && document.activeElement === first) {
          event.preventDefault();
          last.focus();
        } else if (!event.shiftKey && document.activeElement === last) {
          event.preventDefault();
          first.focus();
        }
      }
    };
    document.addEventListener("keydown", handleKeyDown);
    return () => {
      document.removeEventListener("keydown", handleKeyDown);
      previouslyFocused.current?.focus();
    };
  }, [open, onClose]);

  if (!open) return null;

  const fieldError = (field: string): string | undefined =>
    errors[field] ?? serverError?.fieldErrorFor(field);

  const set = <K extends keyof HabitFormValues>(key: K, value: HabitFormValues[K]): void =>
    setValues((current) => ({ ...current, [key]: value }));

  const handleSubmit = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const clientErrors = validateHabitForm(values);
    setErrors(clientErrors);
    if (Object.keys(clientErrors).length > 0) return;
    try {
      await onSubmit(formToInput(values));
    } catch {
      // Parent keeps the dialog open and passes serverError back down.
    }
  };

  const summaryText: string =
    values.scheduleType === "CUSTOM"
      ? values.scheduleDays.length > 0
        ? scheduleDaysLabel(values.scheduleDays)
        : "No days selected yet"
      : values.scheduleType === "WEEKDAYS"
        ? "Monday to Friday"
        : "Every day of the week";

  return (
    <div
      className="fixed inset-0 z-40 flex items-center justify-center overflow-y-auto bg-slate-900/50 p-4"
      onMouseDown={(event) => {
        if (event.target === event.currentTarget) onClose();
      }}
    >
      <div
        ref={dialogRef}
        role="dialog"
        aria-modal="true"
        aria-labelledby="habit-form-title"
        className="my-8 w-full max-w-lg rounded-2xl bg-white p-6 shadow-xl"
      >
        <h2 id="habit-form-title" className="text-lg font-semibold text-slate-900">
          {habit ? "Edit habit" : "New habit"}
        </h2>
        <form onSubmit={handleSubmit} noValidate className="mt-4 flex flex-col gap-4">
          <TextField
            label="Name"
            name="name"
            maxLength={80}
            value={values.name}
            onChange={(e) => set("name", e.target.value)}
            error={fieldError("name")}
            placeholder="e.g. Read 20 pages"
          />
          <TextField
            label="Description"
            name="description"
            maxLength={280}
            value={values.description}
            onChange={(e) => set("description", e.target.value)}
            error={fieldError("description")}
            placeholder="Optional details"
          />

          <fieldset className="flex flex-col gap-2">
            <legend className="text-sm font-medium text-slate-700">Icon</legend>
            <div className="flex flex-wrap gap-1.5">
              {ICON_PRESETS.map((icon) => (
                <button
                  key={icon}
                  type="button"
                  aria-pressed={values.icon === icon}
                  aria-label={`Icon ${icon}`}
                  onClick={() => set("icon", icon)}
                  className={`flex size-9 items-center justify-center rounded-lg border text-lg transition-colors
                    focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-indigo-600
                    ${values.icon === icon ? "border-indigo-600 bg-indigo-50" : "border-slate-200 hover:bg-slate-50"}`}
                >
                  {icon}
                </button>
              ))}
            </div>
          </fieldset>

          <fieldset className="flex flex-col gap-2">
            <legend className="text-sm font-medium text-slate-700">Color</legend>
            <div className="flex flex-wrap gap-1.5">
              {COLOR_PRESETS.map((color) => (
                <button
                  key={color}
                  type="button"
                  aria-pressed={values.color === color}
                  aria-label={`Color ${color}`}
                  onClick={() => set("color", color)}
                  className={`size-9 rounded-lg border-2 transition-colors
                    focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-indigo-600
                    ${values.color === color ? "border-slate-900" : "border-transparent"}`}
                  style={{ backgroundColor: color }}
                />
              ))}
            </div>
          </fieldset>

          <Select
            label="Schedule"
            name="scheduleType"
            value={values.scheduleType}
            onChange={(e) => set("scheduleType", e.target.value as ScheduleType)}
            options={SCHEDULE_OPTIONS}
            error={fieldError("scheduleType")}
          />

          {values.scheduleType === "CUSTOM" ? (
            <fieldset className="flex flex-col gap-2">
              <legend className="text-sm font-medium text-slate-700">Days of week</legend>
              <div className="flex flex-wrap gap-3">
                {WEEKDAY_CHECKBOXES.map(({ day, label }) => (
                  <Checkbox
                    key={day}
                    label={label}
                    checked={values.scheduleDays.includes(day)}
                    onChange={(e) => {
                      const checked = e.target.checked;
                      set(
                        "scheduleDays",
                        checked
                          ? [...values.scheduleDays, day]
                          : values.scheduleDays.filter((d) => d !== day),
                      );
                    }}
                  />
                ))}
              </div>
              {fieldError("scheduleDays") ? (
                <p className="text-xs font-medium text-red-700">{fieldError("scheduleDays")}</p>
              ) : (
                <p className="text-xs text-slate-500">Scheduled on {summaryText}.</p>
              )}
            </fieldset>
          ) : (
            <p className="text-xs text-slate-500">Scheduled on {summaryText}.</p>
          )}

          <TextField
            label="Start date"
            name="startDate"
            type="date"
            value={values.startDate}
            onChange={(e) => set("startDate", e.target.value)}
            error={fieldError("startDate")}
          />

          {serverError && serverError.fieldErrors.length === 0 ? (
            <p role="alert" className="rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700">
              {serverError.detail || serverError.message}
            </p>
          ) : null}

          <div className="mt-2 flex justify-end gap-3">
            <Button variant="secondary" onClick={onClose} disabled={submitting}>
              Cancel
            </Button>
            <Button type="submit" loading={submitting}>
              {habit ? "Save changes" : "Create habit"}
            </Button>
          </div>
        </form>
      </div>
    </div>
  );
}
