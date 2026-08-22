import dayjs from 'dayjs';
import { describe, expect, it } from 'vitest';
import { buildNotificationRulePayload, recipientTypesForChannel, type RuleFormValues } from './NotificationRuleModal';

const base = (channel: 'IN_APP' | 'EMAIL'): RuleFormValues => ({
  name: '  Trip delay  ', description: '  Operational alert  ', eventType: 'TRIP_DELAY_RECORDED', channel,
  templateCode: 'TRIP_DELAY', severityThreshold: 'WARNING', recipientType: 'ROLE', recipientValue: ' OPERATIONS ',
  quietHoursEnabled: false, suppressionWindowMinutes: 0, escalationEnabled: false, enabled: true,
});

describe('notification rule form contract', () => {
  it('allows USER and ROLE but not EMAIL_ADDRESS for IN_APP', () => {
    expect(recipientTypesForChannel('IN_APP')).toEqual(['USER', 'ROLE']);
  });

  it('allows EMAIL_ADDRESS only for EMAIL', () => {
    expect(recipientTypesForChannel('EMAIL')).toEqual(['USER', 'ROLE', 'EMAIL_ADDRESS']);
  });

  it('trims administrator-entered names and canonical recipient values', () => {
    const payload = buildNotificationRulePayload(base('IN_APP'));
    expect(payload.name).toBe('Trip delay');
    expect(payload.recipientValue).toBe('OPERATIONS');
  });

  it('preserves suppression zero as explicitly disabled', () => {
    expect(buildNotificationRulePayload(base('IN_APP')).suppressionWindowMinutes).toBe(0);
  });

  it('removes stale quiet-hours and escalation fields from IN_APP payloads', () => {
    const values = { ...base('IN_APP'), quietHoursEnabled: true, quietStartTime: dayjs('2026-01-01T22:00:00'),
      quietEndTime: dayjs('2026-01-01T06:00:00'), quietDays: ['MONDAY'] as RuleFormValues['quietDays'], escalationEnabled: true,
      escalationDelayMinutes: 10, escalationRecipientType: 'ROLE' as const, escalationRecipientValue: 'OPERATIONS' };
    const payload = buildNotificationRulePayload(values);
    expect(payload).toMatchObject({ quietHoursEnabled: false, quietDays: [], escalationEnabled: false });
    expect(payload.quietStartTime).toBeUndefined();
    expect(payload.escalationRecipientValue).toBeUndefined();
  });

  it('supports an overnight EMAIL quiet-hours range without reordering it', () => {
    const payload = buildNotificationRulePayload({ ...base('EMAIL'), quietHoursEnabled: true,
      quietStartTime: dayjs('2026-01-01T22:00:00'), quietEndTime: dayjs('2026-01-01T06:00:00'), quietDays: ['MONDAY'] });
    expect(payload).toMatchObject({ quietStartTime: '22:00:00', quietEndTime: '06:00:00', quietDays: ['MONDAY'] });
  });

  it.each([0, 60])('preserves valid escalation boundary delay %s', (delay) => {
    const payload = buildNotificationRulePayload({ ...base('EMAIL'), escalationEnabled: true,
      escalationDelayMinutes: delay, escalationRecipientType: 'USER', escalationRecipientValue: ' supervisor ' });
    expect(payload).toMatchObject({ escalationDelayMinutes: delay, escalationRecipientType: 'USER', escalationRecipientValue: 'supervisor' });
  });

  it('omits disabled EMAIL quiet and escalation details', () => {
    const payload = buildNotificationRulePayload(base('EMAIL'));
    expect(payload.quietStartTime).toBeUndefined();
    expect(payload.escalationDelayMinutes).toBeUndefined();
  });
});
