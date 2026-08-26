import React, { useEffect, useRef } from 'react';
import axios from 'axios';
import dayjs, { type Dayjs } from 'dayjs';
import { Alert, App, Card, Divider, Form, Input, InputNumber, Modal, Select, Space, Switch, TimePicker, Typography } from 'antd';
import type { CreateNotificationRuleRequest, DayOfWeek, NotificationChannel, NotificationRule, NotificationSeverity, RecipientType, UpdateNotificationRuleRequest } from './types';
import { useCreateNotificationRule, useNotificationEventCatalogue, useNotificationTemplates, useUpdateNotificationRule } from './useNotificationRules';

interface NotificationRuleModalProps { open: boolean; onClose: () => void; ruleToEdit?: NotificationRule | null }
export interface RuleFormValues {
  name: string; description?: string; eventType: string; channel: NotificationChannel; templateCode: string;
  severityThreshold: NotificationSeverity; recipientType: RecipientType; recipientValue: string;
  quietHoursEnabled: boolean; quietStartTime?: Dayjs; quietEndTime?: Dayjs; quietDays?: DayOfWeek[];
  suppressionWindowMinutes: number; escalationEnabled: boolean; escalationDelayMinutes?: number;
  escalationRecipientType?: 'USER' | 'ROLE'; escalationRecipientValue?: string; enabled: boolean;
}
interface ApiErrorBody { code?: string; message?: string; fieldErrors?: Array<{ field: string; message: string }> | Record<string, string> }

const DAYS: Array<{ value: DayOfWeek; label: string }> = [
  ['MONDAY', 'Monday'], ['TUESDAY', 'Tuesday'], ['WEDNESDAY', 'Wednesday'], ['THURSDAY', 'Thursday'],
  ['FRIDAY', 'Friday'], ['SATURDAY', 'Saturday'], ['SUNDAY', 'Sunday'],
].map(([value, label]) => ({ value: value as DayOfWeek, label }));
const friendlyEvent = (value: string) => value.toLowerCase().split('_').map((part) => `${part[0].toUpperCase()}${part.slice(1)}`).join(' ');

export const recipientTypesForChannel = (channel: NotificationChannel): RecipientType[] =>
  channel === 'EMAIL' ? ['USER', 'ROLE', 'EMAIL_ADDRESS'] : ['USER', 'ROLE'];

export const buildNotificationRulePayload = (values: RuleFormValues): CreateNotificationRuleRequest => {
  const email = values.channel === 'EMAIL';
  return {
    name: values.name.trim(), description: values.description?.trim() || undefined, eventType: values.eventType,
    channel: values.channel, templateCode: values.templateCode, severityThreshold: values.severityThreshold,
    recipientType: values.recipientType, recipientValue: values.recipientValue.trim(),
    suppressionWindowMinutes: values.suppressionWindowMinutes, quietHoursEnabled: email && values.quietHoursEnabled,
    quietStartTime: email && values.quietHoursEnabled ? values.quietStartTime?.format('HH:mm:ss') : undefined,
    quietEndTime: email && values.quietHoursEnabled ? values.quietEndTime?.format('HH:mm:ss') : undefined,
    quietDays: email && values.quietHoursEnabled ? values.quietDays : [], escalationEnabled: email && values.escalationEnabled,
    escalationDelayMinutes: email && values.escalationEnabled ? values.escalationDelayMinutes : undefined,
    escalationRecipientType: email && values.escalationEnabled ? values.escalationRecipientType : undefined,
    escalationRecipientValue: email && values.escalationEnabled ? values.escalationRecipientValue?.trim() : undefined,
    enabled: values.enabled,
  };
};

export const NotificationRuleModal: React.FC<NotificationRuleModalProps> = ({ open, onClose, ruleToEdit }) => {
  const [form] = Form.useForm<RuleFormValues>();
  const initializedForOpen = useRef(false);
  const { message } = App.useApp();
  const eventType = Form.useWatch('eventType', form);
  const channel = Form.useWatch('channel', form);
  const recipientType = Form.useWatch('recipientType', form);
  const quietEnabled = Form.useWatch('quietHoursEnabled', form);
  const escalationEnabled = Form.useWatch('escalationEnabled', form);
  const templateCode = Form.useWatch('templateCode', form);
  const catalogue = useNotificationEventCatalogue();
  const templates = useNotificationTemplates(eventType, channel);
  const createMutation = useCreateNotificationRule();
  const updateMutation = useUpdateNotificationRule(ruleToEdit?.id ?? '');
  const selectedEvent = catalogue.data?.find((item) => item.eventType === eventType);
  const selectedTemplate = templates.data?.find((item) => item.code === templateCode);
  const isEditing = Boolean(ruleToEdit);

  useEffect(() => {
    if (!open) {
      initializedForOpen.current = false;
      return;
    }
    if (initializedForOpen.current) return;
    if (ruleToEdit) {
      form.setFieldsValue({ ...ruleToEdit,
        quietStartTime: ruleToEdit.quietStartTime ? dayjs(`2000-01-01T${ruleToEdit.quietStartTime}`) : undefined,
        quietEndTime: ruleToEdit.quietEndTime ? dayjs(`2000-01-01T${ruleToEdit.quietEndTime}`) : undefined });
    } else {
      const defaultEvent = catalogue.data?.[0];
      if (!defaultEvent) return;
      form.resetFields();
      form.setFieldsValue({ eventType: defaultEvent.eventType, channel: 'IN_APP', recipientType: 'ROLE',
        severityThreshold: defaultEvent.defaultSeverity,
        suppressionWindowMinutes: 0, quietHoursEnabled: false, escalationEnabled: false, enabled: true });
    }
    initializedForOpen.current = true;
  }, [catalogue.data, form, open, ruleToEdit]);

  useEffect(() => {
    if (!eventType || !channel) return;
    const definition = catalogue.data?.find((item) => item.eventType === eventType);
    if (definition && !definition.supportedChannels.includes(channel)) form.setFieldValue('channel', undefined);
    if (templates.data && !templates.data.some((template) => template.code === form.getFieldValue('templateCode'))) {
      form.setFieldValue('templateCode', templates.data[0]?.code);
    }
  }, [catalogue.data, channel, eventType, form, templates.data]);

  useEffect(() => {
    if (channel !== 'IN_APP') return;
    if (recipientType === 'EMAIL_ADDRESS') form.setFieldValue('recipientType', undefined);
    form.setFieldsValue({ quietHoursEnabled: false, quietStartTime: undefined, quietEndTime: undefined, quietDays: [],
      escalationEnabled: false, escalationDelayMinutes: undefined, escalationRecipientType: undefined, escalationRecipientValue: undefined });
  }, [channel, form, recipientType]);

  const mapApiError = (error: unknown) => {
    if (!axios.isAxiosError<ApiErrorBody>(error)) { void message.error('Failed to save notification rule'); return; }
    const body = error.response?.data;
    const entries = Array.isArray(body?.fieldErrors)
      ? body.fieldErrors.map(({ field, message }) => [field, message] as const)
      : Object.entries(body?.fieldErrors ?? {});
    if (entries.length) form.setFields(entries.map(([name, message]) => ({ name: name as keyof RuleFormValues, errors: [message] })));
    const codeField: Partial<Record<string, keyof RuleFormValues>> = {
      NOTIFICATION_EVENT_UNSUPPORTED: 'eventType', NOTIFICATION_TEMPLATE_INCOMPATIBLE: 'templateCode',
      NOTIFICATION_RECIPIENT_INVALID: 'recipientValue', NOTIFICATION_RECIPIENT_NOT_FOUND: 'recipientValue',
      NOTIFICATION_CHANNEL_RECIPIENT_INCOMPATIBLE: 'recipientType', NOTIFICATION_POLICY_INVALID: 'escalationEnabled',
    };
    if (!entries.length && body?.code && codeField[body.code]) form.setFields([{ name: codeField[body.code], errors: [body.message ?? body.code] }]);
    void message.error(body?.message ?? 'Failed to save notification rule');
  };

  const handleSubmit = async () => {
    try {
      const request = buildNotificationRulePayload(await form.validateFields());
      if (isEditing) await updateMutation.mutateAsync(request as UpdateNotificationRuleRequest);
      else await createMutation.mutateAsync(request);
      void message.success(isEditing ? 'Notification rule updated' : 'Notification rule created');
      onClose();
    } catch (error) {
      if (!(typeof error === 'object' && error !== null && 'errorFields' in error)) mapApiError(error);
    }
  };

  return <Modal title={isEditing ? 'Edit Notification Rule' : 'Create Notification Rule'} open={open} onCancel={onClose}
    onOk={handleSubmit} confirmLoading={createMutation.isPending || updateMutation.isPending}
    okText={isEditing ? 'Update Rule' : 'Create Rule'} destroyOnHidden width={820}>
    <Form form={form} layout="vertical" preserve>
      <Divider orientation="left">Basic</Divider>
      <Form.Item name="name" label="Rule Name" rules={[{ required: true }, { max: 128 }]}><Input /></Form.Item>
      <Form.Item name="description" label="Description" rules={[{ max: 255 }]}><Input.TextArea rows={2} /></Form.Item>
      <Space align="start" wrap style={{ width: '100%' }}>
        <Form.Item name="eventType" label="Event" rules={[{ required: true }]} style={{ minWidth: 300 }}>
          <Select showSearch loading={catalogue.isLoading} optionFilterProp="label" options={(catalogue.data ?? []).map((item) => ({
            value: item.eventType, label: `${friendlyEvent(item.eventType)} (${item.owningModule})` }))} />
        </Form.Item>
        <Form.Item name="channel" label="Channel" rules={[{ required: true }]} style={{ minWidth: 160 }}>
          <Select options={(['IN_APP', 'EMAIL'] as NotificationChannel[]).filter((value) => !selectedEvent || selectedEvent.supportedChannels.includes(value))
            .map((value) => ({ value, label: value === 'IN_APP' ? 'In-app' : 'Email' }))} />
        </Form.Item>
        <Form.Item name="severityThreshold" label="Severity threshold" tooltip="Matches events at or above this severity." rules={[{ required: true }]} style={{ minWidth: 180 }}>
          <Select options={['INFO', 'WARNING', 'CRITICAL'].map((value) => ({ value, label: value }))} />
        </Form.Item>
      </Space>
      <Divider orientation="left">System Template</Divider>
      <Form.Item name="templateCode" label="Template" rules={[{ required: true }]}>
        <Select loading={templates.isLoading} disabled={!eventType || !channel} options={(templates.data ?? []).map((item) => ({
          value: item.code, label: `${item.name} · ${item.code} · v${item.version}` }))} />
      </Form.Item>
      {selectedTemplate && <Card size="small" title="Read-only template preview">
        {selectedTemplate.subject && <Typography.Paragraph><Typography.Text strong>Subject: </Typography.Text>{selectedTemplate.subject}</Typography.Paragraph>}
        <Typography.Paragraph style={{ whiteSpace: 'pre-wrap', marginBottom: 8 }}>{selectedTemplate.body}</Typography.Paragraph>
        <Typography.Text type="secondary">Placeholders are populated from system event variables.</Typography.Text>
      </Card>}
      <Divider orientation="left">Recipient</Divider>
      <Space align="start" wrap>
        <Form.Item name="recipientType" label="Recipient Type" rules={[{ required: true }]} style={{ minWidth: 200 }}>
          <Select options={recipientTypesForChannel(channel ?? 'IN_APP').map((value) => ({
            value, label: value === 'EMAIL_ADDRESS' ? 'Email address' : value === 'USER' ? 'User (username)' : 'Role' }))} />
        </Form.Item>
        <Form.Item name="recipientValue" label={recipientType === 'EMAIL_ADDRESS' ? 'Email Address' : recipientType === 'USER' ? 'Username' : 'Role Name'}
          rules={[{ required: true }, ...(recipientType === 'EMAIL_ADDRESS' ? [{ type: 'email' as const }] : [])]} style={{ minWidth: 360 }}>
          <Input type={recipientType === 'EMAIL_ADDRESS' ? 'email' : 'text'} placeholder={recipientType === 'ROLE' ? 'Enter canonical role name' : undefined} />
        </Form.Item>
      </Space>
      <Divider orientation="left">Delivery Policy</Divider>
      <Form.Item name="suppressionWindowMinutes" label="Suppression Window (minutes)" tooltip="0 disables suppression; event-id duplicate protection remains separate."
        rules={[{ required: true }, { type: 'number', min: 0, max: 1440 }]}><InputNumber min={0} max={1440} style={{ width: 180 }} /></Form.Item>
      {channel === 'EMAIL' && <>
        <Form.Item name="quietHoursEnabled" label="Quiet Hours" valuePropName="checked"><Switch /></Form.Item>
        {quietEnabled && <Space align="start" wrap>
          <Form.Item name="quietStartTime" label="Start" rules={[{ required: true }]}><TimePicker format="HH:mm" /></Form.Item>
          <Form.Item name="quietEndTime" label="End" dependencies={['quietStartTime']} rules={[{ required: true }, ({ getFieldValue }) => ({
            validator: (_, value?: Dayjs) => value && getFieldValue('quietStartTime')?.format('HH:mm') === value.format('HH:mm')
              ? Promise.reject(new Error('Start and end must be different')) : Promise.resolve() })]}><TimePicker format="HH:mm" /></Form.Item>
          <Form.Item name="quietDays" label="Quiet Days" rules={[{ required: true, type: 'array', min: 1 }]} style={{ minWidth: 340 }}>
            <Select mode="multiple" options={DAYS} maxTagCount="responsive" />
          </Form.Item>
        </Space>}
      </>}
      {channel === 'EMAIL' && <>
        <Divider orientation="left">Failure Escalation</Divider>
        <Alert type="info" showIcon message="Email rules require an active in-app fallback for critical delivery failures." style={{ marginBottom: 16 }} />
        <Form.Item name="escalationEnabled" label="Enable Fallback" valuePropName="checked"
          rules={[{ validator: (_, value) => value ? Promise.resolve() : Promise.reject(new Error('Email rules require fallback escalation')) }]}><Switch /></Form.Item>
        {escalationEnabled && <Space align="start" wrap>
          <Form.Item name="escalationDelayMinutes" label="Delay (minutes)" rules={[{ required: true }, { type: 'number', min: 0, max: 60 }]}><InputNumber min={0} max={60} /></Form.Item>
          <Form.Item name="escalationRecipientType" label="Fallback Type" rules={[{ required: true }]} style={{ minWidth: 170 }}>
            <Select options={[{ value: 'USER', label: 'User' }, { value: 'ROLE', label: 'Role' }]} />
          </Form.Item>
          <Form.Item name="escalationRecipientValue" label="Fallback Recipient" rules={[{ required: true }]} style={{ minWidth: 300 }}><Input placeholder="Username or canonical role name" /></Form.Item>
        </Space>}
      </>}
      {!isEditing && <Form.Item name="enabled" label="Enable Rule Immediately" valuePropName="checked"><Switch /></Form.Item>}
    </Form>
  </Modal>;
};
