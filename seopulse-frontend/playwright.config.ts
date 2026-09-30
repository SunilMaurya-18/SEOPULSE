import { defineConfig, devices } from '@playwright/test'

/*
 * Smoke tests run against a full stack: API + worker + Postgres + Redis, with
 * SEOPULSE_AUTH_REQUIRE_EMAIL_VERIFICATION=false so a fresh account can audit.
 * Set E2E_BASE_URL to test a deployed frontend; otherwise the Vite dev server
 * is started and proxies to the API on localhost:8082. E2E_IGNORE_HTTPS_ERRORS=1
 * accepts Caddy's internal certificate for the local production stack.
 */
const baseURL = process.env.E2E_BASE_URL ?? 'http://localhost:5173'

export default defineConfig({
  testDir: './e2e',
  timeout: 5 * 60_000,
  expect: { timeout: 15_000 },
  fullyParallel: false,
  retries: process.env.CI ? 1 : 0,
  reporter: process.env.CI ? [['github'], ['html', { open: 'never' }]] : 'list',
  use: {
    baseURL,
    ignoreHTTPSErrors: process.env.E2E_IGNORE_HTTPS_ERRORS === '1',
    actionTimeout: 15_000,
    navigationTimeout: 30_000,
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
  },
  projects: [{ name: 'chromium', use: { ...devices['Desktop Chrome'] } }],
  webServer: process.env.E2E_BASE_URL
    ? undefined
    : {
        command: 'npm run dev',
        url: baseURL,
        reuseExistingServer: true,
        timeout: 120_000,
      },
})
