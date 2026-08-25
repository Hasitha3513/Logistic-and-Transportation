import { test, expect } from '../../fixtures/authFixtures';

test.describe('@routing Phase-2 Route Intelligence: Optimization & Analytics (US-20, US-21, US-22, US-23)', () => {
  const routeId = '50000000-0000-0000-0000-000000000001';
  const stop1 = '20000000-0000-0000-0000-000000000010';
  const stop2 = '20000000-0000-0000-0000-000000000020';

  test.beforeEach(async ({ dispatcherPage }) => {
    await dispatcherPage.route('**/api/locations*', (r) => r.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify([
        { id: '20000000-0000-0000-0000-000000000001', name: 'Colombo Hub', code: 'COL-01' },
        { id: '20000000-0000-0000-0000-000000000002', name: 'Kandy Depot', code: 'KND-01' },
        { id: stop1, name: 'Kegalle Transit', code: 'KEG-01' },
        { id: stop2, name: 'Kadugannawa Station', code: 'KAD-01' },
      ]),
    }));

    await dispatcherPage.route(/\/api\/routes/, async (r) => {
      const url = r.request().url();
      const method = r.request().method();

      if (url.includes('/disruptions/active')) {
        return r.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify([
            {
              id: 'dis-active-1',
              routeId,
              disruptionType: 'ROAD_CLOSURE',
              severity: 'HIGH',
              description: 'Emergency bridge repair on sector 3',
              effectiveFrom: '2026-08-24T08:00:00Z',
              effectiveUntil: null,
              detourRouteId: null,
              status: 'ACTIVE',
              createdAt: '2026-08-24T08:00:00Z',
              createdBy: 'dispatcher',
              resolvedAt: null,
              resolvedBy: null,
            },
          ]),
        });
      }

      if (url.includes('/revisions')) {
        return r.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify([
            {
              id: 'rev-2',
              routeId,
              revisionNumber: 2,
              code: 'RTE-COL-KND',
              name: 'Colombo to Kandy via Bypass',
              originLocationId: '20000000-0000-0000-0000-000000000001',
              destinationLocationId: '20000000-0000-0000-0000-000000000002',
              plannedDistanceKm: 125,
              estimatedDurationMinutes: 195,
              active: true,
              stopLocationIds: [stop1, stop2],
              changedAt: '2026-08-24T12:00:00Z',
              changedBy: 'dispatcher',
            },
            {
              id: 'rev-1',
              routeId,
              revisionNumber: 1,
              code: 'RTE-COL-KND',
              name: 'Colombo to Kandy Direct',
              originLocationId: '20000000-0000-0000-0000-000000000001',
              destinationLocationId: '20000000-0000-0000-0000-000000000002',
              plannedDistanceKm: 115,
              estimatedDurationMinutes: 180,
              active: true,
              stopLocationIds: [stop1, stop2],
              changedAt: '2026-08-20T08:00:00Z',
              changedBy: 'system',
            },
          ]),
        });
      }

      if (url.includes('/performance')) {
        return r.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            routeId,
            routeCode: 'RTE-COL-KND',
            routeName: 'Colombo to Kandy',
            totalTripCount: 12,
            completedTripCount: 10,
            plannedDistanceKm: 115.0,
            averageActualDistanceKm: 120.0,
            distanceVarianceKm: 5.0,
            distanceVariancePercent: 4.35,
            plannedDurationMinutes: 180,
            averageActualDurationMinutes: 195,
            durationVarianceMinutes: 15,
            durationVariancePercent: 8.33,
            onTimeTripCount: 8,
            delayedTripCount: 2,
            averageDelayMinutes: 18.0,
          }),
        });
      }

      if (url.includes('/optimize') && method === 'POST') {
        return r.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            routeId,
            originalStopLocationIds: [stop1, stop2],
            optimizedStopLocationIds: [stop2, stop1],
            originalEstimatedDistanceKm: 115.0,
            optimizedEstimatedDistanceKm: 95.0,
            originalEstimatedDurationMinutes: 180,
            optimizedEstimatedDurationMinutes: 148,
            distanceSavedKm: 20.0,
            durationSavedMinutes: 32,
            percentageDistanceImprovement: 17.39,
          }),
        });
      }

      if (url.includes('/apply-optimization') && method === 'POST') {
        return r.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            id: routeId,
            code: 'RTE-COL-KND',
            name: 'Colombo to Kandy',
            originLocationId: '20000000-0000-0000-0000-000000000001',
            destinationLocationId: '20000000-0000-0000-0000-000000000002',
            plannedDistanceKm: 95.0,
            estimatedDurationMinutes: 148,
            active: true,
            stopLocationIds: [stop2, stop1],
          }),
        });
      }

      if (url.includes('/disruptions') && url.includes('/resolve') && method === 'POST') {
        return r.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            id: 'dis-active-1',
            routeId,
            disruptionType: 'ROAD_CLOSURE',
            severity: 'HIGH',
            description: 'Emergency bridge repair on sector 3',
            effectiveFrom: '2026-08-24T08:00:00Z',
            effectiveUntil: null,
            detourRouteId: null,
            status: 'RESOLVED',
            createdAt: '2026-08-24T08:00:00Z',
            createdBy: 'dispatcher',
            resolvedAt: '2026-08-24T13:00:00Z',
            resolvedBy: 'dispatcher',
          }),
        });
      }

      if (url.includes('/disruptions')) {
        return r.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify([
            {
              id: 'dis-active-1',
              routeId,
              disruptionType: 'ROAD_CLOSURE',
              severity: 'HIGH',
              description: 'Emergency bridge repair on sector 3',
              effectiveFrom: '2026-08-24T08:00:00Z',
              effectiveUntil: null,
              detourRouteId: null,
              status: 'ACTIVE',
              createdAt: '2026-08-24T08:00:00Z',
              createdBy: 'dispatcher',
              resolvedAt: null,
              resolvedBy: null,
            },
          ]),
        });
      }

      if (url.endsWith(`/api/routes/${routeId}`)) {
        return r.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            id: routeId,
            code: 'RTE-COL-KND',
            name: 'Colombo to Kandy',
            originLocationId: '20000000-0000-0000-0000-000000000001',
            destinationLocationId: '20000000-0000-0000-0000-000000000002',
            plannedDistanceKm: 115,
            estimatedDurationMinutes: 180,
            active: true,
            stopLocationIds: [stop1, stop2],
          }),
        });
      }

      return r.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify([
          {
            id: routeId,
            code: 'RTE-COL-KND',
            name: 'Colombo to Kandy',
            originLocationId: '20000000-0000-0000-0000-000000000001',
            destinationLocationId: '20000000-0000-0000-0000-000000000002',
            plannedDistanceKm: 115,
            estimatedDurationMinutes: 180,
            active: true,
            stopLocationIds: [stop1, stop2],
          },
        ]),
      });
    });
  });

  test('E2E-P2-RTE-001: Active disruptions banner displays across route library', async ({ dispatcherPage }) => {
    await dispatcherPage.goto('/routes');
    await expect(dispatcherPage.getByText(/1 Active Route Disruption\(s\) in Network:/)).toBeVisible();
    await expect(dispatcherPage.getByText(/Emergency bridge repair on sector 3/)).toBeVisible();
  });

  test('E2E-P2-RTE-002: Route detail drawer renders revision history snapshots', async ({ dispatcherPage }) => {
    await dispatcherPage.goto('/routes');
    await dispatcherPage.getByRole('button', { name: 'View details' }).first().click();
    await expect(dispatcherPage.getByText(/Revision History \(2\)/)).toBeVisible();
    await expect(dispatcherPage.getByText('Colombo to Kandy via Bypass')).toBeVisible();
    await expect(dispatcherPage.getByText('Colombo to Kandy Direct')).toBeVisible();
    await expect(dispatcherPage.getByText('v2')).toBeVisible();
    await expect(dispatcherPage.getByText('v1')).toBeVisible();
  });

  test('E2E-P2-RTE-003: Dispatcher resolves active disruption on route', async ({ dispatcherPage }) => {
    await dispatcherPage.goto('/routes');
    await dispatcherPage.getByRole('button', { name: 'View details' }).first().click();
    await expect(dispatcherPage.getByText(/Route Disruptions \(1\)/)).toBeVisible();
    const resolveBtn = dispatcherPage.getByTestId('resolve-disruption-btn-dis-active-1');
    await expect(resolveBtn).toBeVisible();
    await resolveBtn.click();
    await dispatcherPage.getByRole('button', { name: 'Yes, Resolve' }).click();
    await expect(dispatcherPage.getByText('Disruption marked as resolved')).toBeVisible();
  });

  test('E2E-P2-RTE-006: Route performance analytics shows planned-vs-actual metrics', async ({ dispatcherPage }) => {
    await dispatcherPage.goto('/routes');
    await dispatcherPage.getByRole('button', { name: 'View details' }).first().click();
    await expect(dispatcherPage.getByText(/Route Operational Performance/)).toBeVisible();
    await expect(dispatcherPage.getByText('Total Trips')).toBeVisible();
    await expect(dispatcherPage.getByText('12', { exact: true })).toBeVisible();
    await expect(dispatcherPage.getByText('Completed Trips')).toBeVisible();
    await expect(dispatcherPage.getByText('10', { exact: true })).toBeVisible();
    await expect(dispatcherPage.getByText('80%')).toBeVisible(); // On-Time Rate: 8/10 = 80%
    await expect(dispatcherPage.getByText('120 km')).toBeVisible();
    await expect(dispatcherPage.getByText('8 On-Time')).toBeVisible();
    await expect(dispatcherPage.getByText('2 Delayed')).toBeVisible();
  });

  test('E2E-P2-RTE-004: Authorized planner optimizes a multi-stop Route and previews improvement', async ({ dispatcherPage }) => {
    await dispatcherPage.goto('/routes');
    await dispatcherPage.getByRole('button', { name: 'View details' }).first().click();
    await dispatcherPage.getByRole('button', { name: 'Optimize Stops' }).click();
    await expect(dispatcherPage.getByText(/Route Stop Optimizer/)).toBeVisible();
    await expect(dispatcherPage.getByText(/Optimization found!/)).toBeVisible();
    await expect(dispatcherPage.getByText('Potential savings:')).toBeVisible();
    await expect(dispatcherPage.getByText(/\+17.39% FASTER/)).toBeVisible();
    await expect(dispatcherPage.getByText(/95 km/)).toBeVisible();
  });

  test('E2E-P2-RTE-005: Applied optimized sequence creates a new Route revision', async ({ dispatcherPage }) => {
    let applied = false;
    await dispatcherPage.route(`**/api/routes/${routeId}/apply-optimization`, (route) => {
      applied = true;
      return route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          id: routeId, code: 'RTE-COL-KND', name: 'Colombo to Kandy',
          originLocationId: '20000000-0000-0000-0000-000000000001',
          destinationLocationId: '20000000-0000-0000-0000-000000000002',
          plannedDistanceKm: 95, estimatedDurationMinutes: 148, active: true,
          stopLocationIds: [stop2, stop1],
        }),
      });
    });
    await dispatcherPage.route(`**/api/routes/${routeId}/revisions`, (route) => route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify(applied ? [{
        id: 'rev-3', routeId, revisionNumber: 3, code: 'RTE-COL-KND', name: 'Colombo to Kandy',
        originLocationId: '20000000-0000-0000-0000-000000000001',
        destinationLocationId: '20000000-0000-0000-0000-000000000002',
        plannedDistanceKm: 95, estimatedDurationMinutes: 148, active: true,
        stopLocationIds: [stop2, stop1], changedAt: '2026-08-24T13:00:00Z', changedBy: 'dispatcher',
      }] : []),
    }));
    await dispatcherPage.goto('/routes');
    await dispatcherPage.getByRole('button', { name: 'View details' }).first().click();
    await dispatcherPage.getByRole('button', { name: 'Optimize Stops' }).click();
    await expect(dispatcherPage.getByText(/Route Stop Optimizer/)).toBeVisible();
    const applyBtn = dispatcherPage.getByRole('button', { name: 'Apply Optimization' });
    await expect(applyBtn).toBeVisible();
    await applyBtn.click();
    await expect(dispatcherPage.getByText('Optimized route sequence applied successfully')).toBeVisible();
    await expect(dispatcherPage.getByText(/Revision History \(1\)/)).toBeVisible();
    await expect(dispatcherPage.getByText('v3')).toBeVisible();
  });
});
