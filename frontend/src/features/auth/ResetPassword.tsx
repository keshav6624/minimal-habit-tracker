import { useState } from "react";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import { Button } from "../../components/Button";
import { PasswordField } from "../../components/PasswordField";
import { api } from "../../lib/api/client";
import { ApiError, describeError } from "../../lib/api/errors";
import { useToast } from "../../app/ToastContext";

export function ResetPassword() {
  const [searchParams] = useSearchParams();
  const token = searchParams.get("token") ?? "";
  const navigate = useNavigate();
  const { showSuccess, showError } = useToast();

  const [newPassword, setNewPassword] = useState("");
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [submitting, setSubmitting] = useState(false);
  const [done, setDone] = useState(false);

  const handleSubmit = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const errors: Record<string, string> = {};
    if (!token) errors.token = "Missing reset token. Please use the link from your email.";
    if (!newPassword) errors.newPassword = "New password is required.";
    else if (newPassword.length < 8) errors.newPassword = "Password must be at least 8 characters.";
    setFieldErrors(errors);
    if (Object.keys(errors).length > 0) return;
    setSubmitting(true);
    try {
      await api.post<unknown>("/auth/reset-password", { token, newPassword });
      setDone(true);
      showSuccess("Password updated. You can sign in now.");
    } catch (error) {
      if (error instanceof ApiError && error.fieldErrors.length > 0) {
        const serverErrors: Record<string, string> = {};
        for (const fe of error.fieldErrors) serverErrors[fe.field] = fe.message;
        setFieldErrors(serverErrors);
      } else if (error instanceof ApiError && error.status === 400) {
        setFieldErrors({ token: "This reset link is invalid or has expired." });
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
        <h1 className="text-2xl font-bold text-slate-900">Choose a new password</h1>
        {done ? (
          <div className="mt-4">
            <p className="rounded-lg bg-emerald-50 px-3 py-2 text-sm text-emerald-800" role="status">
              Your password has been updated.
            </p>
            <p className="mt-4 text-sm text-slate-600">
              <Link to="/login" className="text-indigo-700 hover:underline">
                Sign in
              </Link>
            </p>
          </div>
        ) : (
          <form onSubmit={handleSubmit} noValidate className="mt-6 flex flex-col gap-4">
            {fieldErrors.token ? (
              <p role="alert" className="rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700">
                {fieldErrors.token}
              </p>
            ) : null}
            <PasswordField
              label="New password"
              name="newPassword"
              autoComplete="new-password"
              value={newPassword}
              onChange={(e) => setNewPassword(e.target.value)}
              error={fieldErrors.newPassword}
              hint="At least 8 characters."
            />
            <Button type="submit" loading={submitting} className="w-full">
              Reset password
            </Button>
            <button
              type="button"
              onClick={() => navigate("/forgot-password")}
              className="text-left text-sm text-indigo-700 hover:underline"
            >
              Request a new reset link
            </button>
          </form>
        )}
      </main>
    </div>
  );
}
