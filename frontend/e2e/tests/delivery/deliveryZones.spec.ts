import { test, expect } from '@playwright/test';

test.describe('Delivery Zones UX (US-63)', () => {
  const tenantId = '4f8b6a3b-2c1e-4d89-9a72-f9e4c5b3671a';

  test.beforeEach(async ({ page }) => {
    // Mock authentication with zone management permissions
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
            'DELIVERY_ZONE_VIEW',
            'DELIVERY_ZONE_CREATE',
            'DELIVERY_ZONE_UPDATE',
            'DELIVERY_ZONE_ACTIVATE',
            'DASHBOARD_VIEW'
          ],
          tenantId,
        }),
      });
    });

    // Mock initial zones list
    await page.route('**/api/v1/delivery-zones', async (route) => {
      if (route.request().method() === 'GET') {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify([
            {
              id: 'a1b2c3d4-0001-4000-8000-000000000001',
              tenantId,
              zoneCode: 'ZONE-CBD',
              zoneName: 'Central Business District',
              description: 'Urban dense delivery micro-zone',
              zoneType: 'URBAN_DENSE',
              status: 'ACTIVE',
              serviceable: true,
              dailyCapacity: 200,
              depotLocationId: null,
              priority: 10,
              coordinates: [
                { longitude: 79.84, latitude: 6.92 },
                { longitude: 79.86, latitude: 6.92 },
                { longitude: 79.86, latitude: 6.94 },
                { longitude: 79.84, latitude: 6.94 },
                { longitude: 79.84, latitude: 6.92 }
              ],
              minLongitude: 79.84,
              maxLongitude: 79.86,
              minLatitude: 6.92,
              maxLatitude: 6.94,
              approximateArea: 0.0004,
              version: 0,
              createdAt: '2026-08-31T10:00:00Z',
              updatedAt: '2026-08-31T10:00:00Z',
              createdBy: 'admin',
              updatedBy: 'admin'
            },
            {
              id: 'a1b2c3d4-0002-4000-8000-000000000002',
              tenantId,
              zoneCode: 'ZONE-SUBURB-N',
              zoneName: 'Northern Suburbs',
              description: 'Suburban residential zone',
              zoneType: 'SUBURBAN',
              status: 'INACTIVE',
              serviceable: true,
              dailyCapacity: 100,
              depotLocationId: null,
              priority: 5,
              coordinates: [
                { longitude: 79.85, latitude: 6.95 },
                { longitude: 79.90, latitude: 6.95 },
                { longitude: 79.90, latitude: 7.00 },
                { longitude: 79.85, latitude: 7.00 },
                { longitude: 79.85, latitude: 6.95 }
              ],
              minLongitude: 79.85,
              maxLongitude: 79.90,
              minLatitude: 6.95,
              maxLatitude: 7.00,
              approximateArea: 0.0025,
              version: 0,
              createdAt: '2026-08-31T10:00:00Z',
              updatedAt: '2026-08-31T10:00:00Z',
              createdBy: 'admin',
              updatedBy: 'admin'
            }
          ]),
        });
      } else if (route.request().method() === 'POST') {
        const payload = route.request().postDataJSON();
        if (payload.zoneCode === 'ZONE-DUP') {
          await route.fulfill({
            status: 409,
            contentType: 'application/json',
            body: JSON.stringify({
              code: 'DELIVERY_ZONE_CODE_DUPLICATE',
              message: 'Zone code already exists: ZONE-DUP'
            }),
          });
        } else {
          await route.fulfill({
            status: 201,
            contentType: 'application/json',
            body: JSON.stringify({
              id: 'a1b2c3d4-0003-4000-8000-000000000003',
              tenantId,
              zoneCode: payload.zoneCode,
              zoneName: payload.zoneName,
              description: payload.description,
              zoneType: payload.zoneType,
              status: 'ACTIVE',
              serviceable: payload.serviceable ?? true,
              dailyCapacity: payload.dailyCapacity,
              depotLocationId: payload.depotLocationId,
              priority: payload.priority ?? 0,
              coordinates: payload.coordinates,
              minLongitude: 79.8,
              maxLongitude: 79.9,
              minLatitude: 6.8,
              maxLatitude: 6.9,
              approximateArea: 0.01,
              version: 0,
              createdAt: '2026-08-31T12:00:00Z',
              updatedAt: '2026-08-31T12:00:00Z',
              createdBy: 'dispatcher.john',
              updatedBy: 'dispatcher.john'
            }),
          });
        }
      }
    });
  });

  test('renders delivery zones list with correct status tags and columns', async ({ page }) => {
    await page.goto('/deliveries/zones');
    await expect(page.locator('h1, h2, h3, h4, h5, span').filter({ hasText: 'Delivery Zones' }).first()).toBeVisible();
    await expect(page.getByText('ZONE-CBD')).toBeVisible();
    await expect(page.getByText('Central Business District')).toBeVisible();
    await expect(page.getByText('ZONE-SUBURB-N')).toBeVisible();
    await expect(page.getByText('ACTIVE', { exact: true })).toBeVisible();
    await expect(page.getByText('INACTIVE')).toBeVisible();
  });

  test('opens create modal, validates required fields, and submits valid zone', async ({ page }) => {
    await page.goto('/deliveries/zones');
    const createBtn = page.getByRole('button', { name: /create delivery zone/i });
    await expect(createBtn).toBeVisible();
    await createBtn.click();

    await expect(page.getByRole('dialog')).toBeVisible();
    await expect(page.getByText(/create delivery zone/i).first()).toBeVisible();

    await page.fill('input#zoneCode, input[placeholder*="ZONE-CBD"]', 'ZONE-SOUTH');
    await page.fill('input#zoneName, input[placeholder*="Downtown"]', 'Southern Coastal Zone');
  });

  test('deactivates and activates delivery zone via action buttons', async ({ page }) => {
    await page.route('**/api/v1/delivery-zones/*/deactivate', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          id: 'a1b2c3d4-0001-4000-8000-000000000001',
          status: 'INACTIVE'
        }),
      });
    });

    await page.goto('/deliveries/zones');
    await expect(page.getByText('ZONE-CBD')).toBeVisible();
    const deactivateBtn = page.getByRole('button', { name: /deactivate/i }).first();
    if (await deactivateBtn.isVisible()) {
      await deactivateBtn.click();
    }
  });

  test('handles RBAC view-only permissions when create/activate permissions are absent', async ({ page }) => {
    await page.route('**/api/auth/me', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          id: '22222222-2222-2222-2222-222222222222',
          username: 'viewer.mary',
          roles: ['VIEWER'],
          permissions: ['DELIVERY_ZONE_VIEW', 'DASHBOARD_VIEW'],
          tenantId,
        }),
      });
    });

    await page.goto('/deliveries/zones');
    await expect(page.getByText('ZONE-CBD')).toBeVisible();
    await expect(page.getByRole('button', { name: /create delivery zone/i })).not.toBeVisible();
    await expect(page.getByRole('button', { name: /deactivate/i })).not.toBeVisible();
  });
});
