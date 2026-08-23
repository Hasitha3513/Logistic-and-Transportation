import { test, expect } from '@playwright/test';
import {
  captureVehicleReading, createVehicle, currentUser, inbox, openVehicleReadings, operationFor,
  readOfflineOperations, serverVehicleReadings, setSyncBackendUnavailable, setupRealAdmin,
  restoreSyncBackend, waitForOperationStatus,
} from '../../helpers/offlineSyncTestApi';

test.describe('US-71 Vehicle offline capture and replay', () => {
  test.describe.configure({ timeout: 90_000 });
  test('E2E-OFF-001: capture a manual Vehicle reading while offline', async ({ page, context, request }, testInfo) => {
    const tokens = await setupRealAdmin(page, request);
    const actor = await currentUser(request, tokens);
    const vehicle = await createVehicle(request, tokens, testInfo, 'OFF001');
    await openVehicleReadings(page, vehicle.id);
    await context.setOffline(true);
    try {
      await captureVehicleReading(page, 101, 'OFF-001');
      const operation = await operationFor(page, vehicle.id);
      expect(operation).toMatchObject({ operationType: 'VEHICLE_READING_RECORD', aggregateType: 'VEHICLE',
        aggregateId: vehicle.id, ownerUserId: actor.id, status: 'PENDING' });
      expect(await serverVehicleReadings(request, tokens, vehicle.id)).toHaveLength(0);
    } finally {
      await context.setOffline(false);
    }
  });

  test('E2E-OFF-002: queued reading survives reload/application lifecycle', async ({ page, request }, testInfo) => {
    const tokens = await setupRealAdmin(page, request);
    const vehicle = await createVehicle(request, tokens, testInfo, 'OFF002');
    await openVehicleReadings(page, vehicle.id);
    await setSyncBackendUnavailable(page);
    try {
      await captureVehicleReading(page, 202, 'OFF-002');
      const before = await operationFor(page, vehicle.id);
      await page.reload();
      await openVehicleReadings(page, vehicle.id);
      const after = await operationFor(page, vehicle.id);
      expect(after.operationId).toBe(before.operationId);
      expect(after.status).toBe('PENDING');
      await expect(page.getByText('Pending', { exact: true })).toBeVisible();
    } finally {
      await restoreSyncBackend(page);
    }
  });

  test('E2E-OFF-003: reconnect automatically applies Vehicle reading', async ({ page, context, request }, testInfo) => {
    const tokens = await setupRealAdmin(page, request);
    const vehicle = await createVehicle(request, tokens, testInfo, 'OFF003');
    await openVehicleReadings(page, vehicle.id);
    await context.setOffline(true);
    await captureVehicleReading(page, 303, 'OFF-003');
    const operation = await operationFor(page, vehicle.id);
    await context.setOffline(false);
    const synced = await waitForOperationStatus(page, operation.operationId, 'SYNCED');
    expect(synced.operationId).toBe(operation.operationId);
    await expect.poll(async () => (await serverVehicleReadings(request, tokens, vehicle.id))
      .filter(item => item.notes === 'OFF-003').length).toBe(1);
    expect((await readOfflineOperations(page)).filter(item => item.operationId === operation.operationId)).toHaveLength(1);
  });

  test('E2E-OFF-004: repeated operation applies server mutation exactly once', async ({ page, context, request }, testInfo) => {
    const tokens = await setupRealAdmin(page, request);
    const vehicle = await createVehicle(request, tokens, testInfo, 'OFF004');
    await openVehicleReadings(page, vehicle.id);
    await context.setOffline(true);
    await captureVehicleReading(page, 404, 'OFF-004');
    const operation = await operationFor(page, vehicle.id);
    await context.setOffline(false);
    await waitForOperationStatus(page, operation.operationId, 'SYNCED');
    const envelope = {
      operationId: operation.operationId, operationVersion: operation.operationVersion,
      operationType: operation.operationType, aggregateType: operation.aggregateType,
      aggregateId: operation.aggregateId, payload: operation.payload,
      clientCreatedAt: operation.clientCreatedAt, clientUpdatedAt: operation.clientUpdatedAt,
      clientInstanceId: operation.clientInstanceId, idempotencyKey: operation.idempotencyKey, baseVersion: null,
    };
    const replay = await request.post('/api/offline-sync/operations', {
      headers: { Authorization: `Bearer ${tokens.accessToken}` }, data: { operations: [envelope] },
    });
    expect(replay.ok(), await replay.text()).toBeTruthy();
    expect((await replay.json()).results[0].status).toBe('ALREADY_APPLIED');
    const stored = await inbox(request, tokens, operation.operationId);
    expect(stored.result_status ?? stored.resultStatus ?? stored.RESULT_STATUS).toBe('APPLIED');
    expect((await serverVehicleReadings(request, tokens, vehicle.id)).filter(item => item.notes === 'OFF-004')).toHaveLength(1);
  });
});
