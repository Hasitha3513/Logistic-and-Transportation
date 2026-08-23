import { test, expect } from '@playwright/test';
import { authenticatePage, provisionUser, unique } from '../../helpers/notificationTestApi';
import {
  captureVehicleReading, closeVehicleDetails, configureOutcome, createVehicle, deferOperation, markOperationDue,
  openVehicleReadings, operationFor, readOfflineOperations, releaseOutcome, restoreSyncBackend,
  serverVehicleReadings, setSyncBackendUnavailable, setupRealAdmin, waitForOperationStatus, waitForRetryScheduled,
} from '../../helpers/offlineSyncTestApi';

test.describe('US-71 manual recovery, discard, global status, and durable resume', () => {
  test.describe.configure({ timeout: 90_000 });
  test('E2E-OFF-012: Manual Sync now sends due work', async ({ page, request }, testInfo) => {
    const tokens = await setupRealAdmin(page, request);
    const vehicle = await createVehicle(request, tokens, testInfo, 'OFF012');
    await openVehicleReadings(page, vehicle.id);
    await setSyncBackendUnavailable(page);
    await captureVehicleReading(page, 1212, 'OFF-012');
    const operation = await waitForRetryScheduled(page, (await operationFor(page, vehicle.id)).operationId);
    await deferOperation(page, operation.operationId, 120_000);
    await restoreSyncBackend(page);
    await page.reload();
    await expect(page.getByText('Vehicle Mileage & Readings')).toBeVisible({ timeout: 20_000 });
    await closeVehicleDetails(page);
    await page.getByRole('button', { name: /Offline synchronization status/ }).click();
    const syncDrawer = page.getByRole('dialog', { name: 'Offline synchronization' });
    await expect(syncDrawer.getByRole('rowheader', { name: 'Pending' }).locator('..')).toContainText('1');
    await expect(syncDrawer.getByRole('button', { name: 'Sync now' })).toBeEnabled();
    await markOperationDue(page, operation.operationId);
    await syncDrawer.getByRole('button', { name: 'Sync now' }).dispatchEvent('click');
    await waitForOperationStatus(page, operation.operationId, 'SYNCED');
    expect((await serverVehicleReadings(request, tokens, vehicle.id)).filter(item => item.notes === 'OFF-012')).toHaveLength(1);
  });

  test('E2E-OFF-013: Discard confirmation removes only selected terminal item', async ({ page, context, request }, testInfo) => {
    const tokens = await setupRealAdmin(page, request);
    const first = await createVehicle(request, tokens, testInfo, 'OFF013A');
    const second = await createVehicle(request, tokens, testInfo, 'OFF013B');
    for (const [vehicle, mode] of [[first, 'CONFLICT'], [second, 'REJECTED']] as const) {
      await openVehicleReadings(page, vehicle.id);
      await context.setOffline(true);
      await captureVehicleReading(page, mode === 'CONFLICT' ? 1301 : 1302, `OFF-013-${mode}`);
      const operation = await operationFor(page, vehicle.id);
      await configureOutcome(request, tokens, operation.operationId, mode);
      await context.setOffline(false);
      await waitForOperationStatus(page, operation.operationId, mode === 'CONFLICT' ? 'CONFLICT' : 'FAILED');
    }
    const selected = await operationFor(page, first.id);
    const remaining = await operationFor(page, second.id);
    await closeVehicleDetails(page);
    await page.getByRole('button', { name: /Offline synchronization status/ }).click();
    const item = page.locator('.ant-list-item').filter({ hasText: first.id });
    await item.getByRole('button', { name: 'Discard' }).click();
    const confirm = page.getByRole('dialog', { name: 'Discard unsynchronized operation?' });
    await confirm.getByRole('button', { name: 'Cancel' }).click();
    expect((await readOfflineOperations(page)).some(op => op.operationId === selected.operationId)).toBe(true);
    await item.getByRole('button', { name: 'Discard' }).click();
    await page.getByRole('dialog', { name: 'Discard unsynchronized operation?' })
      .getByRole('button', { name: 'Discard local copy' }).click();
    await expect.poll(async () => (await readOfflineOperations(page)).some(op => op.operationId === selected.operationId)).toBe(false);
    expect((await readOfflineOperations(page)).some(op => op.operationId === remaining.operationId)).toBe(true);
    expect((await serverVehicleReadings(request, tokens, first.id)).filter(item => item.notes === 'OFF-013-CONFLICT')).toHaveLength(0);
  });

  test('E2E-OFF-014: global counts reflect queue states and remain owner-scoped', async ({ page, context, request }, testInfo) => {
    const admin = await setupRealAdmin(page, request);
    const pendingVehicle = await createVehicle(request, admin, testInfo, 'OFF014P');
    const conflictVehicle = await createVehicle(request, admin, testInfo, 'OFF014C');
    const failedVehicle = await createVehicle(request, admin, testInfo, 'OFF014F');
    const syncingVehicle = await createVehicle(request, admin, testInfo, 'OFF014S');
    for (const [vehicle, mode, status] of [
      [pendingVehicle, 'RETRYABLE', 'PENDING'], [conflictVehicle, 'CONFLICT', 'CONFLICT'], [failedVehicle, 'REJECTED', 'FAILED'],
    ] as const) {
      await openVehicleReadings(page, vehicle.id);
      await context.setOffline(true);
      await captureVehicleReading(page, 1400 + Math.floor(Math.random() * 50), `OFF-014-${mode}`);
      const operation = await operationFor(page, vehicle.id);
      await configureOutcome(request, admin, operation.operationId, mode, mode === 'RETRYABLE' ? 10 : 1);
      await context.setOffline(false);
      if (status === 'PENDING') {
        await waitForRetryScheduled(page, operation.operationId);
        await deferOperation(page, operation.operationId, 120_000);
      } else {
        await waitForOperationStatus(page, operation.operationId, status);
      }
    }
    await openVehicleReadings(page, syncingVehicle.id);
    await context.setOffline(true);
    await captureVehicleReading(page, 1499, 'OFF-014-BLOCK');
    const syncing = await operationFor(page, syncingVehicle.id);
    await configureOutcome(request, admin, syncing.operationId, 'BLOCK');
    await context.setOffline(false);
    await waitForOperationStatus(page, syncing.operationId, 'SYNCING');
    await closeVehicleDetails(page);
    await page.getByRole('button', { name: /Offline synchronization status/ }).click();
    const drawer = page.getByRole('dialog', { name: 'Offline synchronization' });
    await expect(drawer.getByRole('rowheader', { name: 'Pending' }).locator('..')).toContainText('1');
    await expect(drawer.getByRole('rowheader', { name: 'Syncing' }).locator('..')).toContainText('1');
    await expect(drawer.getByRole('rowheader', { name: 'Conflicts' }).locator('..')).toContainText('1');
    await expect(drawer.getByRole('rowheader', { name: 'Failed' }).locator('..')).toContainText('1');

    const other = await provisionUser(request, admin, unique('off014-owner', testInfo), ['DASHBOARD_VIEW']);
    const otherPage = await context.newPage();
    await authenticatePage(otherPage, other.tokens);
    await otherPage.goto('/');
    await otherPage.getByRole('button', { name: /Offline synchronization status/ }).click();
    await expect(otherPage.getByText('No offline operations need attention')).toBeVisible();
    await otherPage.close();
    await releaseOutcome(request, admin, syncing.operationId);
    await waitForOperationStatus(page, syncing.operationId, 'SYNCED');
  });

  test('E2E-OFF-015: queue survives a new page in the same storage context and resumes', async ({ page, context, request }, testInfo) => {
    const tokens = await setupRealAdmin(page, request);
    const vehicle = await createVehicle(request, tokens, testInfo, 'OFF015');
    await context.route('**/api/offline-sync/operations', route => route.abort('connectionrefused'));
    await openVehicleReadings(page, vehicle.id);
    await captureVehicleReading(page, 1515, 'OFF-015');
    const operation = await waitForOperationStatus(page, (await operationFor(page, vehicle.id)).operationId, 'PENDING');
    await page.close();
    const resumed = await context.newPage();
    await resumed.goto(`/fleet/vehicles?vehicleId=${vehicle.id}`);
    await expect(resumed.getByText('Vehicle Mileage & Readings')).toBeVisible({ timeout: 20_000 });
    expect((await operationFor(resumed, vehicle.id)).operationId).toBe(operation.operationId);
    await context.unroute('**/api/offline-sync/operations');
    await markOperationDue(resumed, operation.operationId);
    await resumed.evaluate(() => window.dispatchEvent(new Event('online')));
    await waitForOperationStatus(resumed, operation.operationId, 'SYNCED');
    expect((await serverVehicleReadings(request, tokens, vehicle.id)).filter(item => item.notes === 'OFF-015')).toHaveLength(1);
  });
});
