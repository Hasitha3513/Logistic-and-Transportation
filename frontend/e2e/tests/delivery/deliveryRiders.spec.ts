import { test, expect } from '@playwright/test';

test.describe('Delivery Riders UX & Shift Scheduling (US-65)', () => {
  const tenantId = '4f8b6a3b-2c1e-4d89-9a72-f9e4c5b3671a';

  test.beforeEach(async ({ page }) => {
    // Mock authentication with rider permissions
    await page.route('**/api/auth/me', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          id: '11111111-1111-1111-1111-111111111111',
          username: 'dispatcher.john',
          roles: ['DISPATCHER'],
          permissions: [
            'DELIVERY_VIEW',
            'DELIVERY_RIDER_VIEW',
            'DELIVERY_RIDER_CREATE',
            'DELIVERY_RIDER_UPDATE',
            'DELIVERY_RIDER_ACTIVATE',
            'DELIVERY_RIDER_ASSIGN',
            'DELIVERY_RIDER_OVERRIDE',
            'DASHBOARD_VIEW',
          ],
          tenantId,
        }),
      });
    });

    // Mock delivery zones
    await page.route('**/api/v1/delivery-zones', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify([
          {
            id: 'a1b2c3d4-0001-4000-8000-000000000001',
            tenantId,
            zoneCode: 'ZONE-CBD',
            zoneName: 'Central Business District',
            zoneType: 'URBAN_DENSE',
            status: 'ACTIVE',
            serviceable: true,
            priority: 10,
          },
        ]),
      });
    });

    // Mock delivery riders
    await page.route('**/api/v1/deliveries/riders', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify([
          {
            id: 'rdr-0001-1111-2222-333333333333',
            tenantId,
            riderCode: 'RDR-COL-001',
            driverId: 'drv-0001-1111-2222-333333333333',
            riderType: 'FULL_TIME',
            primaryZoneId: 'a1b2c3d4-0001-4000-8000-000000000001',
            secondaryZoneIds: [],
            maxConcurrentDeliveries: 4,
            status: 'ACTIVE',
            version: 0,
            createdAt: '2026-09-01T10:00:00Z',
            createdBy: 'admin',
            updatedAt: '2026-09-01T10:00:00Z',
            updatedBy: 'admin',
          },
        ]),
      });
    });
  });

  test('navigates to delivery riders list and displays roster elements', async ({ page }) => {
    await page.goto('/deliveries/riders');
    await expect(page.locator('text=Delivery Riders Roster & Duty Scheduling')).toBeVisible();
    await expect(page.locator('button:has-text("Onboard Rider")')).toBeVisible();
    await expect(page.locator('text=RDR-COL-001')).toBeVisible();
  });

  test('opens onboard rider modal and validates driver id requirement', async ({ page }) => {
    await page.goto('/deliveries/riders');
    await page.click('button:has-text("Onboard Rider")');
    await expect(page.locator('.ant-modal-title:has-text("Onboard New Delivery Rider")')).toBeVisible();
    await page.click('.ant-modal-content button[type="submit"]:has-text("Onboard Rider")');
    await expect(page.locator('text=Driver ID is required')).toBeVisible();
  });
});
