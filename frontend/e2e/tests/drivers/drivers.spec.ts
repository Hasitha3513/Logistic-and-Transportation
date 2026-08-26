
import { test, expect } from '../../fixtures/authFixtures';
import { DriversPage } from '../../pages/DriversPage';

test.describe('@drivers Driver Profiles & Licensing (US-39, US-40)', () => {
  let driversList: Record<string, unknown>[] = [];

  test.beforeEach(async ({ fleetManagerPage }) => {
    driversList = [
      { id: '40000000-0000-0000-0000-000000000001', employeeNumber: 'DRV-001', firstName: 'Kasun', lastName: 'Fernando', status: 'AVAILABLE', active: true },
    ];

    await fleetManagerPage.route('**/api/drivers', async (r) => {
      if (r.request().method() === 'POST') {
        const body = r.request().postDataJSON();
        const created = { id: '40000000-0000-0000-0000-000000000099', ...body, status: 'AVAILABLE', active: true };
        driversList.push(created);
        return r.fulfill({ status: 201, contentType: 'application/json', body: JSON.stringify(created) });
      }
      return r.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(driversList) });
    });
  });

  test('E2E-DRV-001: Create driver profile with valid data', async ({ fleetManagerPage }) => {
    const page = new DriversPage(fleetManagerPage);
    await page.goto();
    await page.createDriver({ employeeNumber: 'DRV-999', firstName: 'Nimal', lastName: 'Perera' });
    await expect(page.driverTable).toContainText('Nimal Perera');
  });

  test('E2E-DRV-002: Driver creation form validates required fields', async ({ fleetManagerPage }) => {
    const page = new DriversPage(fleetManagerPage);
    await page.goto();
    await page.openCreateModal();
    await fleetManagerPage.click('.ant-modal-footer button.ant-btn-primary');
    const errors = fleetManagerPage.locator('.resource-editor-error');
    await expect(errors.first()).toBeVisible({ timeout: 10000 });
    await expect(errors).toHaveCount(4);
  });
});
