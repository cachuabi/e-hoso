import { defineConfig, devices } from '@playwright/test';

export default defineConfig({
  testDir: './e2e',
  timeout: 180_000,
  workers: 1,
  reporter: [['list'], ['html', { open: 'never', outputFolder: 'report' }]],
  use: {
    baseURL: process.env.BASE_URL ?? 'https://localhost',
    ignoreHTTPSErrors: true,
    screenshot: 'only-on-failure',
  },
  projects: [
    { name: 'chromium', use: { ...devices['Desktop Chrome'] } },
    // NFR-09: other engines run the compatibility smoke test only.
    { name: 'firefox', use: { ...devices['Desktop Firefox'] }, testMatch: /compat\.spec/ },
    { name: 'webkit', use: { ...devices['Desktop Safari'] }, testMatch: /compat\.spec/ },
  ],
});
