import React, { useEffect } from 'react';
import { Form, Input, Modal, Select, Switch, message } from 'antd';
import type {
  CreateNotificationRuleRequest,
  NotificationChannel,
  NotificationRule,
  NotificationSeverity,
  RecipientType,
  UpdateNotificationRuleRequest,
} from './types';
import { useCreateNotificationRule, useUpdateNotificationRule } from './useNotificationRules';

interface NotificationRuleModalProps {
  open: boolean;
  onClose: () => void;
  ruleToEdit?: NotificationRule | null;
}

const EVENT_TYPE_OPTIONS = [
  { value: 'TRIP_DELAY_RECORDED', label: 'Trip Delay Recorded (TRIP_DELAY_RECORDED)' },
  { value: 'TRIP_INCIDENT_RECORDED', label: 'Trip Incident Recorded (TRIP_INCIDENT_RECORDED)' },
  { value: 'VEHICLE_MAINTENANCE_SCHEDULED', label: 'Vehicle Maintenance Scheduled (VEHICLE_MAINTENANCE_SCHEDULED)' },
  { value: 'VEHICLE_MAINTENANCE_BLOCKED', label: 'Vehicle Maintenance Blocked (VEHICLE_MAINTENANCE_BLOCKED)' },
  { value: 'DRIVER_EXCEPTION_CREATED', label: 'Driver Exception / Leave Created (DRIVER_EXCEPTION_CREATED)' },
  { value: 'DRIVER_MEDICAL_FITNESS_EXPIRED', label: 'Driver Medical Fitness Expired (DRIVER_MEDICAL_FITNESS_EXPIRED)' },
  { value: 'DRIVER_MEDICALLY_UNFIT', label: 'Driver Medically Unfit (DRIVER_MEDICALLY_UNFIT)' },
  { value: 'DRIVER_DRUG_TEST_FAILED', label: 'Driver Drug Test Failed (DRIVER_DRUG_TEST_FAILED)' },
  { value: 'DRIVER_LICENSE_EXPIRING', label: 'Driver License Expiring (DRIVER_LICENSE_EXPIRING)' },
  { value: 'FUEL_LIMIT_EXCEEDED', label: 'Fuel Issue Limit Exceeded (FUEL_LIMIT_EXCEEDED)' },
];

export const NotificationRuleModal: React.FC<NotificationRuleModalProps> = ({
  open,
  onClose,
  ruleToEdit,
}) => {
  const [form] = Form.useForm();
  const createMutation = useCreateNotificationRule();
  const updateMutation = useUpdateNotificationRule(ruleToEdit?.id ?? '');

  const isEditing = Boolean(ruleToEdit);
  const isLoading = createMutation.isPending || updateMutation.isPending;

  useEffect(() => {
    if (open) {
      if (ruleToEdit) {
        form.setFieldsValue({
          name: ruleToEdit.name,
          description: ruleToEdit.description,
          eventType: ruleToEdit.eventType,
          channel: ruleToEdit.channel,
          recipientType: ruleToEdit.recipientType,
          recipientValue: ruleToEdit.recipientValue,
          severityThreshold: ruleToEdit.severityThreshold,
          enabled: ruleToEdit.enabled,
        });
      } else {
        form.resetFields();
        form.setFieldsValue({
          eventType: 'TRIP_DELAY_RECORDED',
          channel: 'IN_APP',
          recipientType: 'ROLE',
          recipientValue: 'DISPATCHER',
          severityThreshold: 'INFO',
          enabled: true,
        });
      }
    }
  }, [open, ruleToEdit, form]);

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields();
      if (isEditing && ruleToEdit) {
        const payload: UpdateNotificationRuleRequest = {
          name: values.name,
          description: values.description,
          eventType: values.eventType,
          channel: values.channel as NotificationChannel,
          recipientType: values.recipientType as RecipientType,
          recipientValue: values.recipientValue,
          severityThreshold: values.severityThreshold as NotificationSeverity,
        };
        await updateMutation.mutateAsync(payload);
        message.success('Notification rule updated successfully');
      } else {
        const payload: CreateNotificationRuleRequest = {
          name: values.name,
          description: values.description,
          eventType: values.eventType,
          channel: values.channel as NotificationChannel,
          recipientType: values.recipientType as RecipientType,
          recipientValue: values.recipientValue,
          severityThreshold: values.severityThreshold as NotificationSeverity,
          enabled: values.enabled ?? true,
        };
        await createMutation.mutateAsync(payload);
        message.success('Notification rule created successfully');
      }
      onClose();
    } catch (err: unknown) {
      const apiError = err as { response?: { data?: { message?: string } } };
      const errorMessage = apiError.response?.data?.message ?? 'Failed to save notification rule';
      message.error(errorMessage);
    }
  };

  return (
    <Modal
      title={isEditing ? 'Edit Notification Rule' : 'Create Notification Rule'}
      open={open}
      onCancel={onClose}
      onOk={handleSubmit}
      confirmLoading={isLoading}
      okText={isEditing ? 'Update Rule' : 'Create Rule'}
      destroyOnClose
      width={600}
    >
      <Form form={form} layout="vertical" initialValues={{ channel: 'IN_APP', severityThreshold: 'INFO', enabled: true }}>
        <Form.Item
          name="name"
          label="Rule Name"
          rules={[{ required: true, message: 'Rule name is required' }, { max: 100, message: 'Max 100 characters' }]}
        >
          <Input placeholder="e.g., Critical Trip Delay Alert" aria-label="Rule Name" />
        </Form.Item>

        <Form.Item name="description" label="Description">
          <Input.TextArea rows={2} placeholder="Optional notes on when this alert triggers" aria-label="Description" />
        </Form.Item>

        <Form.Item
          name="eventType"
          label="Event Type"
          rules={[{ required: true, message: 'Event type is required' }]}
        >
          <Select
            showSearch
            placeholder="Select or enter event type"
            options={EVENT_TYPE_OPTIONS}
            aria-label="Event Type"
          />
        </Form.Item>

          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 16 }}>
            <Form.Item
              name="severityThreshold"
              label="Severity Threshold"
              rules={[{ required: true, message: 'Severity threshold is required' }]}
            >
              <Select
                options={[
                  { value: 'INFO', label: 'INFO (All Events)' },
                  { value: 'WARNING', label: 'WARNING & CRITICAL only' },
                  { value: 'CRITICAL', label: 'CRITICAL only' },
                ]}
                aria-label="Severity Threshold"
              />
            </Form.Item>
          </div>

        <div style={{ display: 'grid', gridTemplateColumns: '1fr 2fr', gap: 16 }}>
          <Form.Item
            name="recipientType"
            label="Recipient Type"
            rules={[{ required: true, message: 'Recipient type is required' }]}
          >
            <Select
              options={[
                { value: 'ROLE', label: 'Role (e.g. DISPATCHER)' },
                { value: 'USER', label: 'Specific User (Username)' },
                { value: 'EMAIL_ADDRESS', label: 'Email Address' },
              ]}
              aria-label="Recipient Type"
            />
          </Form.Item>

          <Form.Item
            name="recipientValue"
            label="Recipient Value"
            rules={[
              { required: true, message: 'Recipient value is required' },
              ({ getFieldValue }) => ({
                validator(_, value) {
                  if (getFieldValue('recipientType') === 'EMAIL_ADDRESS' && value) {
                    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
                    if (!emailRegex.test(value)) {
                      return Promise.reject(new Error('Please enter a valid email address'));
                    }
                  }
                  return Promise.resolve();
                },
              }),
            ]}
          >
            <Input placeholder="e.g. DISPATCHER, admin, or ops@logistics.com" aria-label="Recipient Value" />
          </Form.Item>
        </div>

        {!isEditing && (
          <Form.Item name="enabled" label="Enable Rule Immediately" valuePropName="checked">
            <Switch aria-label="Enable Rule" />
          </Form.Item>
        )}
      </Form>
    </Modal>
  );
};
