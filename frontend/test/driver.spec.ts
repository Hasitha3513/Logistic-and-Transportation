import { test, expect } from '@playwright/test';
import { mockAdminUser, setupMockAuth } from './utils/mock';

test.describe('Driver Management', () => {
  const driversList = [
    {
      id: '40000000-0000-0000-0000-000000000001',
      employeeNumber: 'DRV-001',
      firstName: 'Kasun',
      lastName: 'Fernando',
      phone: '+94 77 555 1001',
      email: 'kasun.fernando@example.test',
      status: 'AVAILABLE',
      active: true,
    },
  ];

  test.beforeEach(async ({ page }) => {
    await setupMockAuth(page, mockAdminUser, true);

    await page.route('**/api/drivers', async (route) => {
      if (route.request().method() === 'POST') {
        const body = route.request().postDataJSON();
        const created = { id: '40000000-0000-0000-0000-000000000099', ...body, status: 'AVAILABLE', active: true };
        driversList.push(created);
        return route.fulfill({ status: 201, contentType: 'application/json', body: JSON.stringify(created) });
      }
      return route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(driversList) });
    });
  });

  test('create a driver with valid data', async ({ page }) => {
    await page.goto('/drivers');
    await expect(page.locator('.resource-list__title')).toContainText('Driver registry', { timeout: 10000 });
    await page.click('button:has-text("Create")');

    await page.fill('#resource-employeeNumber', 'DR12345');
    await page.fill('#resource-firstName', 'John');
    await page.fill('#resource-lastName', 'Doe');
    await page.fill('#resource-email', 'john.doe@example.com');
    await page.click('#resource-status');
    await page.click('.ant-select-dropdown:visible .ant-select-item-option:has-text("Available")');
    await page.click('.ant-modal-footer button.ant-btn-primary');

    await expect(page.locator('.ant-message')).toContainText('saved', { timeout: 10000 });
    await expect(page.locator('table')).toContainText('John Doe', { timeout: 10000 });
  });

  test('validation errors when required fields are missing', async ({ page }) => {
    await page.goto('/drivers');
    await expect(page.locator('.resource-list__title')).toContainText('Driver registry', { timeout: 10000 });
    await page.click('button:has-text("Create")');
    // Submit without filling fields
    await page.click('.ant-modal-footer button.ant-btn-primary');
    const errors = page.locator('.resource-editor-error');
    await expect(errors.first()).toBeVisible({ timeout: 10000 });
    await expect(errors).toHaveCount(4); // employeeNumber, firstName, lastName, status required
  });
});

