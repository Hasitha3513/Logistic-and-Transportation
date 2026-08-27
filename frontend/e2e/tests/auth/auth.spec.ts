
import { test, expect } from '@playwright/test';
import { LoginPage } from '../../pages/LoginPage';
import { setupAuth } from '../../fixtures/authFixtures';
import { USERS } from '../../data/mockUsers';

test.describe('@rbac Authentication & Session Management', () => {
  test.beforeEach(async ({ page }) => {
    await setupAuth(page, USERS.ADMIN, false);
  });

  test('E2E-AUTH-001: Valid login redirects to dashboard', async ({ page }) => {
    const loginPage = new LoginPage(page);
    await loginPage.goto();
    await loginPage.login('admin@example.com', 'adminPassword');

    await expect(page.getByRole('heading', { name: 'Operations Overview' })).toBeVisible({ timeout: 15000 });
    await expect(page.locator('.user-menu')).toContainText('Admin User');
  });

  test('E2E-AUTH-002: Invalid credentials show error message', async ({ page }) => {
    const loginPage = new LoginPage(page);
    await loginPage.goto();
    await loginPage.login('wrong@example.com', 'badPass');

    await loginPage.expectErrorMessage('Sign-in failed. Check your username and password.');
  });

  test('E2E-AUTH-003: User session logout clears storage and redirects to sign-in', async ({ page }) => {
    const loginPage = new LoginPage(page);
    await loginPage.goto();
    await loginPage.login('admin@example.com', 'adminPassword');
    await expect(page.getByRole('heading', { name: 'Operations Overview' })).toBeVisible();

    await loginPage.logout();
  });
});
