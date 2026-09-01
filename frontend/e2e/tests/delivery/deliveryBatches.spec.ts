import { test, expect } from '@playwright/test';

test.describe('Delivery Batches & Clustering UX (US-66)', () => {
  const tenantId = '4f8b6a3b-2c1e-4d89-9a72-f9e4c5b3671a';

  test.beforeEach(async ({ page }) => {
    // Mock authentication with delivery batch permissions
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
            'DELIVERY_BATCH_VIEW',
            'DELIVERY_BATCH_CREATE',
            'DELIVERY_BATCH_UPDATE',
            'DELIVERY_BATCH_ASSIGN',
            'DELIVERY_BATCH_DISPATCH',
            'DELIVERY_BATCH_CANCEL',
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

    // Mock delivery slots
    await page.route('**/api/v1/delivery-slots', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify([
          {
            id: 'b1b2c3d4-0001-4000-8000-000000000001',
            tenantId,
            deliveryZoneId: 'a1b2c3d4-0001-4000-8000-000000000001',
            slotName: 'Morning Peak',
            status: 'ACTIVE',
          },
        ]),
      });
    });

    // Mock delivery riders
    await page.route('**/api/v1/deliveries/riders*', async (route) => {
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
            maxConcurrentDeliveries: 5,
            status: 'ACTIVE',
          },
        ]),
      });
    });

    // Mock delivery batches
    await page.route('**/api/v1/deliveries/batches*', async (route) => {
      if (route.request().method() === 'GET') {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            content: [
              {
                id: 'bat-0001-1111-2222-333333333333',
                tenantId,
                batchCode: 'BAT-2026-000001',
                deliveryZoneId: 'a1b2c3d4-0001-4000-8000-000000000001',
                deliverySlotId: 'b1b2c3d4-0001-4000-8000-000000000001',
                status: 'DRAFT',
                maxBatchSize: 5,
                activeOrderCount: 3,
                totalOrderCount: 3,
                version: 0,
                createdAt: '2026-09-01T10:00:00Z',
                updatedAt: '2026-09-01T10:00:00Z',
                createdBy: 'dispatcher.john',
                updatedBy: 'dispatcher.john',
              },
            ],
            page: 0,
            size: 50,
            totalElements: 1,
            totalPages: 1,
          }),
        });
      } else {
        await route.continue();
      }
    });

    // Mock batch orders
    await page.route('**/api/v1/deliveries/batches/*/orders', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify([
          {
            id: 'mem-0001-1111-2222-333333333333',
            tenantId,
            batchId: 'bat-0001-1111-2222-333333333333',
            deliveryOrderId: 'del-0001-1111-2222-333333333333',
            sequenceHint: 1,
            status: 'ACTIVE',
            addedAt: '2026-09-01T10:00:00Z',
            addedBy: 'dispatcher.john',
            version: 0,
          },
        ]),
      });
    });
  });

  test('should render delivery batches page with table and trigger detail drawer', async ({ page }) => {
    await page.goto('/deliveries/batches');

    await expect(page.getByRole('heading', { name: 'Delivery Batches & Clustering' })).toBeVisible();
    await expect(page.getByText('BAT-2026-000001')).toBeVisible();
    await expect(page.getByText('Central Business District')).toBeVisible();
    await expect(page.getByText('DRAFT')).toBeVisible();

    // Click Details button
    await page.getByRole('button', { name: 'Details' }).first().click();

    // Verify detail drawer opened
    await expect(page.getByText('Batch Details: BAT-2026-000001')).toBeVisible();
    await expect(page.getByText('Contained Orders (1)')).toBeVisible();
  });
});
