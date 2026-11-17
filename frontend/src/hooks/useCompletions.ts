import { useCallback, useEffect, useMemo, useState } from "react";
import { api } from "../lib/api/client";
import { describeError } from "../lib/api/errors";
import type { CompletionEntry, CompletionPutResponse } from "../types/api";
import type { DateKey } from "../lib/dates";

export interface CompletionsState {
  /** Set of date keys (in the requested range) that are completed. */
  completedDates: Set<DateKey>;
  loading: boolean;
  error: string | null;
  refetch: () => void;
  /** Optimistically toggle a date; throws so callers can toast on failure. */
  setCompleted: (date: DateKey, completed: boolean) => Promise<void>;
}

/** GET /habits/{id}/completions?from&to + optimistic PUT toggling with rollback. */
export function useCompletions(habitId: string, from: DateKey, to: DateKey): CompletionsState {
  const [entries, setEntries] = useState<Map<DateKey, boolean>>(new Map());
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [tick, setTick] = useState(0);

  useEffect(() => {
    if (!habitId) {
      setEntries(new Map());
      setLoading(false);
      return;
    }
    let cancelled = false;
    setLoading(true);
    setError(null);
    api
      .get<{ from: string; to: string; completions: CompletionEntry[] }>(`/habits/${habitId}/completions`, {
        from,
        to,
      })
      .then((result) => {
        if (cancelled) return;
        const map = new Map<DateKey, boolean>();
        for (const entry of result.completions) {
          map.set(entry.localDate, entry.completed);
        }
        setEntries(map);
        setLoading(false);
      })
      .catch((cause: unknown) => {
        if (cancelled) return;
        setError(describeError(cause));
        setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [habitId, from, to, tick]);

  const setCompleted = useCallback(
    async (date: DateKey, completed: boolean): Promise<void> => {
      if (!habitId) throw new Error("No habit selected");
      // Optimistic update; roll back if the server rejects it.
      const previous = entries;
      setEntries((current) => new Map(current).set(date, completed));
      try {
        await api.put<CompletionPutResponse>(`/habits/${habitId}/completions/${date}`, {
          completed,
        });
      } catch (cause) {
        setEntries(previous);
        throw cause;
      }
    },
    [habitId, entries],
  );

  const completedDates = useMemo(() => {
    const set = new Set<DateKey>();
    for (const [key, value] of entries) {
      if (value) set.add(key);
    }
    return set;
  }, [entries]);

  const refetch = useCallback(() => setTick((t) => t + 1), []);

  return { completedDates, loading, error, refetch, setCompleted };
}
