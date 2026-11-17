export interface CheckboxProps extends React.InputHTMLAttributes<HTMLInputElement> {
  label: string;
  description?: string;
}

export function Checkbox({ label, description, id, className = "", ...rest }: CheckboxProps) {
  const checkboxId = id ?? `check-${label.toLowerCase().replace(/[^a-z0-9]+/g, "-")}`;
  return (
    <div className={`flex items-start gap-3 ${className}`}>
      <input
        id={checkboxId}
        type="checkbox"
        className="mt-1 size-4 rounded border-slate-300 text-indigo-600
          focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-indigo-600"
        {...rest}
      />
      <div className="flex flex-col gap-0.5">
        <label htmlFor={checkboxId} className="text-sm font-medium text-slate-700">
          {label}
        </label>
        {description ? <p className="text-xs text-slate-500">{description}</p> : null}
      </div>
    </div>
  );
}
