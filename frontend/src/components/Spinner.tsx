export function Spinner({ className = "", label }: { className?: string; label?: string }) {
  return (
    <span
      className={`inline-block size-5 animate-spin rounded-full border-2 border-slate-300 border-t-indigo-600 align-middle ${className}`}
      role="status"
      aria-label={label ?? "Loading"}
    />
  );
}
