import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterAll, afterEach, beforeAll, describe, it, expect, vi } from "vitest";
import { BrowserRouter } from "react-router-dom";
import { SignUp } from "../src/features/auth/SignUp";
import { AuthProvider } from "../src/app/AuthContext";
import { ToastProvider } from "../src/app/ToastContext";

function jsonResponse(status: number, body: unknown): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}

const originalFetch = globalThis.fetch;

beforeAll(() => {
  document.cookie = "XSRF-TOKEN=test-csrf-token; path=/";
});

afterEach(() => {
  vi.restoreAllMocks();
});

afterAll(() => {
  globalThis.fetch = originalFetch;
});

function stubFetch(
  handler: (url: string, init?: RequestInit) => Response,
): void {
  const fetchMock = vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
    const url = typeof input === "string" ? input : input instanceof URL ? input.toString() : input.url;
    return handler(url, init);
  });
  vi.stubGlobal("fetch", fetchMock);
}

/** Default stub: session probe fails (anonymous) and everything else 500s loudly. */
function stubAnonymousFetch(
  extra: (url: string, init?: RequestInit) => Response | undefined = () => undefined,
): void {
  stubFetch((url, init) => {
    if (url.includes("/auth/csrf")) {
      return new Response(null, { status: 204 });
    }
    if (url.includes("/users/me") && (init?.method ?? "GET") === "GET") {
      return jsonResponse(401, { type: "UNAUTHENTICATED", title: "Unauthorized", status: 401 });
    }
    const handled = extra(url, init);
    if (handled) return handled;
    return jsonResponse(500, { type: "ERROR", title: "Unexpected", status: 500 });
  });
}

function renderSignUp() {
  return render(
    <BrowserRouter>
      <AuthProvider>
        <ToastProvider>
          <SignUp />
        </ToastProvider>
      </AuthProvider>
    </BrowserRouter>,
  );
}

describe("SignUp validation", () => {
  it("shows required errors for an empty submit", async () => {
    stubAnonymousFetch();
    const user = userEvent.setup();
    renderSignUp();
    await user.click(await screen.findByRole("button", { name: "Create account" }));

    expect(await screen.findByText("Email is required.")).toBeInTheDocument();
    expect(screen.getByText("Password is required.")).toBeInTheDocument();
  });

  it("shows format and length errors", async () => {
    stubAnonymousFetch();
    const user = userEvent.setup();
    renderSignUp();
    await user.type(await screen.findByLabelText("Email"), "not-an-email");
    await user.type(screen.getByLabelText("Password"), "short");
    await user.click(screen.getByRole("button", { name: "Create account" }));

    expect(await screen.findByText("Enter a valid email address.")).toBeInTheDocument();
    expect(
      screen.getByText("Password must be at least 8 characters."),
    ).toBeInTheDocument();
  });

  it("maps a 409 duplicate-email response to the email field", async () => {
    stubAnonymousFetch((url) => {
      if (url.includes("/auth/register")) {
        return jsonResponse(409, {
          type: "EMAIL_TAKEN",
          title: "Email already registered",
          status: 409,
        });
      }
      return undefined;
    });
    const user = userEvent.setup();
    renderSignUp();
    await user.type(await screen.findByLabelText("Email"), "taken@example.com");
    await user.type(screen.getByLabelText("Password"), "longenough1");
    await user.click(screen.getByRole("button", { name: "Create account" }));

    expect(
      await screen.findByText("An account with this email already exists."),
    ).toBeInTheDocument();
  });

  it("maps server fieldErrors to the matching inputs", async () => {
    stubAnonymousFetch((url) => {
      if (url.includes("/auth/register")) {
        return jsonResponse(400, {
          type: "VALIDATION",
          title: "Validation failed",
          status: 400,
          fieldErrors: [{ field: "password", message: "Password is too weak." }],
        });
      }
      return undefined;
    });
    const user = userEvent.setup();
    renderSignUp();
    await user.type(await screen.findByLabelText("Email"), "new@example.com");
    await user.type(screen.getByLabelText("Password"), "12345678");
    await user.click(screen.getByRole("button", { name: "Create account" }));

    await waitFor(() =>
      expect(screen.getByText("Password is too weak.")).toBeInTheDocument(),
    );
  });
});
