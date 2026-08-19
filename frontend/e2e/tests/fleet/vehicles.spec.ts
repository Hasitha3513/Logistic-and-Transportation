
import { test, expect } from '../../fixtures/authFixtures';
import { FleetVehiclesPage } from '../../pages/FleetVehiclesPage';

test.describe('@fleet Vehicle Master & Fleet Categories (US-01, US-02)', () => {
  let vehiclesList: any[] = [];

  test.beforeEach(async ({ fleetManagerPage }) => {
    vehiclesList = [
      { id: '32000000-0000-0000-0000-000000000001', registrationNumber: 'WP-CAB-1201', manufacturer: 'Isuzu', model: 'NPR', operationalStatus: 'AVAILABLE', active: true },
    ];

    await fleetManagerPage.route('**/api/vehicle-categories', (r) => r.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify([{ id: 'cat-1', name: 'Trucks' }]) }));
    await fleetManagerPage.route('**/api/vehicle-types', (r) => r.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify([{ id: 'type-1', name: 'Box Truck' }]) }));
    await fleetManagerPage.route('**/api/vehicles', async (r) => {
      if (r.request().method() === 'POST') {
        const body = r.request().postDataJSON();
        const created = { id: '32000000-0000-0000-0000-000000000099', ...body, active: true };
        vehiclesList.push(created);
        return r.fulfill({ status: 201, contentType: 'application/json', body: JSON.stringify(created) });
      }
      return r.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(vehiclesList) });
    });
  });

  test('E2E-FLT-001: Create vehicle master record with valid payload', async ({ fleetManagerPage }) => {
    const page = new FleetVehiclesPage(fleetManagerPage);
    await page.goto();
    await page.createVehicle({ registrationNumber: 'WP-ABC-9999', manufacturer: 'Toyota', model: 'Dyna' });
    await expect(page.vehicleTable).toContainText('Toyota');
  });

  test('E2E-FLT-002: Vehicle creation form validates required fields', async ({ fleetManagerPage }) => {
    const page = new FleetVehiclesPage(fleetManagerPage);
    await page.goto();
    await page.openCreateModal();
    await fleetManagerPage.click('.ant-modal-footer button.ant-btn-primary');
    const errors = fleetManagerPage.locator('.resource-editor-error');
    await expect(errors.first()).toBeVisible({ timeout: 10000 });
  });
});
