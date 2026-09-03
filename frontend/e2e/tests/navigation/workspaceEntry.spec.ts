import { expect, setupAuth, test } from '../../fixtures/authFixtures';
import { USERS } from '../../data/mockUsers';

test.describe('Workspace entry', () => {
  test('authenticated user enters the permission-aware main application route', async ({ adminPage }) => {
    await adminPage.goto('/workspace');
    await expect(adminPage.getByRole('heading', { name: 'Workspace', level: 2 })).toBeVisible();

    await adminPage.getByRole('button', { name: /Open workspace/i }).click();

    await expect(adminPage).toHaveURL('/');
    await expect(adminPage.getByRole('heading', { name: 'Dashboard', level: 2 })).toBeVisible();
    await adminPage.reload();
    await expect(adminPage).toHaveURL('/');
    await expect(adminPage.getByRole('heading', { name: 'Dashboard', level: 2 })).toBeVisible();
  });

  test('unauthenticated access to the main route remains denied', async ({ page }) => {
    await setupAuth(page, USERS.ADMIN, false);
    await page.goto('/');

    await expect(page).toHaveURL(/\/login$/);
    await expect(page.getByRole('heading', { name: 'TransportOps' })).toBeVisible();
  });
});
