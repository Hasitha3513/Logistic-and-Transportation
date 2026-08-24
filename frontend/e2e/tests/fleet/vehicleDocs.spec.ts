
import { test, expect } from '../../fixtures/authFixtures';
import { FleetVehiclesPage } from '../../pages/FleetVehiclesPage';

test.describe('@fleet Vehicle Documents & Compliance (US-03, US-83)', () => {
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

    await fleetManagerPage.route(`**/api/vehicles/${vehicleId}/documents`, (r) => r.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify([
        { id: 'doc-1', documentType: 'INSURANCE', documentNumber: 'INS-2026-999', validFrom: '2026-01-01', validTo: '2027-01-01', active: true },
      ]),
    }));
  });

  test('E2E-FLT-003: View vehicle compliance insurance document in drawer', async ({ fleetManagerPage }) => {
    const page = new FleetVehiclesPage(fleetManagerPage);
    await page.goto();
    await page.openDetails('WP-CAB-1201');
    await expect(fleetManagerPage.locator('.ant-drawer-body')).toContainText('INS-2026-999');
  });
});
