import { useCallback, useState } from "react";
import { api } from "../lib/api/client";
import { ApiError } from "../lib/api/errors";
import type { Habit, HabitInput } from "../types/api";

export interface HabitMutations {
  creating: boolean;
  updating: boolean;
  /** ApiError from the last failed create/update — carries inline fieldErrors. */
  lastValidationError: ApiError | null;
  createHabit: (input: HabitInput) => Promise<Habit>;
  updateHabit: (id: string, input: Partial<HabitInput>) => Promise<Habit>;
  archiveHabit: (id: string) => Promise<void>;
  restoreHabit: (id: string) => Promise<void>;
  deleteHabit: (id: string) => Promise<void>;
}

/**
 * Mutation helpers for habits. Callers handle toasts/refresh; thrown ApiErrors
 * propagate so forms can surface inline fieldErrors.
 */
export function useHabitMutations(): HabitMutations {
  const [creating, setCreating] = useState(false);
  const [updating, setUpdating] = useState(false);
  const [lastValidationError, setLastValidationError] = useState<ApiError | null>(null);

  const createHabit = useCallback(async (input: HabitInput): Promise<Habit> => {
    setCreating(true);
    setLastValidationError(null);
    try {
      return await api.post<Habit>("/habits", input);
    } catch (error) {
      if (error instanceof ApiError) setLastValidationError(error);
      throw error;
    } finally {
      setCreating(false);
    }
  }, []);

  const updateHabit = useCallback(async (id: string, input: Partial<HabitInput>): Promise<Habit> => {
    setUpdating(true);
    setLastValidationError(null);
    try {
      return await api.patch<Habit>(`/habits/${id}`, input);
    } catch (error) {
      if (error instanceof ApiError) setLastValidationError(error);
      throw error;
    } finally {
      setUpdating(false);
    }
  }, []);

  const archiveHabit = useCallback(async (id: string): Promise<void> => {
    await api.post<unknown>(`/habits/${id}/archive`);
  }, []);

  const restoreHabit = useCallback(async (id: string): Promise<void> => {
    await api.post<unknown>(`/habits/${id}/restore`);
  }, []);

  const deleteHabit = useCallback(async (id: string): Promise<void> => {
    await api.delete(`/habits/${id}`);
  }, []);

  return {
    creating,
    updating,
    lastValidationError,
    createHabit,
    updateHabit,
    archiveHabit,
    restoreHabit,
    deleteHabit,
  };
}
