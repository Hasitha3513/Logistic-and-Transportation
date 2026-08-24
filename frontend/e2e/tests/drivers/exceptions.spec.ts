
import { test, expect } from '../../fixtures/authFixtures';
import { DriversPage } from '../../pages/DriversPage';

test.describe('@drivers Driver Leave & Availability Exceptions (US-45)', () => {
  const driverId = '40000000-0000-0000-0000-000000000001';

  test.beforeEach(async ({ fleetManagerPage }) => {
    await fleetManagerPage.route('**/api/drivers', (r) => r.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify([{ id: driverId, employeeNumber: 'DRV-001', firstName: 'Kasun', lastName: 'Fernando', status: 'AVAILABLE', active: true }]),
    }));

    await fleetManagerPage.route(`**/api/drivers/${driverId}/exceptions`, (r) => r.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify([
        { id: 'ex-1', driverId, exceptionType: 'MEDICAL_LEAVE', status: 'ACTIVE', startDate: '2026-08-25', endDate: '2026-08-27', reason: 'Medical Checkup' },
      ]),
    }));
  });

  test('E2E-DRV-006: View driver exception window in details drawer', async ({ fleetManagerPage }) => {
    const page = new DriversPage(fleetManagerPage);
    await page.goto();
    await page.openDetails('DRV-001');
    await expect(fleetManagerPage.locator('.ant-drawer-body')).toContainText('MEDICAL_LEAVE');
  });
});
