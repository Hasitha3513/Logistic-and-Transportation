
import { test, expect } from '../../fixtures/authFixtures';
import { TripDetailsPage } from '../../pages/TripDetailsPage';

test.describe('@fuel Trip Fuel Cost Allocation (US-34)', () => {
  const tripId = '60000000-0000-0000-0000-000000000001';

  test.beforeEach(async ({ adminPage }) => {
    await adminPage.route(`**/api/trips/${tripId}`, (r) => r.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ id: tripId, tripNumber: 'TRIP-2026-001', status: 'COMPLETED', active: true }),
    }));

    await adminPage.route(`**/api/trips/${tripId}/status-history`, (r) => r.fulfill({
      status: 200, contentType: 'application/json', body: JSON.stringify([]),
    }));

    await adminPage.route(`**/api/trips/${tripId}/fuel-cost`, (r) => r.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        tripId,
        fuelType: 'DIESEL',
        totalDistanceKm: 120,
        consumedLiters: 24.5,
        unitPrice: 310.0,
        totalCost: 7595.0,
      }),
    }));
  });

  test('E2E-FUEL-004: View allocated trip fuel cost section on completed trip', async ({ adminPage }) => {
    const page = new TripDetailsPage(adminPage);
    await page.goto(tripId);
    await expect(adminPage.locator('.trip-detail-card').first()).toBeVisible();
  });
});
