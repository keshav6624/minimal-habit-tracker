import { useId, useState } from "react";

export interface PasswordFieldProps extends React.InputHTMLAttributes<HTMLInputElement> {
  label: string;
  /** Field-level error message from client validation or the API fieldErrors. */
  error?: string;
  hint?: string;
}

/** Text field for passwords with a show/hide toggle. */
export function PasswordField({ label, error, hint, id, className = "", ...rest }: PasswordFieldProps) {
  const [visible, setVisible] = useState(false);
  const generatedId = useId();
  const inputId = id ?? generatedId;
  const errorId = `${inputId}-error`;
  const hintId = `${inputId}-hint`;
  const describedBy = error ? errorId : hint ? hintId : undefined;
  return (
    <div className={`flex flex-col gap-1.5 ${className}`}>
      <label htmlFor={inputId} className="text-sm font-medium text-slate-700">
        {label}
      </label>
      <div className="relative">
        <input
          id={inputId}
          type={visible ? "text" : "password"}
          aria-invalid={error ? true : undefined}
          aria-describedby={describedBy}
          className={`w-full rounded-lg border bg-white px-3 py-2 pr-14 text-[15px] text-slate-900 transition-colors
            placeholder:text-slate-400 hover:border-slate-400
            ${error ? "border-red-500" : "border-slate-300 focus:border-indigo-600"}
            focus-visible:outline-2 focus-visible:outline-offset-1 focus-visible:outline-indigo-600`}
          {...rest}
        />
        <button
          type="button"
          onClick={() => setVisible((v) => !v)}
          aria-pressed={visible}
          aria-label={visible ? `Hide password` : `Show password`}
          className="absolute inset-y-0 right-1.5 my-auto h-7 rounded-md px-2 text-xs font-medium text-slate-600
            hover:bg-slate-100 focus-visible:outline-2 focus-visible:outline-offset-1 focus-visible:outline-indigo-600"
        >
          {visible ? "Hide" : "Show"}
        </button>
      </div>
      {hint && !error ? <p id={hintId} className="text-xs text-slate-500">{hint}</p> : null}
      {error ? (
        <p id={errorId} className="text-xs font-medium text-red-700">
          {error}
        </p>
      ) : null}
    </div>
  );
}
