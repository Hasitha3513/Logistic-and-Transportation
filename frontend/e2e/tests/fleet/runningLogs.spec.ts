
import { test, expect } from '../../fixtures/authFixtures';
import { FleetVehiclesPage } from '../../pages/FleetVehiclesPage';

test.describe('@fleet Vehicle Running Logs & Mileage (US-06, US-33)', () => {
  const vehicleId = '32000000-0000-0000-0000-000000000001';

  test.beforeEach(async ({ fleetManagerPage }) => {
    await fleetManagerPage.route('**/api/vehicles', (r) => r.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify([{ id: vehicleId, registrationNumber: 'WP-CAB-1201', manufacturer: 'Isuzu', currentOdometerKm: 45000, active: true }]),
    }));
    await fleetManagerPage.route(`**/api/vehicles/${vehicleId}`, (r) => r.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ id: vehicleId, registrationNumber: 'WP-CAB-1201', manufacturer: 'Isuzu', currentOdometerKm: 45000, active: true }),
    }));

    await fleetManagerPage.route(`**/api/vehicles/${vehicleId}/readings`, (r) => r.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify([
        { id: 'rd-1', readingType: 'ODOMETER', readingValue: 45000, recordedAt: '2026-08-19T08:00:00Z' },
      ]),
    }));

    await fleetManagerPage.route(`**/api/vehicles/${vehicleId}/mileage`, (r) => r.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ totalDistanceKm: 12500, totalEngineHours: 420 }),
    }));
  });

  test('E2E-FLT-005: View latest odometer reading and mileage summary', async ({ fleetManagerPage }) => {
    const page = new FleetVehiclesPage(fleetManagerPage);
    await page.goto();
    await page.openDetails('WP-CAB-1201');
    await expect(fleetManagerPage.locator('.ant-drawer-body')).toContainText('45000');
  });
});
