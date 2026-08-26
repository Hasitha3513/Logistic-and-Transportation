import { test, expect } from '@playwright/test';
import {
  adminLogin, advanceTime, attempts, createRule, deleteRule, deliveries, processEmail,
  triggerDelay, unique,
} from '../../helpers/notificationTestApi';

const emailPolicy = (fallback: string) => ({
  quietHoursEnabled: false, quietDays: [], suppressionWindowMinutes: 0,
  escalationEnabled: true, escalationDelayMinutes: 0, escalationRecipientType: 'USER',
  escalationRecipientValue: fallback,
});

test.describe('US-77 deterministic EMAIL delivery', () => {
  test('E2E-NOT-012: EMAIL failure never reports false SENT', async ({ request }, testInfo) => {
    const admin = await adminLogin(request);
    const recipient = `e2e-terminal-${unique('not012', testInfo).toLowerCase()}@example.test`;
    const rule = await createRule(request, admin, { name: unique('E2E TERMINAL', testInfo), channel: 'EMAIL',
      recipientType: 'EMAIL_ADDRESS', recipientValue: recipient, ...emailPolicy(process.env.E2E_ADMIN_USERNAME!) });
    const event = await triggerDelay(request, admin, unique('terminal-email', testInfo));
    await expect.poll(async () => (await deliveries(request, admin)).find((item) => item.eventId === event.id && item.ruleId === rule.id)).toBeTruthy();
    const before = (await deliveries(request, admin)).find((item) => item.eventId === event.id && item.ruleId === rule.id)!;
    expect(before.status).toBe('PENDING');
    await processEmail(request, admin);
    await expect.poll(async () => (await deliveries(request, admin)).find((item) => item.notificationId === before.notificationId)?.status).toBe('FAILED');
    const failed = (await deliveries(request, admin)).find((item) => item.notificationId === before.notificationId)!;
    expect(failed.terminalFailure).toBe(true);
    expect((await attempts(request, admin, failed.notificationId))[0]).toMatchObject({ attemptNumber: 1, state: 'FAILED', errorCode: 'E2E_TERMINAL' });
    await deleteRule(request, admin, rule.id);
  });

  test('E2E-NOT-015: Transient EMAIL failure retries once and succeeds', async ({ request }, testInfo) => {
    const admin = await adminLogin(request);
    const recipient = `e2e-retry-${unique('not015', testInfo).toLowerCase()}@example.test`;
    const rule = await createRule(request, admin, { name: unique('E2E RETRY', testInfo), channel: 'EMAIL',
      recipientType: 'EMAIL_ADDRESS', recipientValue: recipient, ...emailPolicy(process.env.E2E_ADMIN_USERNAME!) });
    const event = await triggerDelay(request, admin, unique('retry-email', testInfo));
    await expect.poll(async () => (await deliveries(request, admin)).find((item) => item.eventId === event.id && item.ruleId === rule.id)).toBeTruthy();
    const delivery = (await deliveries(request, admin)).find((item) => item.eventId === event.id && item.ruleId === rule.id)!;
    await processEmail(request, admin);
    await expect.poll(async () => (await deliveries(request, admin)).find((item) => item.notificationId === delivery.notificationId)?.attemptCount).toBe(1);
    const pending = (await deliveries(request, admin)).find((item) => item.notificationId === delivery.notificationId)!;
    expect(pending.status).toBe('PENDING');
    expect(pending.nextDeliveryAt).toBeTruthy();
    expect(await attempts(request, admin, delivery.notificationId)).toEqual([
      expect.objectContaining({ attemptNumber: 1, state: 'FAILED', errorCode: 'E2E_TRANSIENT' }),
    ]);
    await advanceTime(request, admin);
    await processEmail(request, admin);
    await expect.poll(async () => (await deliveries(request, admin)).find((item) => item.notificationId === delivery.notificationId)?.status).toBe('SENT');
    const history = await attempts(request, admin, delivery.notificationId);
    expect(history).toHaveLength(2);
    expect(history[1]).toMatchObject({ attemptNumber: 2, state: 'SUCCEEDED' });
    expect(history[1].providerMessageId).toMatch(/^e2e-provider-/);
    await deleteRule(request, admin, rule.id);
  });
});
