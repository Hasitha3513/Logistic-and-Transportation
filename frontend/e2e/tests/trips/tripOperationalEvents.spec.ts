import { test, expect } from '../../fixtures/authFixtures';
import { TripDetailsPage } from '../../pages/TripDetailsPage';

test.describe('@trips En-Route Checkpoints & Operational Events (US-13)', () => {
  const tripId = '64000000-0000-4000-8000-000000000001';

  test.beforeEach(async ({ dispatcherPage }) => {
    let operationalEvents = [
      {
        id: 'evt-init-1',
        tripId,
        eventType: 'CHECKPOINT',
        checkpointType: 'DEPARTURE',
        locationDescription: 'Central Warehouse Gate 1',
        occurredAt: '2026-08-19T08:00:00Z',
        remarks: 'Vehicle departed origin depot',
        recordedBy: 'dispatcher',
        createdAt: '2026-08-19T08:00:00Z',
        updatedAt: '2026-08-19T08:00:00Z',
      },
    ];

    await dispatcherPage.route(`**/api/**/trips/${tripId}/status-history*`, (r) =>
      r.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify([
          {
            id: 'th-1',
            tripId,
            fromStatus: 'ASSIGNED',
            toStatus: 'IN_PROGRESS',
            action: 'START',
            actor: 'dispatcher',
            occurredAt: '2026-08-19T08:00:00Z',
          },
        ]),
      })
    );

    await dispatcherPage.route(`**/api/**/trips/${tripId}/operational-events*`, (r) =>
      r.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(operationalEvents),
      })
    );

    await dispatcherPage.route(`**/api/**/trips/${tripId}/checkpoints*`, async (r) => {
      const payload = r.request().postDataJSON();
      const created = {
        id: 'evt-chk-new',
        tripId,
        eventType: 'CHECKPOINT',
        ...payload,
        recordedBy: 'dispatcher',
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
      };
      operationalEvents = [...operationalEvents, created];
      return r.fulfill({
        status: 201,
        contentType: 'application/json',
        body: JSON.stringify(created),
      });
    });

    await dispatcherPage.route(`**/api/**/trips/${tripId}/delays*`, async (r) => {
      const payload = r.request().postDataJSON();
      const created = {
        id: 'evt-delay-new',
        tripId,
        eventType: 'DELAY',
        ...payload,
        recordedBy: 'dispatcher',
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
      };
      operationalEvents = [...operationalEvents, created];
      return r.fulfill({
        status: 201,
        contentType: 'application/json',
        body: JSON.stringify(created),
      });
    });

    await dispatcherPage.route(`**/api/**/trips/${tripId}/incidents*`, async (r) => {
      const payload = r.request().postDataJSON();
      const created = {
        id: 'evt-inc-new',
        tripId,
        eventType: 'INCIDENT',
        ...payload,
        reason: payload.description,
        recordedBy: 'dispatcher',
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
      };
      operationalEvents = [...operationalEvents, created];
      return r.fulfill({
        status: 201,
        contentType: 'application/json',
        body: JSON.stringify(created),
      });
    });

    await dispatcherPage.route(`**/api/**/trips/${tripId}`, (r) =>
      r.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          id: tripId,
          tripNumber: 'TRP-2026-LOG-001',
          status: 'IN_PROGRESS',
          originLocationId: 'loc-1',
          destinationLocationId: 'loc-2',
          requestedStartTime: '2026-08-19T08:00:00Z',
          requestedEndTime: '2026-08-19T14:00:00Z',
          priority: 'HIGH',
          vehicleId: '32000000-0000-0000-0000-000000000001',
          driverId: '33000000-0000-0000-0000-000000000001',
          active: true,
        }),
      })
    );

    await dispatcherPage.route('**/api/**/customers*', (r) =>
      r.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify([]),
      })
    );
    await dispatcherPage.route('**/api/**/locations*', (r) =>
      r.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify([
          { id: 'loc-1', name: 'Colombo Depot' },
          { id: 'loc-2', name: 'Kandy Hub' },
        ]),
      })
    );
    await dispatcherPage.route('**/api/**/vehicles*', (r) =>
      r.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify([]),
      })
    );
    await dispatcherPage.route('**/api/**/drivers*', (r) =>
      r.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify([]),
      })
    );
    await dispatcherPage.route('**/api/**/routes*', (r) =>
      r.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify([]),
      })
    );
  });

  test('E2E-TRIP-008: Record and view en-route checkpoints, delays, and incidents', async ({
    dispatcherPage,
  }) => {
    const page = new TripDetailsPage(dispatcherPage);
    await page.goto(tripId);

    // Switch to Trip Logs tab
    await dispatcherPage.getByRole('tab', { name: 'Trip Logs' }).click();

    const section = dispatcherPage
      .locator('.trip-detail-card')
      .filter({ hasText: 'En-Route Checkpoints & Operational Events' });

    await expect(section).toBeVisible();
    await expect(section).toContainText('Central Warehouse Gate 1');
    await expect(section).toContainText('Departure');

    // 1. Record Checkpoint
    const checkpointBtn = section.getByRole('button', { name: /Record Checkpoint/i });
    await expect(checkpointBtn).toBeVisible();
    await checkpointBtn.click();

    const checkpointModal = dispatcherPage.locator('.ant-modal-content').filter({ hasText: 'Record En-Route Checkpoint' });
    await expect(checkpointModal).toBeVisible();

    await checkpointModal.locator('.ant-select').first().click();
    await dispatcherPage.locator('.ant-select-item-option-content').filter({ hasText: 'Pickup Point' }).click();

    await checkpointModal.locator('input#locationDescription').fill('Expressway Waypoint Alpha');
    await checkpointModal.getByRole('button', { name: 'Record Checkpoint' }).click();

    await expect(checkpointModal).not.toBeVisible();
    await expect(section).toContainText('Expressway Waypoint Alpha');
    await expect(section).toContainText('Pickup Point');

    // 2. Record Delay
    const delayBtn = section.getByRole('button', { name: /Record Delay/i });
    await delayBtn.click();

    const delayModal = dispatcherPage.locator('.ant-modal-content').filter({ hasText: 'Record Operational Delay' });
    await expect(delayModal).toBeVisible();

    await delayModal.locator('input#delayMinutes').fill('35');
    await delayModal.locator('input#reason').fill('Expressway resurfacing single-lane block');
    await delayModal.getByRole('button', { name: 'Record Delay' }).click();

    await expect(delayModal).not.toBeVisible();
    await expect(section).toContainText('Delay: 35 mins');
    await expect(section).toContainText('Expressway resurfacing single-lane block');

    // 3. Record Incident
    const incidentBtn = section.getByRole('button', { name: /Record Incident/i });
    await incidentBtn.click();

    const incidentModal = dispatcherPage.locator('.ant-modal-content').filter({ hasText: 'Record Operational Incident' });
    await expect(incidentModal).toBeVisible();

    await incidentModal.locator('.ant-select').first().click();
    await dispatcherPage.locator('.ant-select-item-option-content').filter({ hasText: 'Medium' }).click();

    await incidentModal.locator('input#description').fill('Rear tire pressure loss, replaced with spare');
    await incidentModal.getByRole('button', { name: 'Record Incident' }).click();

    await expect(incidentModal).not.toBeVisible();
    await expect(section).toContainText('Incident: Medium');
    await expect(section).toContainText('Rear tire pressure loss, replaced with spare');
  });

  test('E2E-TRIP-008-NEG: Validate required fields in checkpoint and delay modals', async ({
    dispatcherPage,
  }) => {
    const page = new TripDetailsPage(dispatcherPage);
    await page.goto(tripId);

    await dispatcherPage.getByRole('tab', { name: 'Trip Logs' }).click();

    const section = dispatcherPage
      .locator('.trip-detail-card')
      .filter({ hasText: 'En-Route Checkpoints & Operational Events' });

    // Checkpoint validation
    await section.getByRole('button', { name: /Record Checkpoint/i }).click();
    const checkpointModal = dispatcherPage.locator('.ant-modal-content').filter({ hasText: 'Record En-Route Checkpoint' });
    await expect(checkpointModal).toBeVisible();

    await checkpointModal.getByRole('button', { name: 'Record Checkpoint' }).click();
    await expect(checkpointModal).toContainText('Please select a checkpoint type');
    await checkpointModal.getByRole('button', { name: 'Cancel' }).click();
    await expect(checkpointModal).not.toBeVisible();

    // Delay validation
    await section.getByRole('button', { name: /Record Delay/i }).click();
    const delayModal = dispatcherPage.locator('.ant-modal-content').filter({ hasText: 'Record Operational Delay' });
    await expect(delayModal).toBeVisible();
    await delayModal.getByRole('button', { name: 'Record Delay' }).click();
    await expect(delayModal).toContainText('Please enter delay duration in minutes');
    await expect(delayModal).toContainText('Please enter the delay reason');
    await delayModal.getByRole('button', { name: 'Cancel' }).click();
    await expect(delayModal).not.toBeVisible();
  });
});
