import { useState } from "react";
import { Link } from "react-router-dom";
import { Button } from "../../components/Button";
import { TextField } from "../../components/TextField";
import { api } from "../../lib/api/client";
import { describeError } from "../../lib/api/errors";
import { useToast } from "../../app/ToastContext";

/**
 * POST /auth/forgot-password always returns a neutral 202 regardless of
 * whether the email exists — the UI must never leak that distinction.
 */
export function ForgotPassword() {
  const { showError } = useToast();
  const [email, setEmail] = useState("");
  const [fieldError, setFieldError] = useState<string | null>(null);
  const [sent, setSent] = useState(false);
  const [submitting, setSubmitting] = useState(false);

  const handleSubmit = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!email.trim()) {
      setFieldError("Email is required.");
      return;
    }
    setFieldError(null);
    setSubmitting(true);
    try {
      await api.post<unknown>("/auth/forgot-password", { email: email.trim() });
      setSent(true);
    } catch (error) {
      showError(describeError(error));
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="mx-auto flex min-h-dvh w-full max-w-md flex-col justify-center px-4 py-10">
      <main className="rounded-xl border border-slate-200 bg-white p-6 shadow-sm sm:p-8">
        <h1 className="text-2xl font-bold text-slate-900">Forgot your password?</h1>
        {sent ? (
          <div className="mt-4">
            <p className="rounded-lg bg-emerald-50 px-3 py-2 text-sm text-emerald-800" role="status">
              If an account exists for that email, a reset link has been sent. Please check your inbox.
            </p>
            <p className="mt-4 text-sm text-slate-600">
              <Link to="/login" className="text-indigo-700 hover:underline">
                Back to sign in
              </Link>
            </p>
          </div>
        ) : (
          <>
            <p className="mt-1 text-sm text-slate-600">
              Enter your account email and we'll send you a reset link.
            </p>
            <form onSubmit={handleSubmit} noValidate className="mt-6 flex flex-col gap-4">
              <TextField
                label="Email"
                type="email"
                name="email"
                autoComplete="email"
                placeholder="you@example.com"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                error={fieldError ?? undefined}
              />
              <Button type="submit" loading={submitting} className="w-full">
                Send reset link
              </Button>
            </form>
            <p className="mt-4 text-sm text-slate-600">
              Remembered it?{" "}
              <Link to="/login" className="text-indigo-700 hover:underline">
                Sign in
              </Link>
            </p>
          </>
        )}
      </main>
    </div>
  );
}
