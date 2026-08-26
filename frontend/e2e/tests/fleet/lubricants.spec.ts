import { test, expect } from '../../fixtures/authFixtures';
import { FleetVehiclesPage } from '../../pages/FleetVehiclesPage';

test.describe('@fleet Vehicle Lubricant & Fluid Consumption Logs (US-05)', () => {
  const vehicleId = '32000000-0000-0000-0000-000000000001';

  test.beforeEach(async ({ fleetManagerPage }) => {
    const vehicleData = {
      id: vehicleId,
      registrationNumber: 'WP-CAB-1201',
      manufacturer: 'Isuzu',
      operationalStatus: 'AVAILABLE',
      active: true,
    };

    await fleetManagerPage.route('**/api/vehicles', (r) =>
      r.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify([vehicleData]),
      })
    );

    await fleetManagerPage.route(`**/api/vehicles/${vehicleId}`, (r) =>
      r.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(vehicleData),
      })
    );

    await fleetManagerPage.route(`**/api/vehicles/${vehicleId}/documents`, (r) =>
      r.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify([]),
      })
    );

    await fleetManagerPage.route(`**/api/vehicles/${vehicleId}/maintenance-schedules`, (r) =>
      r.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify([]),
      })
    );

    await fleetManagerPage.route(`**/api/vehicles/${vehicleId}/readings`, (r) =>
      r.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify([]),
      })
    );

    let logs = [
      {
        id: 'lub-1',
        vehicleId,
        fluidType: 'ENGINE_OIL',
        quantity: 15.0,
        unit: 'LITRE',
        recordedAt: '2026-02-15T10:00:00Z',
        odometerKm: 52000,
        engineHours: 1400,
        vendorId: null,
        supplierName: 'Mobil Lubricants',
        referenceNumber: 'REF-LUB-001',
        remarks: 'Scheduled 50k oil change',
        active: true,
        createdAt: '2026-02-15T10:00:00Z',
        updatedAt: '2026-02-15T10:00:00Z',
        createdBy: 'fleet.manager',
        updatedBy: 'fleet.manager',
      },
    ];

    await fleetManagerPage.route(`**/api/vehicles/${vehicleId}/lubricant-logs`, async (r) => {
      if (r.request().method() === 'GET') {
        return r.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify(logs),
        });
      }

      if (r.request().method() === 'POST') {
        const payload = r.request().postDataJSON();
        const created = {
          id: 'lub-2',
          vehicleId,
          ...payload,
          active: true,
          createdAt: new Date().toISOString(),
          updatedAt: new Date().toISOString(),
          createdBy: 'fleet.manager',
          updatedBy: 'fleet.manager',
        };
        logs = [created, ...logs];
        return r.fulfill({
          status: 201,
          contentType: 'application/json',
          body: JSON.stringify(created),
        });
      }

      return r.continue();
    });
  });

  test('E2E-FLT-006: View lubricant consumption records and log new fluid record', async ({
    fleetManagerPage,
  }) => {
    const page = new FleetVehiclesPage(fleetManagerPage);
    await page.goto();
    await page.openDetails('WP-CAB-1201');

    const drawer = fleetManagerPage.locator('.ant-drawer-body');
    await expect(drawer).toContainText('Lubricant & Fluid Consumption Logs');
    await expect(drawer).toContainText('Engine Oil');
    await expect(drawer).toContainText('15 litre');
    await expect(drawer).toContainText('Mobil Lubricants');

    // Click Add Lubricant Log button
    const addButton = drawer.getByRole('button', { name: /Add Lubricant \/ Fluid Log/i });
    await expect(addButton).toBeVisible();
    await addButton.click();

    const modal = fleetManagerPage.locator('.ant-modal-content');
    await expect(modal).toBeVisible();
    await expect(modal).toContainText('Record Lubricant / Fluid Consumption');

    // Fill form
    await modal.locator('.ant-select').first().click();
    await fleetManagerPage.locator('.ant-select-dropdown').getByText('Coolant').click();

    const qtyInput = modal.locator('input#quantity');
    await qtyInput.fill('6.5');

    const supplierInput = modal.locator('input#supplierName');
    await supplierInput.fill('Caltex Coolant Service');

    const refInput = modal.locator('input#referenceNumber');
    await refInput.fill('INV-COOL-009');

    // Submit modal
    await modal.getByRole('button', { name: /OK/i }).click();

    // Verify modal closes and new record is displayed
    await expect(modal).not.toBeVisible();
    await expect(drawer).toContainText('Coolant');
    await expect(drawer).toContainText('6.5 litre');
  });

  test('E2E-FLT-006-NEG: Vehicle lubricant log form validates positive quantity and required fields', async ({
    fleetManagerPage,
  }) => {
    const page = new FleetVehiclesPage(fleetManagerPage);
    await page.goto();
    await page.openDetails('WP-CAB-1201');

    const drawer = fleetManagerPage.locator('.ant-drawer-body');
    const addButton = drawer.getByRole('button', { name: /Add Lubricant \/ Fluid Log/i });
    await addButton.click();

    const modal = fleetManagerPage.locator('.ant-modal-content');
    await expect(modal).toBeVisible();

    // Click OK without filling required fields
    await modal.getByRole('button', { name: /OK/i }).click();

    await expect(modal).toContainText('Please select a fluid type');
    await expect(modal).toContainText('Please enter quantity');

    // Fill non-positive quantity
    const qtyInput = modal.locator('input#quantity');
    await qtyInput.fill('0');
    await modal.getByRole('button', { name: /OK/i }).click();

    await expect(modal).toContainText('Quantity must be greater than zero');
  });
});

