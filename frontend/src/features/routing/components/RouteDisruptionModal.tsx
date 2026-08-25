import { Alert, Form, Input, Modal, Select } from 'antd';
import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { routeApi } from '../api/routeApi';
import { useCreateRouteDisruption } from '../hooks/useRouteHistoryAndDisruptions';
import type { DisruptionSeverity, RouteDisruptionType } from '../types/route';

interface RouteDisruptionModalProps {
  routeId: string;
  open: boolean;
  onClose: () => void;
}

export function RouteDisruptionModal({ routeId, open, onClose }: RouteDisruptionModalProps) {
  const [form] = Form.useForm();
  const [formError, setFormError] = useState<string>();
  const createMutation = useCreateRouteDisruption(routeId);
  const { data: allRoutes } = useQuery({
    queryKey: ['routes'],
    queryFn: () => routeApi.list(),
  });

  const availableDetours = (allRoutes ?? []).filter((r) => r.id !== routeId && r.active);

  const handleSubmit = async () => {
    try {
      setFormError(undefined);
      const values = await form.validateFields();
      await createMutation.mutateAsync({
        disruptionType: values.disruptionType as RouteDisruptionType,
        severity: values.severity as DisruptionSeverity,
        description: values.description,
        effectiveFrom: new Date(values.effectiveFrom).toISOString(),
        effectiveUntil: values.effectiveUntil ? new Date(values.effectiveUntil).toISOString() : null,
        detourRouteId: values.detourRouteId || null,
      });
      form.resetFields();
      onClose();
    } catch (err: unknown) {
      const apiErr = err as { errorFields?: unknown; response?: { data?: { message?: string } }; message?: string };
      if (apiErr?.errorFields) {
        return;
      }
      setFormError(apiErr?.response?.data?.message ?? apiErr?.message ?? 'Failed to record disruption');
    }
  };

  return (
    <Modal
      title="Report Route Disruption"
      open={open}
      onCancel={onClose}
      onOk={handleSubmit}
      confirmLoading={createMutation.isPending}
      okText="Record Disruption"
      destroyOnHidden
    >
      {formError && (
        <Alert type="error" showIcon message={formError} style={{ marginBottom: 16 }} />
      )}
      <Form form={form} layout="vertical" initialValues={{
        severity: 'MEDIUM',
        disruptionType: 'ROAD_CLOSURE',
        effectiveFrom: new Date().toISOString().slice(0, 16),
      }}>
        <Form.Item
          name="disruptionType"
          label="Disruption Type"
          rules={[{ required: true, message: 'Please select a disruption type' }]}
        >
          <Select options={[
            { label: 'Road Closure', value: 'ROAD_CLOSURE' },
            { label: 'Accident', value: 'ACCIDENT' },
            { label: 'Severe Weather', value: 'WEATHER' },
            { label: 'Restriction / Hazard', value: 'RESTRICTION' },
          ]} />
        </Form.Item>

        <Form.Item
          name="severity"
          label="Severity"
          rules={[{ required: true, message: 'Please select severity level' }]}
        >
          <Select options={[
            { label: 'Low', value: 'LOW' },
            { label: 'Medium', value: 'MEDIUM' },
            { label: 'High', value: 'HIGH' },
            { label: 'Critical', value: 'CRITICAL' },
          ]} />
        </Form.Item>

        <Form.Item
          name="description"
          label="Description / Reason"
          rules={[{ required: true, message: 'Please provide disruption details' }]}
        >
          <Input.TextArea rows={3} placeholder="Describe the cause, blockage, or affected roadway segment..." />
        </Form.Item>

        <Form.Item
          name="effectiveFrom"
          label="Effective From"
          rules={[{ required: true, message: 'Effective from timestamp is required' }]}
        >
          <Input type="datetime-local" />
        </Form.Item>

        <Form.Item
          name="effectiveUntil"
          label="Estimated Effective Until (Optional)"
        >
          <Input type="datetime-local" />
        </Form.Item>

        <Form.Item
          name="detourRouteId"
          label="Detour Route (Optional)"
        >
          <Select
            allowClear
            placeholder="Select detour route if available"
            options={availableDetours.map((r) => ({
              label: `${r.name} (${r.code})`,
              value: r.id,
            }))}
          />
        </Form.Item>
      </Form>
    </Modal>
  );
}
