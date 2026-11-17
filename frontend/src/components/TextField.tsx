export interface TextFieldProps extends React.InputHTMLAttributes<HTMLInputElement> {
  label: string;
  /** Field-level error message from client validation or the API fieldErrors. */
  error?: string;
  hint?: string;
}

export function TextField({ label, error, hint, id, className = "", ...rest }: TextFieldProps) {
  const inputId = id ?? `field-${label.toLowerCase().replace(/[^a-z0-9]+/g, "-")}`;
  const errorId = `${inputId}-error`;
  const hintId = `${inputId}-hint`;
  const describedBy = error ? errorId : hint ? hintId : undefined;
  return (
    <div className={`flex flex-col gap-1.5 ${className}`}>
      <label htmlFor={inputId} className="text-sm font-medium text-slate-700">
        {label}
      </label>
      <input
        id={inputId}
        aria-invalid={error ? true : undefined}
        aria-describedby={describedBy}
        className={`rounded-lg border bg-white px-3 py-2 text-[15px] text-slate-900 transition-colors
          placeholder:text-slate-400 hover:border-slate-400
          ${error ? "border-red-500" : "border-slate-300 focus:border-indigo-600"}
          focus-visible:outline-2 focus-visible:outline-offset-1 focus-visible:outline-indigo-600`}
        {...rest}
      />
      {hint && !error ? (
        <p id={hintId} className="text-xs text-slate-500">{hint}</p>
      ) : null}
      {error ? (
        <p id={errorId} className="text-xs font-medium text-red-700">
          {error}
        </p>
      ) : null}
    </div>
  );
}
