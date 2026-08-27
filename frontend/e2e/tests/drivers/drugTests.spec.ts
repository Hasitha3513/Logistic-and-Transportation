import { test, expect } from '../../fixtures/authFixtures';
import { DriversPage } from '../../pages/DriversPage';

test.describe('@drivers Driver Substance Screening & Drug Tests (US-44)', () => {
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
        safetyScore: 95.0,
        totalTripsCompleted: 10,
        tripCompletionRate: 100.0,
        totalViolations: 0,
        totalPenaltyPoints: 0,
        criticalViolations: 0,
        totalFines: 0,
        unpaidFines: 0,
        overallRating: 'EXCELLENT',
        evaluatedAt: '2026-08-19',
      }),
    }));

    await fleetManagerPage.route(`**/api/drivers/${driverId}/violations`, (r) => r.fulfill({
      status: 200, contentType: 'application/json', body: JSON.stringify([]),
    }));

    await fleetManagerPage.route(`**/api/drivers/${driverId}/exceptions`, (r) => r.fulfill({
      status: 200, contentType: 'application/json', body: JSON.stringify([]),
    }));

    await fleetManagerPage.route(`**/api/drivers/${driverId}/licenses`, (r) => r.fulfill({
      status: 200, contentType: 'application/json', body: JSON.stringify([]),
    }));

    await fleetManagerPage.route(`**/api/drivers/${driverId}/medical-records`, (r) => r.fulfill({
      status: 200, contentType: 'application/json', body: JSON.stringify([]),
    }));

    await fleetManagerPage.route(`**/api/drivers/${driverId}/drug-tests`, (r) => r.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify([
        {
          id: 'dt-1',
          driverId,
          testType: 'RANDOM',
          scheduledDate: '2026-06-01',
          result: 'NEGATIVE',
          status: 'COMPLETED',
          laboratoryOrProvider: 'LabCorp',
          referenceNumber: 'REF-DT-100',
          returnToDutyRequired: false,
          active: true,
        },
      ]),
    }));
  });

  test('E2E-DRV-008: View driver substance screening records and results in drawer', async ({ fleetManagerPage }) => {
    const page = new DriversPage(fleetManagerPage);
    await page.goto();
    await page.openDetails('DRV-001');
    await expect(fleetManagerPage.locator('.ant-drawer-body')).toContainText('Substance Screening & Drug Tests');
    await expect(fleetManagerPage.locator('.ant-drawer-body')).toContainText('RANDOM');
    await expect(fleetManagerPage.locator('.ant-drawer-body')).toContainText('NEGATIVE');
  });
});
