import { test, expect } from '@playwright/test';
import { mockAdminUser, setupMockAuth } from './utils/mock';

test.describe('Global Error Handling', () => {
  test('displays error banner on server failure', async ({ page }) => {
    await setupMockAuth(page, mockAdminUser);

    // Mock a 500 error for /api/drivers
    await page.route('**/api/drivers', (route) => {
      return route.fulfill({
        status: 500,
        body: JSON.stringify({ message: 'Internal Server Error' }),
        contentType: 'application/json',
      });
    });

    await page.goto('/login');
    await page.fill('input[name="username"]', 'admin@example.com');
    await page.fill('input[name="password"]', 'adminPassword');
    await page.click('button[type="submit"]');
    await expect(page.getByRole('heading', { name: 'Dashboard' })).toBeVisible({ timeout: 15000 });

    await page.goto('/drivers');
    const errorBanner = page.locator('.ant-alert-error');
    await expect(errorBanner).toBeVisible({ timeout: 10000 });
    await expect(errorBanner).toContainText('Driver registry could not be loaded');

    // Remove 500 error and provide valid response
    await page.unroute('**/api/drivers');
    await page.route('**/api/drivers', (route) => {
      return route.fulfill({
        status: 200,
        body: JSON.stringify([
          {
            id: '40000000-0000-0000-0000-000000000001',
            employeeNumber: 'DRV-001',
            firstName: 'Kasun',
            lastName: 'Fernando',
            phone: '+94 77 555 1001',
            email: 'kasun.fernando@example.test',
            status: 'AVAILABLE',
            active: true,
          },
        ]),
        contentType: 'application/json',
      });
    });

    await page.click('button:has-text("Refresh")');
    const table = page.locator('table');
    await expect(table).toBeVisible();
    await expect(table).toContainText('Kasun Fernando');
  });
});

