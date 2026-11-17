import type { ToastItem } from "../app/ToastContext";

export interface ToastProps {
  toasts: ToastItem[];
  onDismiss: (id: number) => void;
}

/** Polite live region so screen readers announce toasts without interrupting. */
export function Toast({ toasts, onDismiss }: ToastProps) {
  return (
    <div
      aria-live="polite"
      aria-atomic="false"
      className="pointer-events-none fixed inset-x-0 bottom-4 z-50 flex flex-col items-center gap-2 px-4 sm:bottom-6"
    >
      {toasts.map((toast) => (
        <div
          key={toast.id}
          className={`pointer-events-auto flex w-full max-w-sm items-center gap-2 rounded-lg px-4 py-3 text-sm shadow-lg
            ${toast.kind === "success" ? "bg-emerald-700 text-white" : "bg-red-700 text-white"}`}
        >
          <span aria-hidden="true" className="text-base">
            {toast.kind === "success" ? "✓" : "⚠"}
          </span>
          <p className="flex-1">{toast.message}</p>
          <button
            type="button"
            onClick={() => onDismiss(toast.id)}
            aria-label="Dismiss notification"
            className="rounded-md p-1 hover:bg-white/20 focus-visible:outline-2 focus-visible:outline-offset-1 focus-visible:outline-white"
          >
            ✕
          </button>
        </div>
      ))}
    </div>
  );
}
