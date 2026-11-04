import { defineConfig } from '@playwright/test';

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
});
