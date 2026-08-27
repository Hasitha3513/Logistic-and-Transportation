import { test, expect } from '@playwright/test';
import { NotificationRulesPageObject } from '../../pages/NotificationRulesPage';
import {
  adminLogin, authenticatePage, createRule, deleteRule, deliveries, headers, notifications, provisionUser,
  triggerDelay, unique,
} from '../../helpers/notificationTestApi';

test.describe('US-77 notification rule administration', () => {
  test('E2E-NOT-001: Authorized administrator opens Notification Rules', async ({ page, request }) => {
    const admin = await adminLogin(request);
    await authenticatePage(page, admin);
    const rules = new NotificationRulesPageObject(page);
    await rules.open();
    await expect(page.getByRole('button', { name: 'Create Notification Rule' })).toBeVisible();
    await expect(page.getByRole('tab', { name: 'Rules' })).toBeVisible();
    await expect(page.getByRole('tab', { name: 'Delivery Diagnostics' })).toBeVisible();
  });

  test('E2E-NOT-002: Create IN_APP rule from catalogue and template', async ({ page, request }, testInfo) => {
    const admin = await adminLogin(request);
    await authenticatePage(page, admin);
    const name = unique('E2E IN APP', testInfo);
    const rules = new NotificationRulesPageObject(page);
    await rules.open();
    await rules.createInApp(name, 'LOCAL_MVP_ADMIN');
    const persisted = (await (await request.get('/api/notification-rules', { headers: headers(admin) })).json())
      .find((rule: { name: string }) => rule.name === name);
    expect(persisted).toMatchObject({ eventType: 'TRIP_DELAY_RECORDED', channel: 'IN_APP', templateCode: 'TRIP_DELAY', recipientType: 'ROLE', recipientValue: 'LOCAL_MVP_ADMIN' });
    await deleteRule(request, admin, persisted.id);
  });

  test('E2E-NOT-003: Edit rule and persist all changed fields', async ({ page, request }, testInfo) => {
    const admin = await adminLogin(request);
    const original = unique('E2E EDIT', testInfo);
    const renamed = `${original} Updated`;
    const rule = await createRule(request, admin, { name: original, channel: 'IN_APP', recipientType: 'USER', recipientValue: process.env.E2E_ADMIN_USERNAME });
    await authenticatePage(page, admin);
    const rules = new NotificationRulesPageObject(page);
    await rules.open();
    const originalRow = await rules.locateRow(original);
    await originalRow.getByRole('button', { name: `Edit ${original}` }).click();
    const modal = page.getByRole('dialog', { name: 'Edit Notification Rule' });
    await modal.getByLabel('Rule Name').fill(renamed);
    await rules.select(modal, 'Severity threshold', 'CRITICAL');
    await modal.getByLabel('Suppression Window (minutes)').fill('12');
    await modal.getByRole('button', { name: 'Update Rule' }).click();
    await expect(modal).toBeHidden();
    await page.reload();
    await expect(page.getByRole('heading', { name: 'Notification Rules', level: 2 })).toBeVisible();
    const response = await request.get(`/api/notification-rules/${rule.id}`, { headers: headers(admin) });
    expect(await response.json()).toMatchObject({ name: renamed, severityThreshold: 'CRITICAL', suppressionWindowMinutes: 12, templateCode: 'TRIP_DELAY', recipientType: 'USER' });
    await deleteRule(request, admin, rule.id);
  });

  test('E2E-NOT-004: Enable and disable rule with disabled-event proof', async ({ page, request }, testInfo) => {
    const admin = await adminLogin(request);
    const actor = await provisionUser(request, admin, unique('not004', testInfo), ['NOTIFICATION_VIEW']);
    const name = unique('E2E TOGGLE', testInfo);
    const rule = await createRule(request, admin, { name, channel: 'IN_APP', recipientType: 'USER', recipientValue: actor.username, enabled: false });
    await authenticatePage(page, admin);
    const rules = new NotificationRulesPageObject(page);
    await rules.open();
    const toggle = (await rules.locateRow(name)).getByRole('switch', { name: `Toggle rule ${name}` });
    await toggle.click();
    await expect(toggle).toBeChecked();
    const enabledEvent = await triggerDelay(request, admin, unique('enabled', testInfo));
    await expect.poll(async () => (await notifications(request, actor.tokens)).filter((item) => item.eventId === enabledEvent.id).length).toBe(1);
    await toggle.click();
    await expect(toggle).not.toBeChecked();
    const sentinel = await createRule(request, admin, { name: unique('E2E TOGGLE SENTINEL', testInfo), channel: 'IN_APP',
      recipientType: 'USER', recipientValue: actor.username });
    const disabledEvent = await triggerDelay(request, admin, unique('disabled', testInfo));
    await expect.poll(async () => (await deliveries(request, admin))
      .some((item) => item.eventId === disabledEvent.id && item.ruleId === sentinel.id)).toBe(true);
    expect((await deliveries(request, admin)).some((item) => item.eventId === disabledEvent.id && item.ruleId === rule.id)).toBe(false);
    const persisted = await (await request.get(`/api/notification-rules/${rule.id}`, { headers: headers(admin) })).json();
    expect(persisted.enabled).toBe(false);
    await deleteRule(request, admin, sentinel.id);
    await deleteRule(request, admin, rule.id);
  });

  test('E2E-NOT-005: Unauthorized roles cannot view or mutate rules', async ({ page, request }, testInfo) => {
    const admin = await adminLogin(request);
    const viewer = await provisionUser(request, admin, unique('not005view', testInfo), ['NOTIFICATION_RULE_VIEW']);
    await authenticatePage(page, viewer.tokens);
    const rules = new NotificationRulesPageObject(page);
    await rules.open();
    await expect(page.getByRole('button', { name: 'Create Notification Rule' })).toHaveCount(0);
    await expect(page.getByRole('button', { name: /^Edit / })).toHaveCount(0);

    const unauthorized = await provisionUser(request, admin, unique('not005none', testInfo), []);
    const forbidden = await request.post('/api/notification-rules', { headers: headers(unauthorized.tokens), data: {
      name: 'Forbidden', eventType: 'TRIP_DELAY_RECORDED', channel: 'IN_APP', recipientType: 'USER',
      recipientValue: unauthorized.username, templateCode: 'TRIP_DELAY', enabled: true, severityThreshold: 'INFO',
    }});
    expect(forbidden.status()).toBe(403);
    const unauthenticated = await request.post('/api/notification-rules', { data: {} });
    expect(unauthenticated.status()).toBe(401);
  });
});
