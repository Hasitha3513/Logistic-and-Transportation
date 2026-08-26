
import { test, expect } from '../../fixtures/authFixtures';
import { DashboardPage } from '../../pages/DashboardPage';

test.describe('@reporting Operations Reporting & Audit (US-75)', () => {
  test('E2E-RPT-001: View operations dashboard summary statistics', async ({ adminPage }) => {
    const page = new DashboardPage(adminPage);
    await page.goto();
    await page.expectDashboardLoaded();
    await expect(page.vehiclesCard).toContainText('80%');
  });
});
