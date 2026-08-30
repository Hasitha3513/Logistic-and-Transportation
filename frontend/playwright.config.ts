import { defineConfig, devices } from '@playwright/test';

const isWindows = process.platform === 'win32';
const mavenWrapper = isWindows ? '..\\mvnw.cmd' : '../mvnw';
const e2eAdminUsername = process.env.E2E_ADMIN_USERNAME ?? 'admin';
const e2eAdminPassword = process.env.E2E_ADMIN_PASSWORD ?? 'AdminPass!2026';
const e2eJwtSecret = process.env.E2E_JWT_SECRET ?? 'local-development-secret-change-me-32-bytes-minimum';
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
    baseURL: process.env.BASE_URL || 'http://localhost:5174',
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
      command: `${mavenWrapper} -q -f ../pom.xml spring-boot:run "-Dspring-boot.run.profiles=postgres,e2e" "-Dspring-boot.run.arguments=--server.port=8088 --app.dev.identity-bootstrap.enabled=true --app.dev.identity-bootstrap.username=${e2eAdminUsername} --app.dev.identity-bootstrap.password=${e2eAdminPassword} --app.dev.identity-bootstrap.email=e2e.admin@example.test --security.jwt.secret=${e2eJwtSecret} --app.dev.sample-data.enabled=true"`,
      url: 'http://localhost:8088/api/health',
      timeout: 180_000,
      reuseExistingServer: true,
      env: {
        ...process.env,
        JWT_SECRET: e2eJwtSecret,
        DEV_IDENTITY_BOOTSTRAP_ENABLED: 'true',
        DEV_IDENTITY_USERNAME: e2eAdminUsername,
        DEV_IDENTITY_PASSWORD: e2eAdminPassword,
        DEV_IDENTITY_EMAIL: 'e2e.admin@example.test',
        DEV_SAMPLE_DATA_ENABLED: 'true',
        JAVA_HOME: process.env.E2E_JAVA_HOME || process.env.JAVA_HOME || '/usr/lib/jvm/java-1.21.0-openjdk-amd64',
        PATH: `${process.env.E2E_JAVA_HOME || process.env.JAVA_HOME || '/usr/lib/jvm/java-1.21.0-openjdk-amd64'}/bin:${process.env.PATH || ''}`,
      },
    },
    {
      command: `"${process.execPath}" ./node_modules/vite/bin/vite.js --host localhost --port 5174`,
      url: 'http://localhost:5174/login',
      timeout: 120_000,
      reuseExistingServer: true,
      env: {
        ...process.env,
        BACKEND_URL: 'http://localhost:8088',
        PATH: `${process.env.PATH || ''}`,
      },
    },
  ],
});
