
import { test, expect } from '../../fixtures/authFixtures';
import { DriversPage } from '../../pages/DriversPage';

test.describe('@drivers Driver Performance Scorecards (US-41)', () => {
  const driverId = '40000000-0000-0000-0000-000000000001';

  test.beforeEach(async ({ fleetManagerPage }) => {
    await fleetManagerPage.route('**/api/drivers', (r) => r.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify([{ id: driverId, employeeNumber: 'DRV-001', firstName: 'Kasun', lastName: 'Fernando', status: 'AVAILABLE', active: true }]),
    }));

    await fleetManagerPage.route(`**/api/drivers/${driverId}/performance`, (r) => r.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        driverId,
        safetyScore: 94.5,
        totalTripsCompleted: 48,
        onTimeDeliveryRate: 98.0,
        fuelEfficiencyScore: 92.0,
      }),
    }));
  });

  test('E2E-DRV-005: View driver safety and performance metrics', async ({ fleetManagerPage }) => {
    const page = new DriversPage(fleetManagerPage);
    await page.goto();
    await page.openDetails('DRV-001');
    await expect(fleetManagerPage.locator('.ant-drawer-body')).toBeVisible();
  });
});
