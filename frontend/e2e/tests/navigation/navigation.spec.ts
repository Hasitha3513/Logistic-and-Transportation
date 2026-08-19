
import { test, expect } from '../../fixtures/authFixtures';

test.describe('@navigation Route Navigation & Deep Links', () => {
  test('E2E-NAV-001: Direct navigation to fleet deep link maintains breadcrumb', async ({ adminPage }) => {
    await adminPage.route('**/api/vehicles', (r) => r.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify([]) }));
    await adminPage.goto('/fleet/vehicles');
    await expect(adminPage.locator('.ant-breadcrumb')).toContainText('Fleet');
  });
});
