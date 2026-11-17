import { expect, test, type Page } from "@playwright/test";

/**
 * Full user journey: register → create habit → complete → calendar → export → sign out.
 * Requires the backend dev server on :8080 (proxied via /api) and Playwright
 * browsers installed locally. Not part of unit checks or the build.
 */

const EMAIL_DOMAIN = "e2e.habit.test";
const uniqueEmail = () =>
  `e2e+${Date.now()}-${Math.floor(Math.random() * 10_000)}@${EMAIL_DOMAIN}`;

async function register(page: Page, email: string): Promise<void> {
  await page.goto("/signup");
  await page.getByLabel("Email").fill(email);
  await page.getByLabel("Password", { exact: true }).fill("correct-horse-battery");
  await page.getByRole("button", { name: "Create account" }).click();
  await expect(page.getByRole("heading", { name: /Good (morning|afternoon|evening|night)/ })).toBeVisible();
}

test("register → create habit → complete → calendar → export → sign out", async ({ page }) => {
  const email = uniqueEmail();
  await register(page, email);

  // Create a habit
  await page.getByRole("link", { name: "Habits" }).first().click();
  await page.getByRole("button", { name: "+ New habit" }).click();
  await page.getByLabel("Name", { exact: true }).fill("E2E smoke habit");
  await page.getByRole("button", { name: "Create habit" }).click();
  await expect(page.getByText('Habit "E2E smoke habit" created.')).toBeVisible();
  await expect(page.getByText("E2E smoke habit").first()).toBeVisible();

  // Complete it from the dashboard (≤2 interactions)
  await page.getByRole("link", { name: "Overview" }).first().click();
  const toggle = page.getByRole("button", { name: /E2E smoke habit — mark complete for/ });
  await toggle.click();
  await expect(toggle).toHaveAttribute("aria-pressed", "true");

  // Calendar shows the completed day
  await page.getByRole("link", { name: "Calendar" }).first().click();
  const todayLabel = new Intl.DateTimeFormat("en-US", {
    year: "numeric",
    month: "long",
    day: "numeric",
  }).format(new Date());
  await expect(
    page.getByRole("button", { name: new RegExp(`${todayLabel}, 1 of 1 habits completed`) }),
  ).toBeVisible();
  await page.getByRole("button", { name: new RegExp(`${todayLabel}, 1 of 1 habits completed`) }).click();
  await expect(page.getByText(`${todayLabel}: 1 of 1 habits completed.`)).toBeVisible();

  // Export downloads a JSON file
  const downloadPromise = page.waitForEvent("download");
  await page.getByRole("button", { name: "Export data (JSON)" }).click();
  const download = await downloadPromise;
  expect(download.suggestedFilename()).toBe("habit-tracker-export.json");

  // Sign out
  await page.getByRole("button", { name: "Account menu" }).click();
  await page.getByRole("menuitem", { name: "Sign out" }).click();
  await expect(page).toHaveURL(/\/login$/);
});
