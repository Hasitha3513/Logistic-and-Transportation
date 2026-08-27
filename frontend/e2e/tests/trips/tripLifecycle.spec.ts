
import { test } from '../../fixtures/authFixtures';
import { TripDetailsPage } from '../../pages/TripDetailsPage';

test.describe('@trips Trip State Transitions & Lifecycle (US-12, US-14, US-15, US-16, US-80)', () => {
  const tripId = '60000000-0000-0000-0000-000000000001';
  let currentStatus = 'DRAFT';

  test.beforeEach(async ({ dispatcherPage }) => {
    currentStatus = 'DRAFT';

    await dispatcherPage.route(`**/api/trips/${tripId}/status-history`, (r) => r.fulfill({
      status: 200, contentType: 'application/json', body: JSON.stringify([]),
    }));

    await dispatcherPage.route(`**/api/trips/${tripId}/submit`, (r) => {
      currentStatus = 'SUBMITTED';
      return r.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ id: tripId, status: currentStatus }) });
    });

    await dispatcherPage.route(`**/api/trips/${tripId}/approve`, (r) => {
      currentStatus = 'APPROVED';
      return r.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ id: tripId, status: currentStatus }) });
    });

    await dispatcherPage.route(`**/api/trips/${tripId}`, (r) => {
      return r.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          id: tripId,
          tripNumber: 'TRIP-2026-001',
          status: currentStatus,
          active: true,
        }),
      });
    });
  });

  test('E2E-TRIP-004: Execute submit and approve lifecycle transitions', async ({ dispatcherPage }) => {
    const page = new TripDetailsPage(dispatcherPage);
    await page.goto(tripId);
    await page.submitTrip();
    await page.approveTrip();
  });
});
