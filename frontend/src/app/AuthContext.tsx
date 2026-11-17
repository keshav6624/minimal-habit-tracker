import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from "react";
import { api, setUnauthorizedHandler } from "../lib/api/client";
import type { Settings, User } from "../types/api";

type AuthStatus = "loading" | "authenticated" | "anonymous";

interface AuthContextValue {
  status: AuthStatus;
  user: User | null;
  settings: Settings | null;
  signIn: (email: string, password: string) => Promise<void>;
  signUp: (input: { email: string; password: string; displayName?: string; timezone?: string }) => Promise<void>;
  signOut: () => Promise<void>;
  setSettings: (settings: Settings) => void;
  /** Wipe local auth state (used after account deletion). */
  clearAuth: () => void;
}

const AuthContext = createContext<AuthContextValue | null>(null);

function redirectToLogin(): void {
  if (window.location.pathname !== "/login") {
    window.location.assign("/login");
  }
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [status, setStatus] = useState<AuthStatus>("loading");
  const [user, setUser] = useState<User | null>(null);
  const [settings, setSettingsState] = useState<Settings | null>(null);

  const loadSettings = useCallback(async (u: User): Promise<void> => {
    try {
      const s = await api.get<Settings>("/users/me/settings");
      setSettingsState(s);
    } catch {
      // Settings are optional; fall back to the user's own timezone.
      setSettingsState({
        timezone: u.timezone,
        weekStartsOn: 1,
        remindersEnabled: false,
      });
    }
  }, []);

  useEffect(() => {
    let cancelled = false;
    (async () => {
      try {
        const me = await api.get<User>("/users/me", undefined, { skipAuthRedirect: true });
        if (!cancelled) {
          setUser(me);
          setStatus("authenticated");
          void loadSettings(me);
        }
      } catch {
        if (!cancelled) {
          setUser(null);
          setStatus("anonymous");
        }
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [loadSettings]);

  useEffect(() => {
    setUnauthorizedHandler(() => {
      setUser(null);
      setSettingsState(null);
      setStatus("anonymous");
      redirectToLogin();
    });
    return () => {
      setUnauthorizedHandler(null);
    };
  }, []);

  const signIn = useCallback(
    async (email: string, password: string): Promise<void> => {
      const me = await api.post<User>(
        "/auth/login",
        { email, password },
        { skipAuthRedirect: true },
      );
      setUser(me);
      setStatus("authenticated");
      void loadSettings(me);
    },
    [loadSettings],
  );

  const signUp = useCallback(
    async (input: {
      email: string;
      password: string;
      displayName?: string;
      timezone?: string;
    }): Promise<void> => {
      const me = await api.post<User>("/auth/register", input, { skipAuthRedirect: true });
      setUser(me);
      setStatus("authenticated");
      void loadSettings(me);
    },
    [loadSettings],
  );

  const signOut = useCallback(async (): Promise<void> => {
    try {
      await api.post<void>("/auth/logout", undefined, { skipAuthRedirect: true });
    } finally {
      setUser(null);
      setSettingsState(null);
      setStatus("anonymous");
    }
  }, []);

  const clearAuth = useCallback((): void => {
    setUser(null);
    setSettingsState(null);
    setStatus("anonymous");
  }, []);

  const setSettings = useCallback((next: Settings): void => {
    setSettingsState(next);
  }, []);

  const value = useMemo<AuthContextValue>(
    () => ({ status, user, settings, signIn, signUp, signOut, setSettings, clearAuth }),
    [status, user, settings, signIn, signUp, signOut, setSettings, clearAuth],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used within AuthProvider");
  return ctx;
}
