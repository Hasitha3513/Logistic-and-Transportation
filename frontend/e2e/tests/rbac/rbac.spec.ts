
import { test, expect } from '../../fixtures/authFixtures';

test.describe('@rbac Role-Based Access Control Navigation (US-74)', () => {
  test('E2E-RBAC-001: Admin sees all management menus', async ({ adminPage }) => {
    await adminPage.goto('/');
    const menu = adminPage.locator('.ant-menu');
    await expect(menu).toBeVisible();
    await expect(menu.locator('.ant-menu-submenu, .ant-menu-item').filter({ hasText: 'Fleet' })).toBeVisible();
    await expect(menu.locator('.ant-menu-submenu, .ant-menu-item').filter({ hasText: 'Drivers' })).toBeVisible();
  });

  test('E2E-RBAC-002: Viewer does not see driver management menu', async ({ viewerPage }) => {
    await viewerPage.goto('/');
    const menu = viewerPage.locator('.ant-menu');
    await expect(menu).toBeVisible();
    await expect(menu.locator('.ant-menu-submenu, .ant-menu-item').filter({ hasText: 'Drivers' })).toHaveCount(0);
  });
});
