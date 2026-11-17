import { useCallback, useEffect, useState } from "react";
import { api } from "../lib/api/client";
import { describeError } from "../lib/api/errors";
import type { Habit, Page } from "../types/api";

export interface HabitListState {
  habits: Habit[];
  totalElements: number;
  totalPages: number;
  page: number;
  loading: boolean;
  error: string | null;
  refetch: () => void;
}

/** GET /habits?status=…&page&size — tiny fetch hook with loading/error/refetch. */
export function useHabits(
  status: "active" | "archived",
  page = 0,
  size = 50,
): HabitListState {
  const [data, setData] = useState<Page<Habit> | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [tick, setTick] = useState(0);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    setError(null);
    api
      .get<Page<Habit>>("/habits", { status, page, size })
      .then((result) => {
        if (!cancelled) {
          setData(result);
          setLoading(false);
        }
      })
      .catch((cause: unknown) => {
        if (!cancelled) {
          setError(describeError(cause));
          setLoading(false);
        }
      });
    return () => {
      cancelled = true;
    };
  }, [status, page, size, tick]);

  const refetch = useCallback(() => setTick((t) => t + 1), []);

  return {
    habits: data?.content ?? [],
    totalElements: data?.totalElements ?? 0,
    totalPages: data?.totalPages ?? 0,
    page: data?.page ?? page,
    loading,
    error,
    refetch,
  };
}
