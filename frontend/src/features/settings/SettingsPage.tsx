import { useCallback, useEffect, useState } from "react";
import { useAuth } from "../../app/AuthContext";
import { useToast } from "../../app/ToastContext";
import { useSettings } from "../../hooks/useSettings";
import { Button } from "../../components/Button";
import { Checkbox } from "../../components/Checkbox";
import { ErrorPanel } from "../../components/ErrorPanel";
import { Skeleton } from "../../components/Skeleton";
import { Select, type SelectOption } from "../../components/Select";
import { PasswordField } from "../../components/PasswordField";
import { ConfirmDialog } from "../../components/ConfirmDialog";
import { api, downloadBlob } from "../../lib/api/client";
import { describeError } from "../../lib/api/errors";
import { WEEKDAY_LABELS } from "../../lib/dates";
import type { Settings } from "../../types/api";

const WEEK_START_OPTIONS: SelectOption[] = [1, 2, 3, 4, 5, 6, 7].map((day) => ({
  value: String(day),
  label: WEEKDAY_LABELS[day] ?? String(day),
}));

function timezoneOptions(): SelectOption[] {
  const intl = Intl as unknown as { supportedValuesOf?: (key: "timeZone") => string[] };
  const zones = intl.supportedValuesOf?.("timeZone") ?? [];
  const options = zones.map((tz: string) => ({ value: tz, label: tz }));
  if (!zones.includes("UTC")) options.push({ value: "UTC", label: "UTC" });
  return options;
}

export function SettingsPage() {
  const { user, setSettings: setAuthSettings } = useAuth();
  const { settings, loading, error, refetch, saveSettings } = useSettings();
  const { showSuccess, showError } = useToast();

  const [timezone, setTimezone] = useState("");
  const [weekStartsOn, setWeekStartsOn] = useState("1");
  const [remindersEnabled, setRemindersEnabled] = useState(false);
  const [saving, setSaving] = useState(false);

  // Hydrate the form whenever the server-side settings object changes.
  useEffect(() => {
    if (settings) {
      setTimezone(settings.timezone);
      setWeekStartsOn(String(settings.weekStartsOn));
      setRemindersEnabled(settings.remindersEnabled);
    }
  }, [settings]);

  const dirty =
    settings !== null &&
    (timezone !== settings.timezone ||
      Number(weekStartsOn) !== settings.weekStartsOn ||
      remindersEnabled !== settings.remindersEnabled);

  const handleSave = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!settings) return;
    const next: Settings = {
      timezone,
      weekStartsOn: Number.parseInt(weekStartsOn, 10),
      remindersEnabled,
    };
    setSaving(true);
    try {
      const saved = await saveSettings(next);
      setAuthSettings(saved);
      showSuccess("Settings saved.");
    } catch (cause) {
      showError(describeError(cause));
    } finally {
      setSaving(false);
    }
  };

  const handleExport = async (format: "json" | "csv") => {
    try {
      const blob = await api.blob("/users/me/export", { format });
      downloadBlob(blob, format === "json" ? "habit-tracker-export.json" : "habit-tracker-export.csv");
      showSuccess(`Export downloaded (${format.toUpperCase()}).`);
    } catch (cause) {
      showError(describeError(cause));
    }
  };

  return (
    <div className="flex flex-col gap-6">
      <header>
        <h1 className="text-[26px] font-bold text-slate-900">Settings</h1>
        <p className="text-sm text-slate-600">Preferences, your data, and your account.</p>
      </header>

      {loading ? (
        <Skeleton className="h-72 w-full max-w-xl rounded-xl" />
      ) : error || !settings ? (
        <ErrorPanel message={error ?? "Could not load your settings."} onRetry={refetch} />
      ) : (
        <div className="flex max-w-xl flex-col gap-6">
          <form
            onSubmit={handleSave}
            aria-label="Preferences"
            className="flex flex-col gap-5 rounded-xl border border-slate-200 bg-white p-6"
          >
            <h2 className="text-lg font-semibold text-slate-900">Preferences</h2>
            <Select
              label="Timezone"
              value={timezone}
              onChange={(e) => setTimezone(e.target.value)}
              options={timezoneOptions()}
              hint={`Currently ${user?.timezone ?? "unknown"}. Decides when your day starts and ends.`}
            />
            <Select
              label="Week starts on"
              value={weekStartsOn}
              onChange={(e) => setWeekStartsOn(e.target.value)}
              options={WEEK_START_OPTIONS}
              hint="Applies to the calendar and weekly summaries."
            />
            <Checkbox
              label="Reminders"
              checked={remindersEnabled}
              onChange={(e) => setRemindersEnabled(e.target.checked)}
              description="Nudge me on days with unfinished habits."
            />
            <div>
              <Button type="submit" loading={saving} disabled={!dirty}>
                Save changes
              </Button>
            </div>
          </form>

          <section
            aria-labelledby="data-heading"
            className="flex flex-col gap-4 rounded-xl border border-slate-200 bg-white p-6"
          >
            <h2 id="data-heading" className="text-lg font-semibold text-slate-900">
              Your data
            </h2>
            <p className="text-sm text-slate-600">
              Download everything this account has stored: habits, completions, and settings.
            </p>
            <div className="flex flex-wrap gap-3">
              <Button variant="secondary" onClick={() => void handleExport("json")}>
                Export JSON
              </Button>
              <Button variant="secondary" onClick={() => void handleExport("csv")}>
                Export CSV
              </Button>
            </div>
          </section>

          <DangerZone />
        </div>
      )}
    </div>
  );
}

/** Delete account: requires password + typed confirmation. */
function DangerZone() {
  const { clearAuth } = useAuth();
  const [open, setOpen] = useState(false);
  const [password, setPassword] = useState("");
  const [confirmation, setConfirmation] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const canDelete =
    password.length > 0 && confirmation.trim().toUpperCase() === "DELETE";

  const handleDelete = async () => {
    setBusy(true);
    setError(null);
    try {
      await api.delete("/users/me", { password });
      setOpen(false);
      clearAuth();
      window.location.assign("/signup");
    } catch (cause) {
      setError(describeError(cause));
      setBusy(false);
    }
  };

  const handleOpen = useCallback(() => {
    setPassword("");
    setConfirmation("");
    setError(null);
    setOpen(true);
  }, []);

  return (
    <section
      aria-labelledby="danger-heading"
      className="flex flex-col gap-4 rounded-xl border border-red-200 bg-white p-6"
    >
      <h2 id="danger-heading" className="text-lg font-semibold text-red-800">
        Danger zone
      </h2>
      <p className="text-sm text-slate-600">
        Permanently delete your account and all habit data. This cannot be undone.
      </p>
      <Button variant="danger" onClick={handleOpen} className="self-start">
        Delete account…
      </Button>

      <ConfirmDialog
        open={open}
        title="Delete account?"
        confirmLabel="Delete my account"
        cancelLabel="Keep my account"
        variant="danger"
        busy={busy}
        confirmDisabled={!canDelete}
        onConfirm={() => void handleDelete()}
        onCancel={() => setOpen(false)}
      >
        <div className="flex flex-col gap-4">
          <p>
            This permanently deletes your account, habits, and completion history. To confirm,
            enter your password and type <strong>DELETE</strong>.
          </p>
          <PasswordField
            label="Password"
            autoComplete="current-password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
          />
          <PasswordField
            label="Type DELETE to confirm"
            autoComplete="off"
            value={confirmation}
            onChange={(e) => setConfirmation(e.target.value)}
          />
          {error ? (
            <p role="alert" className="rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700">
              {error}
            </p>
          ) : null}
        </div>
      </ConfirmDialog>
    </section>
  );
}
