import { zodResolver } from '@hookform/resolvers/zod';
import axios from 'axios';
import { Alert, Form, Input, Modal, Typography } from 'antd';
import { Controller, useForm } from 'react-hook-form';
import { acknowledgementSchema, type AcknowledgementValues } from './validation';
import type { AcknowledgementCommand, GpsExceptionEpisode } from './types';

interface Props {
  episode?: GpsExceptionEpisode;
  open: boolean;
  pending: boolean;
  uncertain?: AcknowledgementCommand;
  error?: unknown;
  onCancel: () => void;
  onSubmit: (command: AcknowledgementCommand) => Promise<void>;
}

export function AcknowledgementModal({ episode, open, pending, uncertain, error, onCancel, onSubmit }: Props) {
  const form = useForm<AcknowledgementValues>({ resolver: zodResolver(acknowledgementSchema), defaultValues: { reason: '' } });
  const submit = form.handleSubmit(async ({ reason }) => {
    const normalized = reason.trim();
    const command = uncertain && uncertain.reason === normalized && uncertain.expectedVersion === episode?.version
      ? uncertain
      : { reason: normalized, expectedVersion: episode!.version, idempotencyKey: crypto.randomUUID() };
    await onSubmit(command);
  });
  const conflict = axios.isAxiosError(error) && error.response?.status === 409;
  return <Modal
    title="Acknowledge GPS exception"
    open={open}
    okText={uncertain ? 'Retry acknowledgement' : 'Acknowledge'}
    confirmLoading={pending}
    okButtonProps={{ disabled: pending }}
    onCancel={onCancel}
    afterOpenChange={(visible) => { if (visible) window.requestAnimationFrame(() => document.getElementById('gps-exception-acknowledgement-reason')?.focus()); }}
    afterClose={() => form.reset()}
    onOk={() => void submit()}
    destroyOnHidden
  >
    <Alert type="info" showIcon message="Review acknowledgement only" description="Acknowledgement records operator review. Recovery and resolution remain detector-controlled." />
    {uncertain && <Alert type="warning" showIcon message="Outcome unknown" description="Retrying this unchanged command uses the same protected idempotency key." />}
    {conflict && <Alert type="warning" showIcon message="Episode state changed" description="The latest episode state has been refreshed. Review it before starting another acknowledgement." />}
    <Form layout="vertical" onFinish={() => void submit()} style={{ marginTop: 16 }}>
      <Controller name="reason" control={form.control} render={({ field }) => <Form.Item label="Review reason" required validateStatus={form.formState.errors.reason ? 'error' : undefined} help={form.formState.errors.reason?.message}><Input.TextArea {...field} id="gps-exception-acknowledgement-reason" aria-label="Acknowledgement reason" maxLength={500} rows={4} /></Form.Item>} />
    </Form>
    <Typography.Text type="secondary">Reason text is stored as controlled audit evidence and is not placed in the browser URL or persistent storage.</Typography.Text>
  </Modal>;
}
