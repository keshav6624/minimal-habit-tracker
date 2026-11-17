export interface EmptyStateProps {
  icon?: string;
  title: string;
  /** Explains the first step the user should take. */
  description: string;
  action?: React.ReactNode;
}

export function EmptyState({ icon = "🌱", title, description, action }: EmptyStateProps) {
  return (
    <section
      aria-label="No content yet"
      className="flex flex-col items-center gap-3 rounded-xl border border-slate-200 bg-white px-6 py-12 text-center"
    >
      <span aria-hidden="true" className="text-4xl">{icon}</span>
      <h2 className="text-lg font-semibold text-slate-800">{title}</h2>
      <p className="max-w-sm text-sm text-slate-600">{description}</p>
      {action ? <div className="mt-2">{action}</div> : null}
    </section>
  );
}
