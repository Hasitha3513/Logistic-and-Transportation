
import { test, expect } from '../../fixtures/authFixtures';
import { RoutesPage } from '../../pages/RoutesPage';

test.describe('@routing Route Definitions & Multi-Stop Plans (US-17, US-18, US-19)', () => {
  let routesList: Record<string, unknown>[] = [];

  test.beforeEach(async ({ dispatcherPage }) => {
    routesList = [
      { id: '50000000-0000-0000-0000-000000000001', code: 'RTE-COL-KND', name: 'Colombo to Kandy', plannedDistanceKm: 115, estimatedDurationMinutes: 180, active: true },
    ];

    await dispatcherPage.route('**/api/locations*', (r) => r.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify([
        { id: '20000000-0000-0000-0000-000000000001', name: 'Colombo Hub', code: 'COL-01' },
        { id: '20000000-0000-0000-0000-000000000002', name: 'Kandy Depot', code: 'KND-01' },
      ]),
    }));

    await dispatcherPage.route('**/api/routes*', async (r) => {
      if (r.request().method() === 'POST') {
        const body = r.request().postDataJSON();
        const created = { id: '50000000-0000-0000-0000-000000000099', ...body, active: true };
        routesList.push(created);
        return r.fulfill({ status: 201, contentType: 'application/json', body: JSON.stringify(created) });
      }
      return r.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(routesList) });
    });
  });

  test('E2E-ROUTE-001: Create route definition with distance and duration', async ({ dispatcherPage }) => {
    const page = new RoutesPage(dispatcherPage);
    await page.goto();
    await page.createRoute({ code: 'RTE-EXP-01', name: 'Express Way Route', distance: 130, duration: 150 });
    await expect(page.routeTable).toContainText('Express Way Route');
  });
});
