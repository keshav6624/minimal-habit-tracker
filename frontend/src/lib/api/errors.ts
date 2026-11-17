import type { FieldError } from "../../types/api";

/** Normalized error type shared by every API-facing hook/component. */
export class ApiError extends Error {
  readonly status: number;
  readonly type: string;
  readonly detail: string;
  readonly fieldErrors: FieldError[];
  readonly traceId?: string;

  constructor(params: {
    status: number;
    type: string;
    title: string;
    detail?: string;
    fieldErrors?: FieldError[];
    traceId?: string;
  }) {
    super(params.title);
    this.name = "ApiError";
    this.status = params.status;
    this.type = params.type;
    this.detail = params.detail ?? "";
    this.fieldErrors = params.fieldErrors ?? [];
    this.traceId = params.traceId;
  }

  get isAuthError(): boolean {
    return this.status === 401;
  }

  get isRateLimited(): boolean {
    return this.status === 429 || this.type === "RATE_LIMITED";
  }

  fieldErrorFor(field: string): string | undefined {
    return this.fieldErrors.find((fe) => fe.field === field)?.message;
  }
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null;
}

function isFieldErrorArray(value: unknown): value is FieldError[] {
  return (
    Array.isArray(value) &&
    value.every(
      (item) =>
        isRecord(item) && typeof item.field === "string" && typeof item.message === "string",
    )
  );
}

/** Convert a raw backend error envelope (or an unparseable response) into an ApiError. */
export function normalizeApiError(status: number, body: unknown): ApiError {
  if (isRecord(body) && typeof body.type === "string" && typeof body.title === "string") {
    return new ApiError({
      status,
      type: body.type,
      title: body.title,
      detail: typeof body.detail === "string" ? body.detail : undefined,
      fieldErrors: isFieldErrorArray(body.fieldErrors) ? body.fieldErrors : undefined,
      traceId: typeof body.traceId === "string" ? body.traceId : undefined,
    });
  }
  return new ApiError({
    status,
    type: status === 0 ? "NETWORK_ERROR" : "HTTP_ERROR",
    title: status === 0 ? "Network error" : `Request failed (HTTP ${status})`,
  });
}

/** Thrown when fetch itself rejects (offline, CORS, server down). */
export function networkError(): ApiError {
  return new ApiError({
    status: 0,
    type: "NETWORK_ERROR",
    title: "Network error",
    detail: "Could not reach the server. Check your connection and try again.",
  });
}

/** User-facing message for any thrown value. */
export function describeError(error: unknown): string {
  if (error instanceof ApiError) {
    if (error.isRateLimited) return "Too many requests. Please slow down and try again shortly.";
    if (error.detail) return error.detail;
    return error.message;
  }
  if (error instanceof Error) return error.message;
  return "Something went wrong. Please try again.";
}
