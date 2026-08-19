import { test, expect } from '@playwright/test';
import { mockAdminUser, mockRegularUser, setupMockAuth } from './utils/mock';

test.describe('Role-Based Access Control', () => {
  test('admin sees driver management menu', async ({ page }) => {
    await setupMockAuth(page, mockAdminUser);
    await page.goto('/login');
    await page.fill('input[name="username"]', 'admin@example.com');
    await page.fill('input[name="password"]', 'adminPassword');
    await page.click('button[type="submit"]');
    await expect(page.getByRole('heading', { name: 'Dashboard' })).toBeVisible({ timeout: 15000 });
    const navItem = page.locator('.ant-menu-submenu, .ant-menu-item').filter({ hasText: 'Drivers' });
    await expect(navItem).toBeAttached({ timeout: 10000 });
  });

  test('regular user does not see driver management menu', async ({ page }) => {
    await setupMockAuth(page, mockRegularUser);
    await page.goto('/login');
    await page.fill('input[name="username"]', 'user@example.com');
    await page.fill('input[name="password"]', 'userPassword');
    await page.click('button[type="submit"]');
    await expect(page.getByRole('heading', { name: 'Dashboard' })).toBeVisible({ timeout: 15000 });
    const navItem = page.locator('.ant-menu-submenu, .ant-menu-item').filter({ hasText: 'Drivers' });
    await expect(navItem).toHaveCount(0);
  });
});

