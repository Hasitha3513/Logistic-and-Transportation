import { test, expect } from '@playwright/test';
import { NotificationCenterObject } from '../../pages/NotificationCenter';
import {
  adminLogin, authenticatePage, createRule, deleteRule, deliveries, notifications, provisionUser, triggerDelay,
  triggerIncident, unique, unreadCount,
} from '../../helpers/notificationTestApi';

async function setupRecipient(request: Parameters<typeof adminLogin>[0], testInfo: Parameters<typeof unique>[1], prefix: string) {
  const admin = await adminLogin(request);
  const actor = await provisionUser(request, admin, unique(prefix, testInfo), ['DASHBOARD_VIEW', 'NOTIFICATION_VIEW']);
  const rule = await createRule(request, admin, {
    name: unique(`${prefix} rule`, testInfo), channel: 'IN_APP', recipientType: 'USER', recipientValue: actor.username,
  });
  return { admin, actor, rule };
}

test.describe('US-77 Notification Center vertical flow', () => {
  test('E2E-NOT-006: MVP trip-delay event creates a resolved notification', async ({ request }, testInfo) => {
    const { admin, actor, rule } = await setupRecipient(request, testInfo, 'not006');
    const reason = unique('vertical-flow', testInfo);
    const event = await triggerDelay(request, admin, reason);
    await expect.poll(async () => (await notifications(request, actor.tokens)).find((item) => item.eventId === event.id)).toBeTruthy();
    const item = (await notifications(request, actor.tokens)).find((candidate) => candidate.eventId === event.id)!;
    expect(item).toMatchObject({ channel: 'IN_APP', status: 'SENT', title: 'Trip TRIP-DEMO-006 delayed' });
    expect(item.message).toContain(reason);
    await deleteRule(request, admin, rule.id);
  });

  test('E2E-NOT-007: Unread badge increments by the event delta', async ({ page, request }, testInfo) => {
    const { admin, actor, rule } = await setupRecipient(request, testInfo, 'not007');
    const before = await unreadCount(request, actor.tokens);
    await authenticatePage(page, actor.tokens);
    await page.goto('/');
    const event = await triggerDelay(request, admin, unique('badge', testInfo));
    await expect.poll(async () => (await deliveries(request, admin))
      .some((item) => item.ruleId === rule.id && item.eventId === event.id), { timeout: 20_000 }).toBe(true);
    await expect.poll(() => unreadCount(request, actor.tokens)).toBeGreaterThanOrEqual(before + 1);
    await request.patch(`/api/notification-rules/${rule.id}/disable`, { headers: { Authorization: `Bearer ${admin.accessToken}` } });
    await page.reload();
    await expect(page.getByRole('button', { name: /Open notifications \([1-9][0-9]* unread\)/ })).toBeVisible();
    await deleteRule(request, admin, rule.id);
  });

  test('E2E-NOT-008: Open Notification Center and inspect notification', async ({ page, request }, testInfo) => {
    const { admin, actor, rule } = await setupRecipient(request, testInfo, 'not008');
    const reason = unique('inspect-body', testInfo);
    const event = await triggerDelay(request, admin, reason);
    await expect.poll(async () => (await notifications(request, actor.tokens)).some((item) => item.eventId === event.id)).toBe(true);
    await request.patch(`/api/notification-rules/${rule.id}/disable`, { headers: { Authorization: `Bearer ${admin.accessToken}` } });
    await authenticatePage(page, actor.tokens);
    await page.goto('/');
    const center = new NotificationCenterObject(page);
    await center.open();
    const item = page.locator('.ant-list-item').filter({ hasText: reason });
    await expect(item).toContainText(reason);
    await expect(item).toContainText('WARNING');
    await expect(item).toContainText('In-app');
    await expect(item.getByRole('button', { name: /Mark Trip TRIP-DEMO-006 delayed as read/ })).toBeVisible();
    await deleteRule(request, admin, rule.id);
  });

  test('E2E-NOT-009: Mark one notification read idempotently', async ({ page, request }, testInfo) => {
    const admin = await adminLogin(request);
    const actor = await provisionUser(request, admin, unique('not009', testInfo), ['DASHBOARD_VIEW', 'NOTIFICATION_VIEW']);
    const rule = await createRule(request, admin, { name: unique('not009 rule', testInfo), eventType: 'TRIP_INCIDENT_RECORDED',
      templateCode: 'TRIP_INCIDENT', channel: 'IN_APP', recipientType: 'USER', recipientValue: actor.username });
    const reason = unique('read-one', testInfo);
    const event = await triggerIncident(request, admin, reason);
    await expect.poll(async () => (await notifications(request, actor.tokens)).some((item) => item.eventId === event.id)).toBe(true);
    await request.patch(`/api/notification-rules/${rule.id}/disable`, { headers: { Authorization: `Bearer ${admin.accessToken}` } });
    await authenticatePage(page, actor.tokens);
    await page.goto('/');
    const center = new NotificationCenterObject(page);
    await center.open();
    await page.locator('.ant-list-item').filter({ hasText: reason }).getByRole('button', { name: /Mark .* as read/ }).click();
    await expect.poll(async () => (await notifications(request, actor.tokens)).find((item) => item.eventId === event.id)?.status).toBe('READ');
    const read = (await notifications(request, actor.tokens)).find((item) => item.eventId === event.id)!;
    expect(read.status).toBe('READ');
    const repeat = await request.patch(`/api/notifications/${read.id}/read`, { headers: { Authorization: `Bearer ${actor.tokens.accessToken}` } });
    expect(repeat.status()).toBe(200);
    expect((await notifications(request, actor.tokens)).filter((item) => item.eventId === event.id)).toHaveLength(1);
    await deleteRule(request, admin, rule.id);
  });

  test('E2E-NOT-010: Mark all test-owned notifications read', async ({ page, request }, testInfo) => {
    const { admin, actor, rule } = await setupRecipient(request, testInfo, 'not010');
    const before = await unreadCount(request, actor.tokens);
    const first = await triggerDelay(request, admin, unique('all-one', testInfo));
    const second = await triggerDelay(request, admin, unique('all-two', testInfo));
    await expect.poll(async () => (await deliveries(request, admin))
      .filter((item) => item.ruleId === rule.id && [first.id, second.id].includes(item.eventId)).length).toBe(2);
    await expect.poll(() => unreadCount(request, actor.tokens)).toBeGreaterThanOrEqual(before + 2);
    await request.patch(`/api/notification-rules/${rule.id}/disable`, { headers: { Authorization: `Bearer ${admin.accessToken}` } });
    await authenticatePage(page, actor.tokens);
    await page.goto('/');
    const center = new NotificationCenterObject(page);
    await center.open();
    await page.getByRole('button', { name: 'Mark all as read' }).click();
    await expect.poll(() => unreadCount(request, actor.tokens)).toBe(0);
    const owned = (await notifications(request, actor.tokens)).filter((item) => [first.id, second.id].includes(item.eventId) && item.channel === 'IN_APP');
    expect(owned).toHaveLength(2);
    expect(owned.every((item) => item.status === 'READ')).toBe(true);
    await deleteRule(request, admin, rule.id);
  });
});
