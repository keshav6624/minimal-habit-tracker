import { normalizeApiError, networkError } from "./errors";
import type { FieldError } from "../../types/api";

const API_BASE = "/api/v1";

const UNSAFE_METHODS = new Set(["POST", "PUT", "PATCH", "DELETE"]);

export type QueryParams = Record<string, string | number | boolean | undefined>;

export interface ApiRequestOptions {
  method?: "GET" | "POST" | "PUT" | "PATCH" | "DELETE";
  body?: unknown;
  query?: QueryParams;
  /** "json" (default) | "blob" | "none" (expect empty body) */
  expect?: "json" | "blob" | "none";
  /**
   * Set for endpoints that legitimately return 401 (login, /users/me probe)
   * so we do not clear auth state or redirect on a normal anonymous visit.
   */
  skipAuthRedirect?: boolean;
}

let unauthorizedHandler: (() => void) | null = null;

/** Registered by AuthContext: clears auth state and redirects to /login. */
export function setUnauthorizedHandler(handler: (() => void) | null): void {
  unauthorizedHandler = handler;
}

function readCookie(name: string): string | null {
  const prefix = `${name}=`;
  for (const piece of document.cookie.split(";")) {
    const trimmed = piece.trim();
    if (trimmed.startsWith(prefix)) {
      return decodeURIComponent(trimmed.slice(prefix.length));
    }
  }
  return null;
}

/**
 * Fetch a fresh CSRF token before any unsafe request.
 * The endpoint sets the XSRF-TOKEN cookie; we echo it in X-XSRF-TOKEN.
 */
async function ensureCsrfToken(): Promise<string> {
  let response: Response;
  try {
    response = await fetch(`${API_BASE}/auth/csrf`, {
      method: "GET",
      credentials: "include",
    });
  } catch {
    throw networkError();
  }
  if (!response.ok) {
    throw normalizeApiError(response.status, null);
  }
  const token = readCookie("XSRF-TOKEN");
  if (!token) {
    throw new Error("CSRF token missing from response cookies");
  }
  return token;
}

function buildUrl(path: string, query?: QueryParams): string {
  const url = `${API_BASE}${path.startsWith("/") ? path : `/${path}`}`;
  if (!query) return url;
  const search = new URLSearchParams();
  for (const [key, value] of Object.entries(query)) {
    if (value !== undefined) search.set(key, String(value));
  }
  const qs = search.toString();
  return qs ? `${url}?${qs}` : url;
}

export async function apiRequest<T>(
  path: string,
  options: ApiRequestOptions = {},
): Promise<T> {
  const method = options.method ?? "GET";
  const headers: Record<string, string> = { Accept: "application/json" };
  let body: string | undefined;
  if (options.body !== undefined) {
    headers["Content-Type"] = "application/json";
    body = JSON.stringify(options.body);
  }
  if (UNSAFE_METHODS.has(method)) {
    headers["X-XSRF-TOKEN"] = await ensureCsrfToken();
  }

  let response: Response;
  try {
    response = await fetch(buildUrl(path, options.query), {
      method,
      headers,
      credentials: "include",
      body,
    });
  } catch {
    throw networkError();
  }

  if (response.status === 401 && !options.skipAuthRedirect) {
    unauthorizedHandler?.();
  }

  if (!response.ok) {
    let payload: unknown = null;
    try {
      payload = await response.json();
    } catch {
      // Body was empty or not JSON; normalize below with a generic envelope.
    }
    throw normalizeApiError(response.status, payload);
  }

  if (options.expect === "none" || response.status === 204) {
    return undefined as T;
  }
  if (options.expect === "blob") {
    return (await response.blob()) as T;
  }
  const text = await response.text();
  if (!text) return undefined as T;
  try {
    return JSON.parse(text) as T;
  } catch (cause) {
    throw new Error(`Unexpected non-JSON response from ${path}`, { cause });
  }
}

export const api = {
  get<T>(
    path: string,
    query?: QueryParams,
    options?: Pick<ApiRequestOptions, "skipAuthRedirect">,
  ): Promise<T> {
    return apiRequest<T>(path, { method: "GET", query, ...options });
  },
  post<T>(path: string, body?: unknown, options?: Pick<ApiRequestOptions, "skipAuthRedirect">): Promise<T> {
    return apiRequest<T>(path, { method: "POST", body, ...options });
  },
  put<T>(path: string, body?: unknown): Promise<T> {
    return apiRequest<T>(path, { method: "PUT", body });
  },
  patch<T>(path: string, body?: unknown): Promise<T> {
    return apiRequest<T>(path, { method: "PATCH", body });
  },
  delete(path: string, body?: unknown): Promise<void> {
    return apiRequest<void>(path, { method: "DELETE", body, expect: "none" });
  },
  /** Fetch any endpoint as a Blob (e.g. export downloads). */
  blob(path: string, query?: QueryParams): Promise<Blob> {
    return apiRequest<Blob>(path, { method: "GET", query, expect: "blob" });
  },
};

export function fieldErrorsOf(error: unknown): FieldError[] {
  return error instanceof Error && "fieldErrors" in error
    ? (error as { fieldErrors?: FieldError[] }).fieldErrors ?? []
    : [];
}

/** Trigger a browser download of a Blob under a suggested file name. */
export function downloadBlob(blob: Blob, fileName: string): void {
  const url = URL.createObjectURL(blob);
  try {
    const anchor = document.createElement("a");
    anchor.href = url;
    anchor.download = fileName;
    document.body.append(anchor);
    anchor.click();
    anchor.remove();
  } finally {
    URL.revokeObjectURL(url);
  }
}
