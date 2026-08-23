import { test, expect } from '@playwright/test';
import { createRule, deleteRule, deliveries, notifications, unique } from '../../helpers/notificationTestApi';
import {
  captureTripEvent, inbox, openTripLogs, operationFor, serverTripEvents, setupRealAdmin, waitForOperationStatus,
} from '../../helpers/offlineSyncTestApi';

const tripId = '60000000-0000-0000-0000-000000000006';

test.describe('US-71 Trip operational events offline', () => {
  test.describe.configure({ timeout: 90_000 });
  test('E2E-OFF-005: capture all supported Trip operational event families offline', async ({ page, context, request }, testInfo) => {
    await setupRealAdmin(page, request);
    await openTripLogs(page, tripId);
    await context.setOffline(true);
    try {
      for (const family of ['CHECKPOINT', 'DELAY', 'INCIDENT'] as const) {
        await captureTripEvent(page, family, unique(`OFF005-${family}`, testInfo));
      }
      const operations = (await (await import('../../helpers/offlineSyncTestApi')).readOfflineOperations(page))
        .filter(item => item.aggregateId === tripId);
      expect(new Set(operations.map(item => item.operationType))).toEqual(new Set([
        'TRIP_CHECKPOINT_RECORD', 'TRIP_DELAY_RECORD', 'TRIP_INCIDENT_RECORD',
      ]));
      expect(operations.every(item => item.status === 'PENDING')).toBe(true);
    } finally {
      await context.setOffline(false).catch(() => undefined);
    }
  });

  test('E2E-OFF-006: reconnect applies Trip event exactly once', async ({ page, context, request }, testInfo) => {
    const tokens = await setupRealAdmin(page, request);
    const marker = unique('OFF006', testInfo);
    await openTripLogs(page, tripId);
    await context.setOffline(true);
    await captureTripEvent(page, 'CHECKPOINT', marker);
    const operation = await operationFor(page, tripId, 'TRIP_CHECKPOINT_RECORD');
    await context.setOffline(false);
    await waitForOperationStatus(page, operation.operationId, 'SYNCED');
    await expect.poll(async () => (await serverTripEvents(request, tokens, tripId))
      .filter(item => item.locationDescription === marker).length).toBe(1);
    const stored = await inbox(request, tokens, operation.operationId);
    expect(stored.result_status ?? stored.resultStatus ?? stored.RESULT_STATUS).toBe('APPLIED');
  });

  test('E2E-OFF-007: synced delay produces the normal notification side effect once', async ({ page, context, request }, testInfo) => {
    const tokens = await setupRealAdmin(page, request);
    const actor = await (await request.get('/api/auth/me', { headers: { Authorization: `Bearer ${tokens.accessToken}` } })).json();
    const marker = unique('OFF007', testInfo);
    const rule = await createRule(request, tokens, { name: unique('OFF007 rule', testInfo), channel: 'IN_APP',
      recipientType: 'USER', recipientValue: actor.username });
    try {
      await openTripLogs(page, tripId);
      await context.setOffline(true);
      await captureTripEvent(page, 'DELAY', marker);
      const operation = await operationFor(page, tripId, 'TRIP_DELAY_RECORD');
      await context.setOffline(false);
      await waitForOperationStatus(page, operation.operationId, 'SYNCED');
      await expect.poll(async () => (await serverTripEvents(request, tokens, tripId)).filter(item => item.reason === marker).length).toBe(1);
      await expect.poll(async () => (await notifications(request, tokens)).filter(item => item.message.includes(marker)).length,
        { timeout: 20_000 }).toBe(1);
      const notification = (await notifications(request, tokens)).find(item => item.message.includes(marker))!;
      expect((await deliveries(request, tokens)).filter(item => item.ruleId === rule.id && item.eventId === notification.eventId)).toHaveLength(1);
      const stored = await inbox(request, tokens, operation.operationId);
      expect(stored.result_status ?? stored.resultStatus ?? stored.RESULT_STATUS).toBe('APPLIED');
    } finally {
      await deleteRule(request, tokens, rule.id);
    }
  });
});
