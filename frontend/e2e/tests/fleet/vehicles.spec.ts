
import { test, expect } from '../../fixtures/authFixtures';
import { FleetVehiclesPage } from '../../pages/FleetVehiclesPage';

test.describe('@fleet Vehicle Master & Fleet Categories (US-01, US-02)', () => {
  let vehiclesList: Record<string, unknown>[] = [];

  test.beforeEach(async ({ fleetManagerPage }) => {
    vehiclesList = [
      { id: '32000000-0000-0000-0000-000000000001', registrationNumber: 'WP-CAB-1201', manufacturer: 'Isuzu', model: 'NPR', operationalStatus: 'AVAILABLE', active: true },
    ];

    await fleetManagerPage.route('**/api/vehicle-categories', (r) => r.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify([{ id: 'cat-1', name: 'Trucks' }]) }));
    await fleetManagerPage.route('**/api/vehicle-types', (r) => r.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify([{ id: 'type-1', categoryId: 'cat-1', name: 'Box Truck' }]) }));
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

  test('E2E-FLT-003: Duplicate registration returns conflict error and maps to form', async ({ fleetManagerPage }) => {
    await fleetManagerPage.route('**/api/vehicles', async (r) => {
      if (r.request().method() === 'POST') {
        return r.fulfill({
          status: 409,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 'VEHICLE_REGISTRATION_DUPLICATE',
            message: 'Vehicle with registration number WP-CAB-1201 already exists',
          }),
        });
      }
      return r.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(vehiclesList) });
    });

    const page = new FleetVehiclesPage(fleetManagerPage);
    await page.goto();
    await page.openCreateModal();
    await fleetManagerPage.fill('#resource-registrationNumber', 'WP-CAB-1201');
    await fleetManagerPage.click('.ant-modal-footer button.ant-btn-primary');
    const errorText = fleetManagerPage.locator('.resource-editor-error');
    await expect(errorText.first()).toBeVisible({ timeout: 10000 });
  });

  test('E2E-FLT-004: View vehicle details drawer', async ({ fleetManagerPage }) => {
    await fleetManagerPage.route('**/api/vehicles/32000000-0000-0000-0000-000000000001', (r) =>
      r.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          id: '32000000-0000-0000-0000-000000000001',
          registrationNumber: 'WP-CAB-1201',
          manufacturer: 'Isuzu',
          model: 'NPR',
          operationalStatus: 'AVAILABLE',
          active: true,
        }),
      })
    );
    await fleetManagerPage.route('**/api/vehicles/32000000-0000-0000-0000-000000000001/documents', (r) =>
      r.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify([]) })
    );

    const page = new FleetVehiclesPage(fleetManagerPage);
    await page.goto();
    await page.openDetails('WP-CAB-1201');
    await expect(page.detailsDrawer).toBeVisible();
    await expect(page.detailsDrawer).toContainText('WP-CAB-1201');
  });
});

