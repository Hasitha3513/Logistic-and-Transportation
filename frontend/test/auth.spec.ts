import { test, expect } from '@playwright/test';
import { setupMockAuth } from './utils/mock';

test.describe('Authentication', () => {
  test('valid login redirects to dashboard', async ({ page }) => {
    await setupMockAuth(page);
    await page.goto('/login');
    await page.fill('input[name="username"]', 'admin@example.com');
    await page.fill('input[name="password"]', 'adminPassword');
    await page.click('button[type="submit"]');
    await expect(page.getByRole('heading', { name: 'Dashboard' })).toBeVisible({ timeout: 15000 });
    await expect(page.locator('.user-menu')).toContainText('Admin User', { timeout: 15000 });
  });

  test('invalid credentials show error message', async ({ page }) => {
    await setupMockAuth(page);
    await page.goto('/login');
    await page.fill('input[name="username"]', 'wrong@example.com');
    await page.fill('input[name="password"]', 'badPass');
    await page.click('button[type="submit"]');
    const error = page.locator('.ant-alert-error');
    await expect(error).toBeVisible();
    await expect(error).toContainText('Sign-in failed. Check your username and password.');
  });
});

