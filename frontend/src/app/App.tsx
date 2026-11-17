import { Navigate, Route, Routes } from "react-router-dom";
import type { ReactNode } from "react";
import { AuthProvider, useAuth } from "./AuthContext";
import { ToastProvider } from "./ToastContext";
import { ProtectedRoute } from "./ProtectedRoute";
import { SignIn } from "../features/auth/SignIn";
import { SignUp } from "../features/auth/SignUp";
import { ForgotPassword } from "../features/auth/ForgotPassword";
import { ResetPassword } from "../features/auth/ResetPassword";
import { AppLayout } from "./AppLayout";
import { OverviewPage } from "../features/dashboard/OverviewPage";
import { HabitsPage } from "../features/habits/HabitsPage";
import { CalendarPage } from "../features/calendar/CalendarPage";
import { AnalyticsPage } from "../features/analytics/AnalyticsPage";
import { SettingsPage } from "../features/settings/SettingsPage";
import { Spinner } from "../components/Spinner";

/** Full-page gate: wait for the /users/me probe before routing. */
function InitialGate({ children }: { children: ReactNode }) {
  const { status } = useAuth();
  if (status === "loading") {
    return (
      <div className="flex min-h-dvh items-center justify-center" role="status" aria-label="Checking your session">
        <Spinner />
      </div>
    );
  }
  return <>{children}</>;
}

/** /login etc. should bounce to the app when already signed in. */
function AnonymousOnly({ children }: { children: ReactNode }) {
  const { status } = useAuth();
  if (status === "authenticated") {
    return <Navigate to="/" replace />;
  }
  return <>{children}</>;
}

export default function App() {
  return (
    <AuthProvider>
      <ToastProvider>
        <Routes>
          <Route
            path="/login"
            element={
              <InitialGate>
                <AnonymousOnly>
                  <SignIn />
                </AnonymousOnly>
              </InitialGate>
            }
          />
          <Route
            path="/signup"
            element={
              <InitialGate>
                <AnonymousOnly>
                  <SignUp />
                </AnonymousOnly>
              </InitialGate>
            }
          />
          <Route
            path="/forgot-password"
            element={
              <InitialGate>
                <AnonymousOnly>
                  <ForgotPassword />
                </AnonymousOnly>
              </InitialGate>
            }
          />
          <Route
            path="/reset-password"
            element={
              <InitialGate>
                <AnonymousOnly>
                  <ResetPassword />
                </AnonymousOnly>
              </InitialGate>
            }
          />
          <Route element={<ProtectedRoute />}>
            <Route element={<AppLayout />}>
              <Route index element={<OverviewPage />} />
              <Route path="habits" element={<HabitsPage />} />
              <Route path="calendar" element={<CalendarPage />} />
              <Route path="analytics" element={<AnalyticsPage />} />
              <Route path="settings" element={<SettingsPage />} />
            </Route>
          </Route>
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </ToastProvider>
    </AuthProvider>
  );
}
