export interface SelectOption {
  value: string;
  label: string;
}

export interface SelectProps extends React.SelectHTMLAttributes<HTMLSelectElement> {
  label: string;
  options: SelectOption[];
  error?: string;
  hint?: string;
}

export function Select({ label, options, error, hint, id, className = "", ...rest }: SelectProps) {
  const selectId = id ?? `select-${label.toLowerCase().replace(/[^a-z0-9]+/g, "-")}`;
  const errorId = `${selectId}-error`;
  const hintId = `${selectId}-hint`;
  const describedBy = error ? errorId : hint ? hintId : undefined;
  return (
    <div className={`flex flex-col gap-1.5 ${className}`}>
      <label htmlFor={selectId} className="text-sm font-medium text-slate-700">
        {label}
      </label>
      <select
        id={selectId}
        aria-invalid={error ? true : undefined}
        aria-describedby={describedBy}
        className={`rounded-lg border bg-white px-3 py-2 text-[15px] text-slate-900 transition-colors
          hover:border-slate-400
          ${error ? "border-red-500" : "border-slate-300 focus:border-indigo-600"}
          focus-visible:outline-2 focus-visible:outline-offset-1 focus-visible:outline-indigo-600`}
        {...rest}
      >
        {options.map((option) => (
          <option key={option.value} value={option.value}>
            {option.label}
          </option>
        ))}
      </select>
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
