import { test, expect } from '@playwright/test';
import { authenticatePage, login, provisionUser, unique } from '../../helpers/notificationTestApi';
import {
  captureVehicleReading, closeVehicleDetails, configureOutcome, createVehicle, openVehicleReadings, operationFor,
  readOfflineOperations, serverVehicleReadings, setupRealAdmin, waitForOperationStatus, waitForRetryScheduled,
} from '../../helpers/offlineSyncTestApi';

test.describe('US-71 independent, authorization, conflict, and retry outcomes', () => {
  test.describe.configure({ timeout: 90_000 });
  test('E2E-OFF-008: mixed batch preserves independent result outcomes', async ({ page, context, request }, testInfo) => {
    const tokens = await setupRealAdmin(page, request);
    const records: Array<{ id: string; operationId: string; expected: 'SYNCED' | 'FAILED' | 'CONFLICT' | 'PENDING' }> = [];
    const vehicle = await createVehicle(request, tokens, testInfo, 'OFF008');
    await openVehicleReadings(page, vehicle.id);
    await context.setOffline(true);
    try {
      for (const [index, mode, expected] of [
        [0, 'APPLIED', 'SYNCED'], [1, 'REJECTED', 'FAILED'], [2, 'CONFLICT', 'CONFLICT'], [3, 'RETRYABLE', 'PENDING'],
      ] as const) {
        const marker = `OFF-008-${mode}`;
        await captureVehicleReading(page, 800 + index, marker);
        const operation = (await readOfflineOperations(page)).find(item => item.payload.notes === marker)!;
        if (mode !== 'APPLIED') await configureOutcome(request, tokens, operation.operationId, mode);
        records.push({ id: vehicle.id, operationId: operation.operationId, expected });
      }
      await context.setOffline(false);
      for (const record of records) await waitForOperationStatus(page, record.operationId, record.expected);
      const retryRecord = (await readOfflineOperations(page)).find(item => item.operationId === records[3].operationId)!;
      expect(retryRecord.attemptCount).toBeGreaterThanOrEqual(1);
      expect(retryRecord.attemptCount).toBeLessThanOrEqual(10);
      expect(retryRecord.nextAttemptAt).toBeTruthy();
      expect((await serverVehicleReadings(request, tokens, records[0].id)).filter(item => item.notes === 'OFF-008-APPLIED')).toHaveLength(1);
    } finally {
      await context.setOffline(false);
    }
  });

  test('E2E-OFF-009: permission revoked before sync becomes Failed/Forbidden', async ({ page, context, request }, testInfo) => {
    const admin = await setupRealAdmin(page, request);
    const actor = await provisionUser(request, admin, unique('off009', testInfo), [
      'DASHBOARD_VIEW', 'VEHICLE_VIEW', 'VEHICLE_READING_VIEW', 'VEHICLE_READING_CREATE',
    ]);
    const vehicle = await createVehicle(request, admin, testInfo, 'OFF009');
    await authenticatePage(page, actor.tokens);
    await openVehicleReadings(page, vehicle.id);
    await context.setOffline(true);
    await captureVehicleReading(page, 909, 'OFF-009');
    const operation = await operationFor(page, vehicle.id);
    const updatedRole = await request.put(`/api/roles/${actor.role.id}`, { headers: { Authorization: `Bearer ${admin.accessToken}` }, data: {
      name: actor.role.name, description: actor.role.description, active: true,
      permissions: ['DASHBOARD_VIEW', 'VEHICLE_VIEW', 'VEHICLE_READING_VIEW'],
    }});
    expect(updatedRole.ok(), await updatedRole.text()).toBeTruthy();
    const refreshed = await login(request, actor.username, actor.password);
    await page.evaluate(({ accessToken, refreshToken }) => {
      localStorage.setItem('transport.accessToken', accessToken); localStorage.setItem('transport.refreshToken', refreshToken);
    }, refreshed);
    await context.setOffline(false);
    const failed = await waitForOperationStatus(page, operation.operationId, 'FAILED');
    expect(failed.lastErrorCode).toBe('OFFLINE_SYNC_FORBIDDEN');
    expect((await serverVehicleReadings(request, admin, vehicle.id)).filter(item => item.notes === 'OFF-009')).toHaveLength(0);
    await closeVehicleDetails(page);
    await page.getByRole('button', { name: /Offline synchronization status/ }).click();
    const item = page.locator('.ant-list-item').filter({ hasText: vehicle.id });
    await expect(item.getByRole('button', { name: 'Retry' })).toHaveCount(0);
  });

  test('E2E-OFF-010: business conflict is visible with safe actions', async ({ page, context, request }, testInfo) => {
    const tokens = await setupRealAdmin(page, request);
    const vehicle = await createVehicle(request, tokens, testInfo, 'OFF010');
    const baseline = await request.post(`/api/vehicles/${vehicle.id}/readings`, { headers: { Authorization: `Bearer ${tokens.accessToken}` }, data: {
      readingType: 'ODOMETER', value: 1000, recordedAt: new Date(Date.now() - 60_000).toISOString(), notes: 'OFF-010 baseline',
    }});
    expect(baseline.status(), await baseline.text()).toBe(201);
    await openVehicleReadings(page, vehicle.id);
    await context.setOffline(true);
    await captureVehicleReading(page, 900, 'OFF-010 conflict');
    const operation = await operationFor(page, vehicle.id);
    await context.setOffline(false);
    const conflict = await waitForOperationStatus(page, operation.operationId, 'CONFLICT');
    expect(conflict.lastErrorCode).toBe('OFFLINE_SYNC_CONFLICT');
    await closeVehicleDetails(page);
    await page.getByRole('button', { name: /Offline synchronization status/ }).click();
    const item = page.locator('.ant-list-item').filter({ hasText: vehicle.id });
    await expect(item.getByRole('button', { name: 'Open' })).toBeVisible();
    await expect(item.getByRole('button', { name: 'Refresh' })).toBeVisible();
    await expect(item.getByRole('button', { name: 'Discard' })).toBeVisible();
    await expect(item.getByRole('button', { name: 'Retry' })).toHaveCount(0);
  });

  test('E2E-OFF-011: transient failure retains identity and later succeeds', async ({ page, context, request }, testInfo) => {
    const tokens = await setupRealAdmin(page, request);
    const vehicle = await createVehicle(request, tokens, testInfo, 'OFF011');
    await openVehicleReadings(page, vehicle.id);
    await context.setOffline(true);
    await captureVehicleReading(page, 1111, 'OFF-011');
    const operation = await operationFor(page, vehicle.id);
    await configureOutcome(request, tokens, operation.operationId, 'RETRYABLE');
    await context.setOffline(false);
    const pending = await waitForRetryScheduled(page, operation.operationId);
    expect(pending.operationId).toBe(operation.operationId);
    expect(pending.attemptCount).toBe(1);
    expect(pending.nextAttemptAt).toBeTruthy();
    const synced = await waitForOperationStatus(page, operation.operationId, 'SYNCED');
    expect(synced.operationId).toBe(operation.operationId);
    expect(synced.attemptCount).toBe(2);
    expect((await serverVehicleReadings(request, tokens, vehicle.id)).filter(item => item.notes === 'OFF-011')).toHaveLength(1);
  });
});
