import { test, expect } from '@playwright/test';
import { NotificationRulesPageObject } from '../../pages/NotificationRulesPage';
import {
  adminLogin, authenticatePage, backendNow, createOperationalTrip, createRule, deleteRule, deliveries, headers, notifications,
  provisionUser, triggerDelay, unique,
} from '../../helpers/notificationTestApi';

const dayName = (date: Date) => ['SUNDAY', 'MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY'][date.getUTCDay()];
const time = (date: Date) => `${String(date.getUTCHours()).padStart(2, '0')}:${String(date.getUTCMinutes()).padStart(2, '0')}:00`;

test.describe('US-77 notification policies', () => {
  test('E2E-NOT-011: Configure EMAIL rule with template and policies through UI', async ({ page, request }, testInfo) => {
    const admin = await adminLogin(request);
    await authenticatePage(page, admin);
    const name = unique('E2E EMAIL UI', testInfo);
    const rules = new NotificationRulesPageObject(page);
    await rules.open();
    const modal = await rules.openCreate();
    await modal.getByLabel('Rule Name').fill(name);
    await rules.select(modal, 'Channel', 'Email');
    await modal.getByLabel('Role Name').fill('LOCAL_MVP_ADMIN');
    await rules.select(modal, 'Severity threshold', 'WARNING');
    await modal.getByLabel('Suppression Window (minutes)').fill('15');
    await rules.toggle(modal, 'Quiet Hours');
    await modal.getByLabel('Start').fill('22:00');
    await modal.getByLabel('Start').press('Enter');
    await modal.getByLabel('End').fill('06:00');
    await modal.getByLabel('End').press('Enter');
    await rules.select(modal, 'Quiet Days', 'Monday');
    await rules.toggle(modal, 'Enable Fallback');
    await modal.getByLabel('Delay (minutes)').fill('5');
    await rules.select(modal, 'Fallback Type', 'User');
    await modal.getByLabel('Fallback Recipient').fill(process.env.E2E_ADMIN_USERNAME!);
    await modal.getByRole('button', { name: 'Create Rule' }).click();
    await expect(modal).toBeHidden();
    const row = await rules.locateRow(name);
    await expect(row).toContainText('EMAIL');
    await expect(row).toContainText('Suppress 15 min');
    await expect(row).toContainText('Fallback USER');
    const persisted = (await (await request.get('/api/notification-rules', { headers: headers(admin) })).json())
      .find((item: { name: string }) => item.name === name);
    expect(persisted).toMatchObject({ channel: 'EMAIL', recipientType: 'ROLE', recipientValue: 'LOCAL_MVP_ADMIN', templateCode: 'TRIP_DELAY', severityThreshold: 'WARNING',
      quietHoursEnabled: true, quietDays: ['MONDAY'], suppressionWindowMinutes: 15,
      escalationEnabled: true, escalationDelayMinutes: 5, escalationRecipientType: 'USER' });
    await deleteRule(request, admin, persisted.id);
  });

  test('E2E-NOT-013: Quiet-hours EMAIL remains PENDING while IN_APP is immediate', async ({ request }, testInfo) => {
    const admin = await adminLogin(request);
    const actor = await provisionUser(request, admin, unique('not013', testInfo), ['NOTIFICATION_VIEW']);
    const now = await backendNow(request, admin);
    const start = new Date(now.getTime() - 60_000);
    const end = new Date(now.getTime() + 5 * 60_000);
    const email = await createRule(request, admin, { name: unique('E2E QUIET EMAIL', testInfo), channel: 'EMAIL',
      recipientType: 'EMAIL_ADDRESS', recipientValue: `e2e-terminal-${unique('quiet', testInfo).toLowerCase()}@example.test`,
      quietHoursEnabled: true, quietStartTime: time(start), quietEndTime: time(end), quietDays: [dayName(start)],
      escalationEnabled: true, escalationDelayMinutes: 0, escalationRecipientType: 'USER', escalationRecipientValue: actor.username });
    const inApp = await createRule(request, admin, { name: unique('E2E QUIET APP', testInfo), channel: 'IN_APP',
      recipientType: 'USER', recipientValue: actor.username });
    const event = await triggerDelay(request, admin, unique('quiet-event', testInfo));
    await expect.poll(async () => (await deliveries(request, admin)).filter((item) => item.eventId === event.id && [email.id, inApp.id].includes(item.ruleId ?? '')).length).toBe(2);
    const owned = (await deliveries(request, admin)).filter((item) => item.eventId === event.id && [email.id, inApp.id].includes(item.ruleId ?? ''));
    const queued = owned.find((item) => item.ruleId === email.id)!;
    expect(queued.nextDeliveryAt).toBeTruthy();
    expect(queued.attemptCount).toBe(0);
    await expect.poll(async () => (await notifications(request, actor.tokens)).some((item) => item.eventId === event.id && item.channel === 'IN_APP' && item.status === 'SENT')).toBe(true);
    await deleteRule(request, admin, email.id);
    await deleteRule(request, admin, inApp.id);
  });

  test('E2E-NOT-014: Suppression prevents equivalent notification and is audited', async ({ request }, testInfo) => {
    const admin = await adminLogin(request);
    const actor = await provisionUser(request, admin, unique('not014', testInfo), ['NOTIFICATION_VIEW']);
    const rule = await createRule(request, admin, { name: unique('E2E SUPPRESS', testInfo), channel: 'IN_APP',
      recipientType: 'USER', recipientValue: actor.username, suppressionWindowMinutes: 15 });
    const tripId = await createOperationalTrip(request, admin, unique('TRIP-NOT014', testInfo));
    const first = await triggerDelay(request, admin, unique('suppress-one', testInfo), tripId);
    await expect.poll(async () => (await notifications(request, actor.tokens)).filter((item) => item.eventId === first.id).length).toBe(1);
    const second = await triggerDelay(request, admin, unique('suppress-two', testInfo), tripId);
    await expect.poll(async () => {
      const response = await request.get(`/api/notification-rule-executions?ruleId=${rule.id}`, { headers: headers(admin) });
      return ((await response.json()) as Array<{ eventId: string }>).filter((item) => [first.id, second.id].includes(item.eventId)).length;
    }).toBe(2);
    expect((await notifications(request, actor.tokens)).filter((item) => [first.id, second.id].includes(item.eventId))).toHaveLength(1);
    const executions = await (await request.get(`/api/notification-rule-executions?ruleId=${rule.id}`, { headers: headers(admin) })).json();
    const ownedExecutions = executions.filter((item: { eventId: string }) => [first.id, second.id].includes(item.eventId));
    expect(ownedExecutions.map((item: { outcome: string }) => item.outcome)).toEqual(expect.arrayContaining(['ACCEPTED', 'SUPPRESSED']));
    const suppressed = ownedExecutions.find((item: { outcome: string }) => item.outcome === 'SUPPRESSED');
    expect(suppressed.controllingNotificationId).toBeTruthy();
    await deleteRule(request, admin, rule.id);
  });
});
