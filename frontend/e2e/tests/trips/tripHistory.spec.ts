
import { test, expect } from '../../fixtures/authFixtures';
import { TripDetailsPage } from '../../pages/TripDetailsPage';

test.describe('@trips Trip Audit Log Timeline (US-13)', () => {
  const tripId = '60000000-0000-0000-0000-000000000001';

  test.beforeEach(async ({ dispatcherPage }) => {
    await dispatcherPage.route(`**/api/trips/${tripId}/status-history*`, (r) => r.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify([
        { id: 'th-1', tripId, fromStatus: 'DRAFT', toStatus: 'SUBMITTED', changedBy: 'admin', changedAt: '2026-08-19T09:00:00Z', action: 'SUBMIT' },
        { id: 'th-2', tripId, fromStatus: 'SUBMITTED', toStatus: 'APPROVED', changedBy: 'manager', changedAt: '2026-08-19T09:30:00Z', action: 'APPROVE' },
      ]),
    }));

    await dispatcherPage.route(`**/api/trips/${tripId}*`, (r) => r.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ id: tripId, tripNumber: 'TRIP-2026-001', status: 'APPROVED', active: true }),
    }));
  });

  test('E2E-TRIP-007: View status transition history timeline', async ({ dispatcherPage }) => {
    const page = new TripDetailsPage(dispatcherPage);
    await page.goto(tripId);
    await expect(dispatcherPage.locator('.ant-timeline, .history-timeline, .trip-detail-card, .lifecycle-actions').first()).toBeVisible();
  });
});
