
import { test, expect } from '../../fixtures/authFixtures';
import { FleetVehiclesPage } from '../../pages/FleetVehiclesPage';

test.describe('@fleet Vehicle Maintenance Scheduling (US-07)', () => {
  const vehicleId = '32000000-0000-0000-0000-000000000001';

  test.beforeEach(async ({ fleetManagerPage }) => {
    await fleetManagerPage.route('**/api/vehicles', (r) => r.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify([{ id: vehicleId, registrationNumber: 'WP-CAB-1201', manufacturer: 'Isuzu', operationalStatus: 'AVAILABLE', active: true }]),
    }));
    await fleetManagerPage.route(`**/api/vehicles/${vehicleId}`, (r) => r.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ id: vehicleId, registrationNumber: 'WP-CAB-1201', manufacturer: 'Isuzu', operationalStatus: 'AVAILABLE', active: true }),
    }));

    await fleetManagerPage.route(`**/api/vehicles/${vehicleId}/maintenance-schedules`, (r) => r.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify([
        { id: 'ms-1', vehicleId, scheduleType: 'PREVENTIVE', status: 'SCHEDULED', scheduledStart: '2026-08-28T08:00:00Z', scheduledEnd: '2026-08-29T18:00:00Z', description: '50K Service' },
      ]),
    }));
  });

  test('E2E-FLT-004: View active maintenance schedule window in vehicle details', async ({ fleetManagerPage }) => {
    const page = new FleetVehiclesPage(fleetManagerPage);
    await page.goto();
    await page.openDetails('WP-CAB-1201');
    await expect(fleetManagerPage.locator('.ant-drawer-body')).toContainText('50K Service');
  });
});
