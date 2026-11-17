import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "../../app/AuthContext";
import { useToast } from "../../app/ToastContext";
import { Button } from "../../components/Button";
import { TextField } from "../../components/TextField";
import { PasswordField } from "../../components/PasswordField";
import { Select } from "../../components/Select";
import { describeError } from "../../lib/api/errors";

const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

export function SignUp() {
  const { signUp } = useAuth();
  const { showSuccess, showError } = useToast();
  const navigate = useNavigate();

  const [displayName, setDisplayName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [timezone, setTimezone] = useState(
    () => Intl.DateTimeFormat().resolvedOptions().timeZone || "UTC",
  );
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [submitting, setSubmitting] = useState(false);

  const timezoneOptions = (() => {
    const intl = Intl as unknown as { supportedValuesOf?: (key: "timeZone") => string[] };
    const zones = intl.supportedValuesOf?.("timeZone") ?? [];
    return [
      ...zones.map((tz: string) => ({ value: tz, label: tz })),
      { value: "UTC", label: "UTC" },
    ];
  })();

  const validate = (): Record<string, string> => {
    const errors: Record<string, string> = {};
    if (!email.trim()) errors.email = "Email is required.";
    else if (!EMAIL_RE.test(email.trim())) errors.email = "Enter a valid email address.";
    if (!password) errors.password = "Password is required.";
    else if (password.length < 8) errors.password = "Password must be at least 8 characters.";
    return errors;
  };

  const handleSubmit = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const errors = validate();
    setFieldErrors(errors);
    if (Object.keys(errors).length > 0) return;
    setSubmitting(true);
    try {
      await signUp({
        email: email.trim(),
        password,
        displayName: displayName.trim() || undefined,
        timezone,
      });
      showSuccess("Account created. Welcome!");
      navigate("/", { replace: true });
    } catch (error) {
      const status = (error as { status?: number }).status;
      if (status === 409) {
        setFieldErrors({ email: "An account with this email already exists." });
      } else if (
        typeof status === "number" &&
        (error as { fieldErrors?: { field: string; message: string }[] }).fieldErrors
      ) {
        const serverErrors: Record<string, string> = {};
        for (const fe of (error as { fieldErrors: { field: string; message: string }[] }).fieldErrors) {
          serverErrors[fe.field] = fe.message;
        }
        setFieldErrors(serverErrors);
      } else {
        showError(describeError(error));
      }
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="mx-auto flex min-h-dvh w-full max-w-md flex-col justify-center px-4 py-10">
      <main className="rounded-xl border border-slate-200 bg-white p-6 shadow-sm sm:p-8">
        <h1 className="text-2xl font-bold text-slate-900">Create your account</h1>
        <p className="mt-1 text-sm text-slate-600">
          One small daily step at a time. Set up in under a minute.
        </p>
        <form onSubmit={handleSubmit} noValidate className="mt-6 flex flex-col gap-4">
          <TextField
            label="Display name"
            name="displayName"
            autoComplete="name"
            placeholder="Alex"
            value={displayName}
            onChange={(e) => setDisplayName(e.target.value)}
            error={fieldErrors.displayName}
            hint="Optional — used to greet you on the dashboard."
          />
          <TextField
            label="Email"
            type="email"
            name="email"
            autoComplete="email"
            placeholder="you@example.com"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            error={fieldErrors.email}
          />
          <PasswordField
            label="Password"
            name="password"
            autoComplete="new-password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            error={fieldErrors.password}
            hint="At least 8 characters."
          />
          <Select
            label="Timezone"
            name="timezone"
            value={timezone}
            onChange={(e) => setTimezone(e.target.value)}
            options={timezoneOptions}
            hint="Used to decide when your day starts."
          />
          <Button type="submit" loading={submitting} className="w-full">
            Create account
          </Button>
        </form>
        <p className="mt-4 text-sm text-slate-600">
          Already have an account?{" "}
          <Link to="/login" className="text-indigo-700 hover:underline">
            Sign in
          </Link>
        </p>
      </main>
    </div>
  );
}
