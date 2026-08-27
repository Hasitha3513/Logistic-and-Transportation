
import { test, expect } from '../../fixtures/authFixtures';
import { DriversPage } from '../../pages/DriversPage';

test.describe('@drivers Driver Violations Management (US-42)', () => {
  const driverId = '40000000-0000-0000-0000-000000000001';

  test.beforeEach(async ({ fleetManagerPage }) => {
    await fleetManagerPage.route('**/api/drivers', (r) => r.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify([{ id: driverId, employeeNumber: 'DRV-001', firstName: 'Kasun', lastName: 'Fernando', status: 'AVAILABLE', active: true }]),
    }));

    await fleetManagerPage.route(`**/api/drivers/${driverId}/violations`, (r) => r.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify([
        { id: 'v-1', driverId, violationType: 'SPEEDING', fineAmount: 150.0, paymentStatus: 'UNPAID', incidentDate: '2026-08-15' },
      ]),
    }));
  });

  test('E2E-DRV-004: View driver violations in driver details drawer', async ({ fleetManagerPage }) => {
    const page = new DriversPage(fleetManagerPage);
    await page.goto();
    await page.openDetails('DRV-001');
    await expect(fleetManagerPage.locator('.ant-drawer-body')).toContainText('SPEEDING');
  });
});
