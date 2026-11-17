import { useCallback, useEffect, useState } from "react";
import { api } from "../lib/api/client";
import { describeError } from "../lib/api/errors";
import type { AnalyticsSummary, HabitAnalyticsDetail } from "../types/api";
import type { DateKey } from "../lib/dates";

export interface AnalyticsState {
  summary: AnalyticsSummary | null;
  loading: boolean;
  error: string | null;
  refetch: () => void;
}

/** GET /analytics/summary?from&to */
export function useAnalytics(from: DateKey, to: DateKey): AnalyticsState {
  const [summary, setSummary] = useState<AnalyticsSummary | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [tick, setTick] = useState(0);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    setError(null);
    api
      .get<AnalyticsSummary>("/analytics/summary", { from, to })
      .then((result) => {
        if (!cancelled) {
          setSummary(result);
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
  }, [from, to, tick]);

  const refetch = useCallback(() => setTick((t) => t + 1), []);

  return { summary, loading, error, refetch };
}

export interface HabitAnalyticsState {
  detail: HabitAnalyticsDetail | null;
  loading: boolean;
  error: string | null;
  refetch: () => void;
}

/** GET /analytics/habits/{id}?from&to */
export function useHabitAnalytics(habitId: string, from: DateKey, to: DateKey): HabitAnalyticsState {
  const [detail, setDetail] = useState<HabitAnalyticsDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [tick, setTick] = useState(0);

  useEffect(() => {
    if (!habitId) {
      setDetail(null);
      setLoading(false);
      return;
    }
    let cancelled = false;
    setLoading(true);
    setError(null);
    api
      .get<HabitAnalyticsDetail>(`/analytics/habits/${habitId}`, { from, to })
      .then((result) => {
        if (!cancelled) {
          setDetail(result);
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
  }, [habitId, from, to, tick]);

  const refetch = useCallback(() => setTick((t) => t + 1), []);

  return { detail, loading, error, refetch };
}
