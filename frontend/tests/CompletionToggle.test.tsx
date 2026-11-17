import { render, screen, waitFor, fireEvent } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { vi, describe, it, expect } from "vitest";
import { CompletionToggle } from "../src/features/habits/CompletionToggle";

const DATE = "2026-11-16";
const PRETTY = "November 16, 2026";

describe("CompletionToggle", () => {
  it("renders unchecked initially and announces completion via aria-live", async () => {
    const onToggle = vi.fn().mockResolvedValue(undefined);
    render(
      <CompletionToggle
        habitId="h1"
        habitName="Read"
        date={DATE}
        completed={false}
        onToggle={onToggle}
      />,
    );

    const button = screen.getByRole("button", { name: `Read — mark complete for ${PRETTY}` });
    expect(button).toHaveAttribute("aria-pressed", "false");

    // Synchronous click so we can observe the optimistic flip before promises settle.
    fireEvent.click(button);

    // Optimistic flip happens immediately.
    expect(
      screen.getByRole("button", { name: `Read — mark not complete for ${PRETTY}` }),
    ).toHaveAttribute("aria-pressed", "true");

    await waitFor(() => expect(onToggle).toHaveBeenCalledWith("h1", DATE, true));

    const live = screen.getByText(new RegExp(`Read marked complete for ${PRETTY}`, "i"));
    expect(live).toHaveAttribute("aria-live", "polite");
  });

  it("rolls back the optimistic state when the server rejects", async () => {
    const onToggle = vi.fn().mockRejectedValue(new Error("409 archived"));
    render(
      <CompletionToggle
        habitId="h2"
        habitName="Run"
        date={DATE}
        completed={false}
        onToggle={onToggle}
      />,
    );

    // Synchronous click to observe the optimistic state first.
    fireEvent.click(screen.getByRole("button", { name: `Run — mark complete for ${PRETTY}` }));

    // Optimistically flipped…
    expect(
      screen.getByRole("button", { name: `Run — mark not complete for ${PRETTY}` }),
    ).toHaveAttribute("aria-pressed", "true");

    // …then rolled back to unchecked after the failure.
    await waitFor(() =>
      expect(
        screen.getByRole("button", { name: `Run — mark complete for ${PRETTY}` }),
      ).toHaveAttribute("aria-pressed", "false"),
    );

    // Error announced politely.
    expect(
      screen.getByText(/Could not update Run for November 16, 2026/i),
    ).toBeInTheDocument();
  });

  it("announces unchecking a completed habit", async () => {
    const onToggle = vi.fn().mockResolvedValue(undefined);
    const user = userEvent.setup();
    render(
      <CompletionToggle
        habitId="h3"
        habitName="Meditate"
        date={DATE}
        completed={true}
        onToggle={onToggle}
      />,
    );

    await user.click(
      screen.getByRole("button", { name: `Meditate — mark not complete for ${PRETTY}` }),
    );
    await waitFor(() => expect(onToggle).toHaveBeenCalledWith("h3", DATE, false));
    expect(
      screen.getByText(new RegExp(`Meditate marked not complete for ${PRETTY}`, "i")),
    ).toBeInTheDocument();
  });

  it("does nothing when disabled", async () => {
    const onToggle = vi.fn();
    const user = userEvent.setup();
    render(
      <CompletionToggle
        habitId="h4"
        habitName="Stretch"
        date={DATE}
        completed={false}
        disabled
        onToggle={onToggle}
      />,
    );
    await user.click(screen.getByRole("button", { name: /Stretch/ }));
    expect(onToggle).not.toHaveBeenCalled();
  });
});
