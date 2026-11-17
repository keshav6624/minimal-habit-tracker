import { defineConfig, devices } from "@playwright/test";

/**
 * E2E journeys. Browsers are NOT installed on this machine (kept light);
 * run `npx playwright install` once before `npx playwright test`.
 * Build/unit checks never require Playwright.
 */
export default defineConfig({
  testDir: "./e2e",
  timeout: 30_000,
  fullyParallel: false,
  retries: 0,
  use: {
    baseURL: "http://localhost:5173",
    trace: "retain-on-failure",
  },
  projects: [
    {
      name: "chromium",
      use: { ...devices["Desktop Chrome"] },
    },
  ],
  webServer: {
    command: "npm run dev -- --strictPort",
    url: "http://localhost:5173",
    reuseExistingServer: true,
    timeout: 60_000,
  },
});
