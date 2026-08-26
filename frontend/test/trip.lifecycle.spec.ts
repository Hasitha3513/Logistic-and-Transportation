import { test, expect } from '@playwright/test';
import { mockAdminUser, setupMockAuth } from './utils/mock';

test.describe('Trip Lifecycle', () => {
  const tripId = '60000000-0000-0000-0000-000000000001';
  let currentStatus = 'DRAFT';

  const mockTrip = () => ({
    id: tripId,
    tripNumber: 'TRIP-DEMO-001',
    customerId: '10000000-0000-0000-0000-000000000001',
    departmentId: '11000000-0000-0000-0000-000000000001',
    projectId: '12000000-0000-0000-0000-000000000001',
    routeId: '50000000-0000-0000-0000-000000000001',
    priority: 'NORMAL',
    status: currentStatus,
    originLocationId: '20000000-0000-0000-0000-000000000001',
    destinationLocationId: '20000000-0000-0000-0000-000000000002',
    requestedStartTime: '2026-08-25T08:00:00Z',
    requestedEndTime: '2026-08-25T16:00:00Z',
    requiredVehicleTypeId: '31000000-0000-0000-0000-000000000001',
    requiredCapacityKg: 2500,
    cargoDescription: 'Packaged retail goods',
    passengerCount: 1,
    vehicleId: '32000000-0000-0000-0000-000000000001',
    driverId: '40000000-0000-0000-0000-000000000001',
  });

  test.beforeEach(async ({ page }) => {
    currentStatus = 'DRAFT';
    await setupMockAuth(page, mockAdminUser, true);

    await page.route('**/api/customers', (route) => route.fulfill({
      status: 200, contentType: 'application/json', body: JSON.stringify([{ id: '10000000-0000-0000-0000-000000000001', name: 'Acme Distribution' }]),
    }));

    await page.route('**/api/locations', (route) => route.fulfill({
      status: 200, contentType: 'application/json', body: JSON.stringify([
        { id: '20000000-0000-0000-0000-000000000001', name: 'Colombo Hub' },
        { id: '20000000-0000-0000-0000-000000000002', name: 'Kandy Depot' },
      ]),
    }));

    await page.route('**/api/vehicles', (route) => route.fulfill({
      status: 200, contentType: 'application/json', body: JSON.stringify([
        { id: '32000000-0000-0000-0000-000000000001', registrationNumber: 'WP-CAB-1201' },
      ]),
    }));

    await page.route('**/api/drivers', (route) => route.fulfill({
      status: 200, contentType: 'application/json', body: JSON.stringify([
        { id: '40000000-0000-0000-0000-000000000001', firstName: 'Kasun', lastName: 'Fernando' },
      ]),
    }));

    await page.route(`**/api/trips/${tripId}/status-history`, (route) => route.fulfill({
      status: 200, contentType: 'application/json', body: JSON.stringify([]),
    }));

    await page.route(`**/api/trips/${tripId}/submit`, (route) => {
      currentStatus = 'SUBMITTED';
      return route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(mockTrip()) });
    });

    await page.route(`**/api/trips/${tripId}/approve`, (route) => {
      currentStatus = 'APPROVED';
      return route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(mockTrip()) });
    });

    await page.route(`**/api/trips/${tripId}`, (route) => {
      return route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(mockTrip()) });
    });
  });

  test('full trip lifecycle UI flow', async ({ page }) => {
    await page.goto(`/trips/${tripId}`);
    const submitBtn = page.locator('.lifecycle-actions button:has-text("Submit")');
    await expect(submitBtn).toBeVisible({ timeout: 15000 });
    await submitBtn.click();
    await page.click('.ant-modal-footer button.ant-btn-primary');
    await expect(page.locator('.ant-message')).toContainText('submitted', { timeout: 15000 });

    // In SUBMITTED status -> Approve action
    const approveBtn = page.locator('.lifecycle-actions button:has-text("Approve")');
    await expect(approveBtn).toBeVisible({ timeout: 15000 });
    await approveBtn.click();
    await page.click('.ant-modal-footer button.ant-btn-primary');
    await expect(page.locator('.ant-message')).toContainText('approved', { timeout: 15000 });
  });
});

