import { render, screen } from "@testing-library/react";
import { describe, it, expect } from "vitest";
import { EmptyState } from "../src/components/EmptyState";
import { Button } from "../src/components/Button";

describe("EmptyState", () => {
  it("renders title, description and no action by default", () => {
    render(
      <EmptyState
        title="No habits yet"
        description="Create your first habit to get started."
      />,
    );
    expect(screen.getByRole("heading", { name: "No habits yet" })).toBeInTheDocument();
    expect(
      screen.getByText("Create your first habit to get started."),
    ).toBeInTheDocument();
    expect(screen.queryByRole("button")).not.toBeInTheDocument();
  });

  it("renders an optional action", async () => {
    render(
      <EmptyState
        title="No habits yet"
        description="Create your first habit to get started."
        action={<Button>Create habit</Button>}
      />,
    );
    expect(screen.getByRole("button", { name: "Create habit" })).toBeInTheDocument();
  });

  it("shows the icon decoration", () => {
    render(
      <EmptyState icon="📅" title="Empty calendar" description="Nothing here yet." />,
    );
    expect(screen.getByText("📅")).toBeInTheDocument();
  });
});
