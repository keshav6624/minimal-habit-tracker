import { useRef, useState } from "react";
import { NavLink, Outlet, useNavigate } from "react-router-dom";
import { useAuth } from "./AuthContext";
import { useToast } from "./ToastContext";
import { api, downloadBlob } from "../lib/api/client";
import { describeError } from "../lib/api/errors";

const NAV_ITEMS = [
  { to: "/", label: "Overview", end: true },
  { to: "/habits", label: "Habits" },
  { to: "/calendar", label: "Calendar" },
  { to: "/analytics", label: "Analytics" },
  { to: "/settings", label: "Settings" },
];

function navLinkClass({ isActive }: { isActive: boolean }): string {
  return `flex items-center gap-3 rounded-lg px-3 py-2 text-sm font-medium transition-colors
    focus-visible:outline-2 focus-visible:outline-offset-1 focus-visible:outline-indigo-600
    ${isActive ? "bg-indigo-50 text-indigo-700" : "text-slate-600 hover:bg-slate-100 hover:text-slate-900"}`;
}

function AccountMenu() {
  const { user, signOut } = useAuth();
  const { showSuccess, showError } = useToast();
  const navigate = useNavigate();
  const [open, setOpen] = useState(false);
  const [busy, setBusy] = useState(false);
  const menuRef = useRef<HTMLDivElement>(null);

  if (!user) return null;

  const handleExport = async (format: "json" | "csv") => {
    setBusy(true);
    try {
      const blob = await api.blob("/users/me/export", { format });
      downloadBlob(blob, format === "json" ? "habit-tracker-export.json" : "habit-tracker-export.csv");
      showSuccess(`Export downloaded (${format.toUpperCase()}).`);
      setOpen(false);
    } catch (error) {
      showError(describeError(error));
    } finally {
      setBusy(false);
    }
  };

  const handleSignOut = async () => {
    setBusy(true);
    try {
      await signOut();
      navigate("/login");
    } catch (error) {
      showError(describeError(error));
      setBusy(false);
    }
  };

  return (
    <div ref={menuRef} className="relative">
      <button
        type="button"
        aria-haspopup="menu"
        aria-expanded={open}
        aria-label="Account menu"
        onClick={() => setOpen((v) => !v)}
        className="flex min-h-10 w-full items-center gap-3 rounded-lg px-3 py-2 text-left transition-colors
          hover:bg-slate-100 focus-visible:outline-2 focus-visible:outline-offset-1 focus-visible:outline-indigo-600"
      >
        <span
          aria-hidden="true"
          className="flex size-8 items-center justify-center rounded-full bg-indigo-600 text-sm font-semibold text-white"
        >
          {user.displayName.charAt(0).toUpperCase() || "?"}
        </span>
        <span className="min-w-0 flex-1 truncate text-sm text-slate-700">{user.email}</span>
        <span aria-hidden="true" className="text-slate-400">▾</span>
      </button>
      {open ? (
        <div
          role="menu"
          aria-label="Account actions"
          className="absolute bottom-full z-30 mb-2 w-full min-w-56 rounded-xl border border-slate-200 bg-white p-1.5 shadow-lg"
        >
          <div className="px-3 py-2">
            <p className="text-sm font-medium text-slate-800">{user.displayName}</p>
            <p className="truncate text-xs text-slate-500">{user.email}</p>
            <p className="truncate text-xs text-slate-400">{user.timezone}</p>
          </div>
          <div className="my-1 border-t border-slate-100" />
          <button
            role="menuitem"
            type="button"
            disabled={busy}
            onClick={() => void handleExport("json")}
            className="block w-full rounded-lg px-3 py-2 text-left text-sm text-slate-700 hover:bg-slate-100
              disabled:text-slate-400 focus-visible:outline-2 focus-visible:outline-offset-1 focus-visible:outline-indigo-600"
          >
            Export data (JSON)
          </button>
          <button
            role="menuitem"
            type="button"
            disabled={busy}
            onClick={() => void handleExport("csv")}
            className="block w-full rounded-lg px-3 py-2 text-left text-sm text-slate-700 hover:bg-slate-100
              disabled:text-slate-400 focus-visible:outline-2 focus-visible:outline-offset-1 focus-visible:outline-indigo-600"
          >
            Export data (CSV)
          </button>
          <div className="my-1 border-t border-slate-100" />
          <button
            role="menuitem"
            type="button"
            disabled={busy}
            onClick={() => void handleSignOut()}
            className="block w-full rounded-lg px-3 py-2 text-left text-sm text-red-700 hover:bg-red-50
              disabled:text-slate-400 focus-visible:outline-2 focus-visible:outline-offset-1 focus-visible:outline-red-600"
          >
            Sign out
          </button>
        </div>
      ) : null}
    </div>
  );
}

/** App shell: desktop left sidebar (≥1024px), mobile top bar + bottom nav. */
export function AppLayout() {
  return (
    <div className="min-h-dvh bg-slate-50">
      <a
        href="#main-content"
        className="sr-only focus:not-sr-only focus:absolute focus:left-2 focus:top-2 focus:z-50 focus:rounded-lg focus:bg-indigo-600 focus:px-3 focus:py-2 focus:text-sm focus:text-white"
      >
        Skip to main content
      </a>

      {/* Desktop sidebar */}
      <header className="fixed inset-y-0 left-0 z-30 hidden w-64 flex-col border-r border-slate-200 bg-white px-4 py-5 lg:flex">
        <p className="px-3 text-lg font-bold text-indigo-700">Habit Tracker</p>
        <nav aria-label="Primary" className="mt-6 flex-1">
          <ul className="space-y-1">
            {NAV_ITEMS.map((item) => (
              <li key={item.to}>
                <NavLink to={item.to} end={item.end} className={navLinkClass}>
                  {item.label}
                </NavLink>
              </li>
            ))}
          </ul>
        </nav>
        <AccountMenu />
      </header>

      {/* Mobile top bar */}
      <header className="sticky top-0 z-30 flex items-center justify-between border-b border-slate-200 bg-white px-4 py-3 lg:hidden">
        <p className="text-lg font-bold text-indigo-700">Habit Tracker</p>
        <div className="w-48">
          <AccountMenu />
        </div>
      </header>

      <main
        id="main-content"
        className="mx-auto w-full max-w-[1200px] px-4 pb-24 pt-6 sm:px-6 lg:pb-10 lg:pl-72 lg:pr-8 lg:pt-8"
      >
        <Outlet />
      </main>

      {/* Mobile bottom nav */}
      <nav
        aria-label="Primary"
        className="fixed inset-x-0 bottom-0 z-30 border-t border-slate-200 bg-white lg:hidden"
      >
        <ul className="flex">
          {NAV_ITEMS.map((item) => (
            <li key={item.to} className="flex-1">
              <NavLink
                to={item.to}
                end={item.end}
                className={({ isActive }) =>
                  `flex flex-col items-center gap-0.5 py-2 text-[11px] font-medium transition-colors
                  focus-visible:outline-2 focus-visible:outline-offset-1 focus-visible:outline-indigo-600
                  ${isActive ? "text-indigo-700" : "text-slate-500 hover:text-slate-800"}`
                }
              >
                {item.label}
              </NavLink>
            </li>
          ))}
        </ul>
      </nav>
    </div>
  );
}
