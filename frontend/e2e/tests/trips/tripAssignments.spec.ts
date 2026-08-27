
import { test, expect } from '../../fixtures/authFixtures';
import { TripDetailsPage } from '../../pages/TripDetailsPage';

test.describe('@trips Resource Allocation & Conflict Prevention (US-04, US-08, US-10, US-11, US-81)', () => {
  const tripId = '60000000-0000-0000-0000-000000000001';

  test.beforeEach(async ({ dispatcherPage }) => {
    await dispatcherPage.route(`**/api/trips/${tripId}`, (r) => r.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        id: tripId,
        tripNumber: 'TRIP-2026-001',
        status: 'APPROVED',
        vehicleId: '32000000-0000-0000-0000-000000000001',
        driverId: '40000000-0000-0000-0000-000000000001',
        routeId: '50000000-0000-0000-0000-000000000001',
        active: true,
      }),
    }));

    await dispatcherPage.route(`**/api/trips/${tripId}/status-history`, (r) => r.fulfill({
      status: 200, contentType: 'application/json', body: JSON.stringify([]),
    }));
  });

  test('E2E-TRIP-002: View allocated vehicle, driver, and route on trip details', async ({ dispatcherPage }) => {
    const page = new TripDetailsPage(dispatcherPage);
    await page.goto(tripId);
    await expect(dispatcherPage.locator('.trip-detail-card').first()).toBeVisible();
  });
});
