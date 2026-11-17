import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterAll, afterEach, describe, it, expect, vi } from "vitest";
import { CalendarPage } from "../src/features/calendar/CalendarPage";
import { ToastProvider } from "../src/app/ToastContext";

function jsonResponse(status: number, body: unknown): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}

const originalFetch = globalThis.fetch;

afterEach(() => {
  vi.restoreAllMocks();
});

afterAll(() => {
  globalThis.fetch = originalFetch;
});

function stubCalendarFetch(): void {
  const fetchMock = vi.fn(async (input: RequestInfo | URL) => {
    const url = typeof input === "string" ? input : input instanceof URL ? input.toString() : input.url;
    if (url.includes("/habits?status=")) {
      return jsonResponse(200, {
        content: [
          {
            id: "h1",
            name: "Read",
            description: null,
            color: "#4f46e5",
            icon: "📚",
            scheduleType: "DAILY",
            scheduleDays: [],
            startDate: "2026-01-01",
            archivedAt: null,
            createdAt: "2026-01-01T00:00:00Z",
            updatedAt: "2026-01-01T00:00:00Z",
          },
        ],
        page: 0,
        size: 50,
        totalElements: 1,
        totalPages: 1,
      });
    }
    if (url.includes("/habits/h1/completions")) {
      return jsonResponse(200, {
        from: "2026-11-01",
        to: "2026-11-30",
        completions: [
          { localDate: "2026-11-15", completed: true, completedAt: "2026-11-15T10:00:00Z" },
        ],
      });
    }
    return jsonResponse(500, { type: "ERROR", title: "Unexpected", status: 500 });
  });
  vi.stubGlobal("fetch", fetchMock);
}

function renderCalendar() {
  return render(
    <ToastProvider>
      <CalendarPage />
    </ToastProvider>,
  );
}

describe("CalendarPage", () => {
  it("renders the month grid with accessible day buttons", async () => {
    stubCalendarFetch();
    renderCalendar();

    // Day buttons expose per-day completion counts in their labels.
    const day = await screen.findByRole("button", {
      name: /November 15, 2026, 1 of 1 habits completed/,
    });
    expect(day).toBeInTheDocument();
    expect(
      screen.getByRole("button", { name: /November 16, 2026, 0 of 1 habits completed/ }),
    ).toBeInTheDocument();
  });

  it("is keyboard operable: focusing a day and pressing Enter selects it and shows a textual summary", async () => {
    stubCalendarFetch();
    const user = userEvent.setup();
    renderCalendar();

    const day = await screen.findByRole("button", {
      name: /November 15, 2026, 1 of 1 habits completed/,
    });
    day.focus();
    expect(day).toHaveFocus();
    await user.keyboard("{Enter}");

    await waitFor(() => {
      expect(screen.getByText(/November 15, 2026: 1 of 1 habits completed/i)).toBeInTheDocument();
    });
    expect(screen.getByText(/Completed: Read/i)).toBeInTheDocument();
  });

  it("navigates between months with the labelled prev/next buttons", async () => {
    stubCalendarFetch();
    const user = userEvent.setup();
    renderCalendar();

    // Wait for the grid to load before reading the heading.
    await screen.findAllByRole("button", { name: /, \d+ of 1 habits completed/ });

    // The month heading updates via prev/next; keep it date-independent.
    const heading = () =>
      screen.getByText(/^(January|February|March|April|May|June|July|August|September|October|November|December) \d{4}$/);
    const before = heading().textContent;

    const prev = screen.getByRole("button", { name: "Previous month" });
    const next = screen.getByRole("button", { name: "Next month" });
    expect(prev).toBeInTheDocument();
    expect(next).toBeInTheDocument();

    await user.click(next);
    await waitFor(() => expect(heading().textContent).not.toBe(before));

    await user.click(prev);
    await waitFor(() => expect(heading().textContent).toBe(before));
  });

  it("shows an empty state when there are no habits", async () => {
    const fetchMock = vi.fn(async (input: RequestInfo | URL) => {
      const url = typeof input === "string" ? input : input instanceof URL ? input.toString() : input.url;
      if (url.includes("/habits")) {
        return jsonResponse(200, {
          content: [],
          page: 0,
          size: 50,
          totalElements: 0,
          totalPages: 0,
        });
      }
      return jsonResponse(500, { type: "ERROR", title: "Unexpected", status: 500 });
    });
    vi.stubGlobal("fetch", fetchMock);

    renderCalendar();
    expect(await screen.findByText("No habits to show yet")).toBeInTheDocument();
  });
});
