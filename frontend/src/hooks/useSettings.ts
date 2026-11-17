import { useCallback, useEffect, useState } from "react";
import { api } from "../lib/api/client";
import { describeError } from "../lib/api/errors";
import type { Settings } from "../types/api";

export interface SettingsState {
  settings: Settings | null;
  loading: boolean;
  error: string | null;
  refetch: () => void;
  saveSettings: (next: Settings) => Promise<Settings>;
}

/** GET/PUT /users/me/settings. */
export function useSettings(): SettingsState {
  const [settings, setSettingsLocal] = useState<Settings | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [tick, setTick] = useState(0);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    setError(null);
    api
      .get<Settings>("/users/me/settings")
      .then((result) => {
        if (!cancelled) {
          setSettingsLocal(result);
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
  }, [tick]);

  const saveSettings = useCallback(async (next: Settings): Promise<Settings> => {
    const saved = await api.put<Settings>("/users/me/settings", next);
    setSettingsLocal(saved);
    return saved;
  }, []);

  const refetch = useCallback(() => setTick((t) => t + 1), []);

  return { settings, loading, error, refetch, saveSettings };
}
