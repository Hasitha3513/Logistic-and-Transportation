
import { test as base, Page } from '@playwright/test';
import { USERS, TestUser } from '../data/mockUsers';

export async function setupAuth(page: Page, user: TestUser, autoLogin = true) {
  if (autoLogin) {
    await page.addInitScript(() => {
      localStorage.setItem('transport.accessToken', 'mock-access-token');
      localStorage.setItem('transport.refreshToken', 'mock-refresh-token');
    });
  }

  await page.route('**/api/auth/refresh', async (route) => {
    return route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ accessToken: 'mock-access-token', refreshToken: 'mock-refresh-token' }),
    });
  });

  await page.route('**/api/auth/logout', async (route) => {
    return route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ success: true }) });
  });

  await page.route('**/api/auth/login', async (route) => {
    const data = route.request().postDataJSON();
    if (data?.username === 'wrong@example.com' || data?.password === 'badPass') {
      return route.fulfill({ status: 401, body: JSON.stringify({ message: 'Invalid credentials' }) });
    }
    return route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ accessToken: 'mock-access-token', refreshToken: 'mock-refresh-token' }),
    });
  });

  await page.route('**/api/auth/me', async (route) => {
    if (autoLogin) {
      return route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(user) });
    }
    const authHeader = route.request().headers()['authorization'];
    if (!authHeader || !authHeader.startsWith('Bearer ')) {
      return route.fulfill({ status: 401, body: JSON.stringify({ message: 'Unauthorized' }) });
    }
    return route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(user) });
  });

  await page.route('**/api/dashboard/operations', async (route) => {
    return route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        date: '2026-08-19',
        status: 'READY',
        vehicles: { available: 12, allocated: 3, maintenance: 1, outOfService: 0, availabilityPercent: 80 },
        drivers: { available: 15, assigned: 3, availabilityPercent: 83.3 },
        trips: { draft: 2, pendingApproval: 1, approved: 2, assigned: 2, dispatched: 1, inProgress: 2, completed: 18, completionPercent: 90 },
      }),
    });
  });

  await page.route('**/api/customers', (r) => r.fulfill({
    status: 200, contentType: 'application/json', body: JSON.stringify([{ id: '10000000-0000-0000-0000-000000000001', name: 'Acme Distribution' }]),
  }));

  await page.route('**/api/locations', (r) => r.fulfill({
    status: 200, contentType: 'application/json', body: JSON.stringify([
      { id: '20000000-0000-0000-0000-000000000001', name: 'Colombo Hub', code: 'COL-01' },
      { id: '20000000-0000-0000-0000-000000000002', name: 'Kandy Depot', code: 'KND-01' },
    ]),
  }));

  await page.route('**/api/fuel-stations', (r) => r.fulfill({
    status: 200, contentType: 'application/json', body: JSON.stringify([
      { id: '70000000-0000-0000-0000-000000000001', name: 'Central Depot Station', code: 'STN-01' },
    ]),
  }));

  await page.route('**/api/vendors', (r) => r.fulfill({
    status: 200, contentType: 'application/json', body: JSON.stringify([
      { id: '80000000-0000-0000-0000-000000000001', name: 'Lanka IOC', code: 'VND-IOC' },
    ]),
  }));
}

type AuthFixtures = {
  adminPage: Page;
  fleetManagerPage: Page;
  dispatcherPage: Page;
  fuelOperatorPage: Page;
  viewerPage: Page;
};

export const test = base.extend<AuthFixtures>({
  adminPage: async ({ page }, use) => {
    await setupAuth(page, USERS.ADMIN);
    await use(page);
  },
  fleetManagerPage: async ({ page }, use) => {
    await setupAuth(page, USERS.FLEET_MANAGER);
    await use(page);
  },
  dispatcherPage: async ({ page }, use) => {
    await setupAuth(page, USERS.DISPATCHER);
    await use(page);
  },
  fuelOperatorPage: async ({ page }, use) => {
    await setupAuth(page, USERS.FUEL_OPERATOR);
    await use(page);
  },
  viewerPage: async ({ page }, use) => {
    await setupAuth(page, USERS.VIEWER);
    await use(page);
  },
});

export { expect } from '@playwright/test';
