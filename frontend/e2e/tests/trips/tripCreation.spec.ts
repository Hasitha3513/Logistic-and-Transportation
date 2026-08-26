
import { test, expect } from '../../fixtures/authFixtures';
import { TripsPage } from '../../pages/TripsPage';

test.describe('@trips Trip Order Creation (US-09)', () => {
  test.beforeEach(async ({ dispatcherPage }) => {
    await dispatcherPage.route('**/api/trips*', (r) => r.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        content: [
          { id: '60000000-0000-0000-0000-000000000001', tripNumber: 'TRIP-2026-001', customerId: '10000000-0000-0000-0000-000000000001', priority: 'NORMAL', status: 'DRAFT', requestedStartTime: '2026-08-20T08:00:00Z', requestedEndTime: '2026-08-20T18:00:00Z', active: true },
        ],
        totalElements: 1,
      }),
    }));
  });

  test('E2E-TRIP-001: View existing trip orders in operations list', async ({ dispatcherPage }) => {
    const page = new TripsPage(dispatcherPage);
    await page.goto();
    await expect(page.tripTable).toContainText('TRIP-2026-001');
  });
});
