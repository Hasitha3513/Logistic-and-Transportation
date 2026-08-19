import type { Page } from '@playwright/test';

export const mockAdminUser = {
  id: '00000000-0000-0000-0000-000000000001',
  username: 'admin',
  email: 'admin@example.com',
  firstName: 'Admin',
  lastName: 'User',
  active: true,
  roles: ['ADMIN'],
  permissions: [
    'DASHBOARD_VIEW',
    'VEHICLE_VIEW',
    'VEHICLE_CREATE',
    'VEHICLE_UPDATE',
    'VEHICLE_STATUS_UPDATE',
    'VEHICLE_DOCUMENT_MANAGE',
    'DRIVER_VIEW',
    'DRIVER_CREATE',
    'DRIVER_UPDATE',
    'DRIVER_LICENSE_MANAGE',
    'ROUTE_VIEW',
    'ROUTE_CREATE',
    'ROUTE_UPDATE',
    'TRIP_VIEW',
    'TRIP_CREATE',
    'TRIP_UPDATE',
    'TRIP_SUBMIT',
    'TRIP_APPROVE',
    'TRIP_REJECT',
    'TRIP_DISPATCH',
    'TRIP_START',
    'TRIP_COMPLETE',
    'TRIP_CLOSE',
    'TRIP_CANCEL',
    'FUEL_ISSUE_VIEW',
    'FUEL_PURCHASE_VIEW',
    'BUNKER_VIEW',
    'FUEL_PRICE_VIEW',
    'IDENTITY_MANAGE',
  ],
};

export const mockRegularUser = {
  id: '00000000-0000-0000-0000-000000000002',
  username: 'user',
  email: 'user@example.com',
  firstName: 'Regular',
  lastName: 'User',
  active: true,
  roles: ['VIEWER'],
  permissions: ['DASHBOARD_VIEW'],
};

export async function setupMockAuth(page: Page, user = mockAdminUser, autoLogin = false) {
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
      body: JSON.stringify({
        accessToken: 'mock-access-token',
        refreshToken: 'mock-refresh-token',
      }),
    });
  });

  await page.route('**/api/auth/logout', async (route) => {
    return route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ success: true }),
    });
  });

  await page.route('**/api/auth/login', async (route) => {
    const postData = route.request().postDataJSON();
    if (postData?.username === 'wrong@example.com' || postData?.password === 'badPass') {
      return route.fulfill({ status: 401, body: JSON.stringify({ message: 'Invalid credentials' }) });
    }
    return route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        accessToken: 'mock-access-token',
        refreshToken: 'mock-refresh-token',
      }),
    });
  });

  await page.route('**/api/auth/me', async (route) => {
    if (autoLogin) {
      return route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(user),
      });
    }
    const authHeader = route.request().headers()['authorization'];
    if (!authHeader || !authHeader.startsWith('Bearer ')) {
      return route.fulfill({
        status: 401,
        contentType: 'application/json',
        body: JSON.stringify({ message: 'Unauthorized' }),
      });
    }
    return route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify(user),
    });
  });

  await page.route('**/api/dashboard/operations', async (route) => {
    return route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        date: '2026-08-19',
        status: 'READY',
        vehicles: { available: 5, allocated: 2, maintenance: 1, outOfService: 0, availabilityPercent: 62.5 },
        drivers: { available: 8, assigned: 2, availabilityPercent: 80 },
        trips: { draft: 1, pendingApproval: 1, approved: 1, assigned: 1, dispatched: 1, inProgress: 1, completed: 5, completionPercent: 83.3 },
      }),
    });
  });
}
