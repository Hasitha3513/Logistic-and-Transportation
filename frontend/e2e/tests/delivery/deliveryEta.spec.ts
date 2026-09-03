import { test, expect } from '@playwright/test';

test.describe('Last-Mile ETA & Route Projections UX (US-67)', () => {
  const tenantId = '4f8b6a3b-2c1e-4d89-9a72-f9e4c5b3671a';
  const batchId = 'c1b2c3d4-0001-4000-8000-000000000001';
  const orderId1 = 'd1b2c3d4-0001-4000-8000-000000000001';

  test.beforeEach(async ({ page }) => {
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
            'DASHBOARD_VIEW',
          ],
          tenantId,
        }),
      });
    });

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

    await page.route('**/api/v1/delivery-slots', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify([]),
      });
    });

    await page.route('**/api/v1/delivery-riders*', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          data: [],
          total: 0,
        }),
      });
    });

    await page.route('**/api/v1/deliveries/batches*', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ content: [
          {
            id: batchId,
            tenantId,
            batchCode: 'BAT-2026-000010',
            deliveryZoneId: 'a1b2c3d4-0001-4000-8000-000000000001',
            status: 'READY',
            maxBatchSize: 10,
            activeOrderCount: 1,
            totalOrderCount: 1,
            version: 1,
            createdAt: '2026-09-01T10:00:00Z',
            updatedAt: '2026-09-01T10:05:00Z',
            createdBy: 'dispatcher.john',
            updatedBy: 'dispatcher.john',
          },
        ], page: 0, size: 10, totalElements: 1, totalPages: 1 }),
      });
    });

    await page.route(`**/api/v1/deliveries/batches/${batchId}/orders`, async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify([
          {
            id: 'e1b2c3d4-0001-4000-8000-000000000001',
            tenantId,
            batchId,
            deliveryOrderId: orderId1,
            sequenceHint: 1,
            status: 'ACTIVE',
            addedAt: '2026-09-01T10:02:00Z',
            addedBy: 'dispatcher.john',
            version: 1,
          },
        ]),
      });
    });

    await page.route(`**/api/v1/deliveries/batches/${batchId}/eta`, async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          batchId,
          calculatedAt: '2026-09-01T10:10:00Z',
          staleAt: '2026-09-01T10:25:00Z',
          totalDurationSeconds: 1400,
          totalDistanceMeters: 4500,
          estimatedCompletionAt: '2026-09-01T10:33:20Z',
          source: 'HEURISTIC',
          isStale: false,
          stops: [
            {
              deliveryOrderId: orderId1,
              sequence: 1,
              estimatedArrivalAt: '2026-09-01T10:23:20Z',
              travelDurationSeconds: 800,
              serviceDurationSeconds: 600,
              distanceMeters: 4500,
              slaStatus: 'ON_TIME',
            },
          ],
        }),
      });
    });

    await page.route(`**/api/v1/deliveries/batches/${batchId}/eta/calculate`, async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          batchId,
          calculatedAt: '2026-09-01T10:12:00Z',
          staleAt: '2026-09-01T10:27:00Z',
          totalDurationSeconds: 1400,
          totalDistanceMeters: 4500,
          estimatedCompletionAt: '2026-09-01T10:35:20Z',
          source: 'HEURISTIC',
          isStale: false,
          stops: [
            {
              deliveryOrderId: orderId1,
              sequence: 1,
              estimatedArrivalAt: '2026-09-01T10:25:20Z',
              travelDurationSeconds: 800,
              serviceDurationSeconds: 600,
              distanceMeters: 4500,
              slaStatus: 'ON_TIME',
            },
          ],
        }),
      });
    });
  });

  test('E2E-ETA-01: Displays batch completion ETA and stop arrival times with SLA badge in drawer', async ({ page }) => {
    await page.goto('/deliveries/batches');

    // Batch row should be visible
    await expect(page.getByText('BAT-2026-000010')).toBeVisible();

    // Click View Details to open drawer
    await page.getByRole('button', { name: /Details/ }).click();

    // Drawer should open and display ETA projection
    await expect(page.getByText('Estimated Arrival & Route Projection')).toBeVisible();
    await expect(page.getByText('Completion ETA')).toBeVisible();
    await expect(page.getByText('HEURISTIC')).toBeVisible();
    await expect(page.getByText('Fresh', { exact: true })).toBeVisible();

    // Stop table should show calculated stop ETA and ON_TIME tag
    await expect(page.getByText('ON_TIME')).toBeVisible();
    await expect(page.getByText('4.5 km (13m travel + 10m buffer)')).toBeVisible();

    // Recalculate ETA button should trigger recalculation
    const recalcBtn = page.getByRole('button', { name: 'Recalculate ETA' });
    await expect(recalcBtn).toBeVisible();
    await recalcBtn.click();

    await expect(page.getByText('Batch ETA recalculated')).toBeVisible();
  });
});
