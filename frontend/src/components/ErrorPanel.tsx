import { Button } from "./Button";

export interface ErrorPanelProps {
  title?: string;
  message: string;
  /** Called when the user clicks Retry. Omit to hide the button. */
  onRetry?: () => void;
}

/** Visible, actionable network/server error state. */
export function ErrorPanel({ title = "Something went wrong", message, onRetry }: ErrorPanelProps) {
  return (
    <section
      role="alert"
      aria-label={title}
      className="flex flex-col items-start gap-3 rounded-xl border border-red-200 bg-red-50 px-5 py-6"
    >
      <div>
        <h2 className="text-base font-semibold text-red-800">{title}</h2>
        <p className="mt-1 text-sm text-red-700">{message}</p>
      </div>
      {onRetry ? (
        <Button variant="secondary" size="sm" onClick={onRetry}>
          Retry
        </Button>
      ) : null}
    </section>
  );
}
