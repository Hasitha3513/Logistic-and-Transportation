import { test, expect } from '@playwright/test';

test.describe('Delivery Slots UX (US-64)', () => {
  const tenantId = '4f8b6a3b-2c1e-4d89-9a72-f9e4c5b3671a';

  test.beforeEach(async ({ page }) => {
    // Mock authentication with slot permissions
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
            'DELIVERY_SLOT_VIEW',
            'DELIVERY_SLOT_CREATE',
            'DELIVERY_SLOT_UPDATE',
            'DELIVERY_SLOT_ACTIVATE',
            'DELIVERY_SLOT_ASSIGN',
            'DELIVERY_SLOT_OVERRIDE',
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
    await page.route('**/api/v1/delivery-slots**', async (route) => {
      if (route.request().method() === 'GET' && route.request().url().includes('/reservations')) {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify([]),
        });
      } else if (route.request().method() === 'GET') {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify([
            {
              id: 'slot-1111-2222-3333-444444444444',
              tenantId,
              deliveryZoneId: 'a1b2c3d4-0001-4000-8000-000000000001',
              slotDate: '2026-09-01',
              startTime: '09:00:00',
              endTime: '12:00:00',
              slotType: 'STANDARD',
              maxCapacity: 10,
              reservedCapacity: 3,
              remainingCapacity: 7,
              cutoffTime: null,
              bufferMinutes: 15,
              status: 'ACTIVE',
              version: 0,
              createdAt: '2026-08-31T10:00:00Z',
              createdBy: 'admin',
              updatedAt: '2026-08-31T10:00:00Z',
              updatedBy: 'admin',
            },
          ]),
        });
      } else {
        await route.continue();
      }
    });
  });

  test('navigates to delivery slots list and displays capacity metrics', async ({ page }) => {
    await page.goto('/deliveries/slots');

    await expect(page.getByTestId('delivery-slot-list-page')).toBeVisible();
    await expect(page.getByText('Delivery Slots & Capacity Management')).toBeVisible();
    await expect(page.getByText('09:00 - 12:00')).toBeVisible();
    await expect(page.getByText('3 / 10 booked')).toBeVisible();
    await expect(page.getByText('7 left')).toBeVisible();
    await expect(page.getByTestId('create-slot-button')).toBeVisible();
  });

  test('opens create slot drawer', async ({ page }) => {
    await page.goto('/deliveries/slots');

    await page.getByTestId('create-slot-button').click();
    await expect(page.getByText('Create Delivery Slot')).toBeVisible();
    await expect(page.getByTestId('slot-zone-select')).toBeVisible();
    await expect(page.getByTestId('slot-time-range').first()).toBeVisible();
    await expect(page.getByTestId('slot-max-capacity')).toBeVisible();
  });
});
