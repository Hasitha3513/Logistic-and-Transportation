import { defineConfig, devices } from '@playwright/test';
import { randomBytes } from 'node:crypto';

const isWindows = process.platform === 'win32';
const mavenWrapper = isWindows ? '..\\mvnw.cmd' : '../mvnw';
const e2eAdminUsername = process.env.E2E_ADMIN_USERNAME ?? `e2e.admin.${process.pid}`;
const e2eAdminPassword = process.env.E2E_ADMIN_PASSWORD ?? `E2e!${randomBytes(18).toString('base64url')}`;
const e2eJwtSecret = process.env.E2E_JWT_SECRET ?? randomBytes(48).toString('base64url');
const e2eWorkers = Number(process.env.E2E_WORKERS ?? 3);

process.env.E2E_ADMIN_USERNAME = e2eAdminUsername;
process.env.E2E_ADMIN_PASSWORD = e2eAdminPassword;

export default defineConfig({
  testDir: './e2e/tests',
  timeout: 45_000,
  expect: {
    timeout: 10_000,
  },
  fullyParallel: true,
  workers: Number.isInteger(e2eWorkers) && e2eWorkers > 0 ? e2eWorkers : 3,
  retries: 0,
  reporter: [
    ['list'],
    ['html', { open: 'never', outputFolder: 'playwright-report' }],
  ],
  use: {
    baseURL: process.env.BASE_URL || 'http://localhost:5173',
    headless: true,
    viewport: { width: 1280, height: 720 },
    ignoreHTTPSErrors: true,
    trace: 'on-first-retry',
    screenshot: 'only-on-failure',
    video: 'retain-on-failure'
  },
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] }
    },
    {
      name: 'firefox',
      use: { ...devices['Desktop Firefox'] }
    },
    {
      name: 'webkit',
      use: { ...devices['Desktop Safari'] }
    }
  ],
  webServer: [
    {
      command: `${mavenWrapper} -q -f ../pom.xml spring-boot:run -Dspring-boot.run.profiles=h2,e2e`,
      url: 'http://localhost:8080/api/health',
      timeout: 180_000,
      reuseExistingServer: false,
      env: {
        ...process.env,
        JWT_SECRET: e2eJwtSecret,
        DEV_IDENTITY_BOOTSTRAP_ENABLED: 'true',
        DEV_IDENTITY_USERNAME: e2eAdminUsername,
        DEV_IDENTITY_PASSWORD: e2eAdminPassword,
        DEV_IDENTITY_EMAIL: 'e2e.admin@example.test',
        DEV_SAMPLE_DATA_ENABLED: 'true',
        ...(process.env.E2E_JAVA_HOME ? { JAVA_HOME: process.env.E2E_JAVA_HOME } : {}),
      },
    },
    {
      command: 'npm run dev -- --host localhost',
      url: 'http://localhost:5173/login',
      timeout: 120_000,
      reuseExistingServer: false,
    },
  ],
});
