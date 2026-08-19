import { test, expect } from '@playwright/test';
import { mockAdminUser, setupMockAuth } from './utils/mock';

test.describe('Vehicle Management', () => {
  const vehiclesList = [
    {
      id: '32000000-0000-0000-0000-000000000001',
      registrationNumber: 'WP-CAB-1201',
      manufacturer: 'Isuzu',
      model: 'NPR',
      capacityKg: 5500,
      operationalStatus: 'AVAILABLE',
      active: true,
    },
  ];

  test.beforeEach(async ({ page }) => {
    await setupMockAuth(page, mockAdminUser, true);

    await page.route('**/api/vehicle-categories', async (route) => {
      return route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify([{ id: 'cat-1', name: 'Trucks' }]),
      });
    });

    await page.route('**/api/vehicle-types', async (route) => {
      return route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify([{ id: 'type-1', name: 'Box Truck' }]),
      });
    });

    await page.route('**/api/vehicles', async (route) => {
      if (route.request().method() === 'POST') {
        const body = route.request().postDataJSON();
        const created = { id: '32000000-0000-0000-0000-000000000099', ...body, active: true };
        vehiclesList.push(created);
        return route.fulfill({ status: 201, contentType: 'application/json', body: JSON.stringify(created) });
      }
      return route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(vehiclesList) });
    });
  });

  test('create vehicle with valid VIN', async ({ page }) => {
    await page.goto('/fleet/vehicles');
    await expect(page.locator('.resource-list__title')).toContainText('Vehicle registry', { timeout: 10000 });
    await page.click('button:has-text("Create")');

    await page.fill('#resource-registrationNumber', 'WP-ABC-1234');
    await page.click('#resource-categoryId');
    await page.click('.ant-select-dropdown:visible .ant-select-item-option:has-text("Trucks")');
    await page.click('#resource-typeId');
    await page.click('.ant-select-dropdown:visible .ant-select-item-option:has-text("Box Truck")');
    await page.click('#resource-ownershipType');
    await page.click('.ant-select-dropdown:visible .ant-select-item-option:has-text("Company owned")');
    await page.click('#resource-operationalStatus');
    await page.click('.ant-select-dropdown:visible .ant-select-item-option:has-text("Available")');
    await page.fill('#resource-manufacturer', 'Toyota');
    await page.fill('#resource-model', 'Corolla');
    await page.click('.ant-modal-footer button.ant-btn-primary');

    await expect(page.locator('.ant-message')).toContainText('saved', { timeout: 10000 });
    await expect(page.locator('table')).toContainText('Toyota', { timeout: 10000 });
  });

  test('validation errors when required fields are missing', async ({ page }) => {
    await page.goto('/fleet/vehicles');
    await expect(page.locator('.resource-list__title')).toContainText('Vehicle registry', { timeout: 10000 });
    await page.click('button:has-text("Create")');
    await page.click('.ant-modal-footer button.ant-btn-primary');
    const errors = page.locator('.resource-editor-error');
    await expect(errors.first()).toBeVisible({ timeout: 10000 });
  });
});

