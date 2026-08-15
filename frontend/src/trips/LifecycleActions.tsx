import { useState } from 'react';
import { CheckOutlined, CloseCircleOutlined, RocketOutlined, StopOutlined } from '@ant-design/icons';
import { zodResolver } from '@hookform/resolvers/zod';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import axios from 'axios';
import dayjs from 'dayjs';
import { Controller, useForm } from 'react-hook-form';
import { z } from 'zod';
import {
  Alert,
  App as AntApp,
  Badge,
  Button,
  Card,
  DatePicker,
  Descriptions,
  Flex,
  Form,
  Input,
  InputNumber,
  Modal,
  Space,
  Typography,
} from 'antd';
import { api } from '../api/client';
import { TripStatusTag } from '../components/status/StatusTags';
import type { Trip } from './types';

const { Text } = Typography;
const reasonSchema = z.object({ reason: z.string().trim().min(1, 'A reason is required').max(1000, 'Reason cannot exceed 1000 characters') });
const dispatchSchema = z.object({ remarks: z.string().max(1000, 'Remarks cannot exceed 1000 characters') });
const startSchema = z.object({ startOdometerKm: z.number({ error: 'Start odometer is required' }).min(0, 'Start odometer cannot be negative') });
const completeSchema = z.object({
  endOdometerKm: z.number({ error: 'End odometer is required' }).min(0, 'End odometer cannot be negative'),
  completionRemarks: z.string().max(1000, 'Remarks cannot exceed 1000 characters'),
});

type Action = 'submit' | 'approve' | 'reject' | 'dispatch' | 'start' | 'complete' | 'close' | 'cancel';
type HasPermission = (permission: string) => boolean;

interface LifecycleActionsProps {
  trip: Trip;
  hasPermission: HasPermission;
}

interface ApiErrorBody { message?: string; correlationId?: string }

function errorDescription(error: unknown) {
  if (axios.isAxiosError<ApiErrorBody>(error)) {
    const body = error.response?.data;
    if (body?.message) return `${body.message}${body.correlationId ? ` (${body.correlationId})` : ''}`;
  }
  return 'The lifecycle action could not be completed.';
}

export default function LifecycleActions({ trip, hasPermission }: LifecycleActionsProps) {
  const { message, notification } = AntApp.useApp();
  const queryClient = useQueryClient();
  const [action, setAction] = useState<Action>();
  const reasonForm = useForm<z.infer<typeof reasonSchema>>({ resolver: zodResolver(reasonSchema), defaultValues: { reason: '' } });
  const dispatchForm = useForm<z.infer<typeof dispatchSchema>>({ resolver: zodResolver(dispatchSchema), defaultValues: { remarks: '' } });
  const startForm = useForm<z.infer<typeof startSchema>>({ resolver: zodResolver(startSchema), defaultValues: { startOdometerKm: undefined } });
  const completeForm = useForm<z.infer<typeof completeSchema>>({ resolver: zodResolver(completeSchema), defaultValues: { endOdometerKm: undefined, completionRemarks: '' } });
  const mutation = useMutation({
    mutationFn: async ({ lifecycleAction, body }: { lifecycleAction: Action; body?: object }) =>
      (await api.post<Trip>(`/trips/${trip.id}/${lifecycleAction}`, body ?? {})).data,
    onSuccess: async (updated) => {
      queryClient.setQueryData(['trip', trip.id], updated);
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ['trip', trip.id, 'status-history'] }),
        queryClient.invalidateQueries({ queryKey: ['trips'] }),
      ]);
      void message.success(`Trip ${updated.status.replaceAll('_', ' ').toLowerCase()}`);
      setAction(undefined);
    },
    onError: (error, variables) => notification.error({
      message: `Trip ${variables.lifecycleAction} failed`,
      description: errorDescription(error),
      duration: 6,
    }),
  });

  const open = (nextAction: Action) => {
    mutation.reset();
    reasonForm.reset();
    dispatchForm.reset();
    startForm.reset();
    completeForm.reset();
    setAction(nextAction);
  };
  const close = () => !mutation.isPending && setAction(undefined);
  const execute = (lifecycleAction: Action, body?: object) => mutation.mutate({ lifecycleAction, body });
  const canCancel = ['DRAFT', 'SUBMITTED', 'REJECTED', 'APPROVED', 'ASSIGNED', 'DISPATCHED'].includes(trip.status);

  const buttons = [
    ...(['DRAFT', 'REJECTED'].includes(trip.status) && hasPermission('TRIP_SUBMIT') ? [<Button key="submit" type="primary" onClick={() => open('submit')}>Submit</Button>] : []),
    ...(trip.status === 'SUBMITTED' && hasPermission('TRIP_APPROVE') ? [<Button key="approve" aria-label="Approve" type="primary" icon={<CheckOutlined />} onClick={() => open('approve')}>Approve</Button>] : []),
    ...(trip.status === 'SUBMITTED' && hasPermission('TRIP_REJECT') ? [<Button key="reject" aria-label="Reject" danger icon={<CloseCircleOutlined />} onClick={() => open('reject')}>Reject</Button>] : []),
    ...(trip.status === 'ASSIGNED' && hasPermission('TRIP_DISPATCH') ? [<Button key="dispatch" aria-label="Dispatch" type="primary" icon={<RocketOutlined />} onClick={() => open('dispatch')}>Dispatch</Button>] : []),
    ...(trip.status === 'DISPATCHED' && hasPermission('TRIP_START') ? [<Button key="start" type="primary" onClick={() => open('start')}>Start trip</Button>] : []),
    ...(trip.status === 'IN_PROGRESS' && hasPermission('TRIP_COMPLETE') ? [<Button key="complete" type="primary" onClick={() => open('complete')}>Complete trip</Button>] : []),
    ...(trip.status === 'COMPLETED' && hasPermission('TRIP_CLOSE') ? [<Button key="close" type="primary" onClick={() => open('close')}>Close trip</Button>] : []),
    ...(canCancel && hasPermission('TRIP_CANCEL') ? [<Button key="cancel" aria-label="Cancel trip" danger icon={<StopOutlined />} onClick={() => open('cancel')}>Cancel trip</Button>] : []),
  ];

  if (!buttons.length) return null;

  const error = mutation.isError && <Alert className="lifecycle-modal-error" type="error" showIcon message="Action could not be completed" description={errorDescription(mutation.error)} />;
  const reasonModal = action === 'reject' || action === 'cancel';

  return (
    <>
      <Card variant="borderless" className="trip-detail-card lifecycle-actions">
        <Flex justify="space-between" align="center" wrap gap={12}>
          <Space direction="vertical" size={2}>
            <Text strong>Available lifecycle actions</Text>
            <Text type="secondary">Actions are permission-aware; Spring Boot validates the transition when submitted.</Text>
          </Space>
          <Space wrap>{buttons}</Space>
        </Flex>
      </Card>

      <Modal title={action === 'submit' ? 'Submit trip' : action === 'approve' ? 'Approve trip' : action === 'close' ? 'Close trip' : undefined}
        open={action === 'submit' || action === 'approve' || action === 'close'} confirmLoading={mutation.isPending}
        okText={action === 'submit' ? 'Submit trip' : action === 'approve' ? 'Approve trip' : 'Close trip'}
        onOk={() => action && execute(action)} onCancel={close}>
        {error}
        <Alert type="warning" showIcon message={`${action === 'submit' ? 'Submit' : action === 'approve' ? 'Approve' : 'Close'} ${trip.tripNumber}?`}
          description={action === 'submit' ? 'The trip will be sent for authorization.' : action === 'approve' ? 'The trip will become eligible for resource assignment.' : 'Closing finalizes the completed trip.'} />
      </Modal>

      <Modal title={action === 'reject' ? 'Reject trip' : 'Cancel trip'} open={reasonModal} confirmLoading={mutation.isPending}
        okButtonProps={{ danger: true }} okText={action === 'reject' ? 'Reject trip' : 'Cancel trip'}
        onOk={() => void reasonForm.handleSubmit((values) => action && execute(action, values))()} onCancel={close}>
        {error}
        <Form layout="vertical">
          <Form.Item label={action === 'reject' ? 'Rejection reason' : 'Cancellation reason'} required
            validateStatus={reasonForm.formState.errors.reason ? 'error' : undefined} help={reasonForm.formState.errors.reason?.message}>
            <Controller name="reason" control={reasonForm.control} render={({ field }) => <Input.TextArea {...field} aria-label={action === 'reject' ? 'Rejection reason' : 'Cancellation reason'} rows={4} maxLength={1000} showCount placeholder="Explain the business reason" />} />
          </Form.Item>
        </Form>
      </Modal>

      <Modal title="Dispatch trip" open={action === 'dispatch'} confirmLoading={mutation.isPending} okText="Dispatch trip"
        onOk={() => void dispatchForm.handleSubmit((values) => execute('dispatch', { remarks: values.remarks.trim() || null }))()} onCancel={close}>
        {error}
        <Alert type="info" showIcon message="Dispatch performs a fresh backend readiness check" description="Vehicle and driver eligibility, documents, licences, and scheduling conflicts will be revalidated." />
        <Descriptions className="lifecycle-readiness" column={1} bordered size="small">
          <Descriptions.Item label="Trip status"><TripStatusTag status={trip.status} /></Descriptions.Item>
          <Descriptions.Item label="Vehicle"><Badge status={trip.vehicleId ? 'success' : 'error'} text={trip.vehicleId ? 'Assigned' : 'Missing'} /></Descriptions.Item>
          <Descriptions.Item label="Driver"><Badge status={trip.driverId ? 'success' : 'error'} text={trip.driverId ? 'Assigned' : 'Missing'} /></Descriptions.Item>
          <Descriptions.Item label="Period">{new Date(trip.requestedStartTime).toLocaleString()} – {new Date(trip.requestedEndTime).toLocaleString()}</Descriptions.Item>
        </Descriptions>
        <Form layout="vertical">
          <Form.Item label="Dispatch remarks" validateStatus={dispatchForm.formState.errors.remarks ? 'error' : undefined} help={dispatchForm.formState.errors.remarks?.message}>
            <Controller name="remarks" control={dispatchForm.control} render={({ field }) => <Input.TextArea {...field} aria-label="Dispatch remarks" rows={3} maxLength={1000} showCount />} />
          </Form.Item>
        </Form>
      </Modal>

      <Modal title="Start trip" open={action === 'start'} confirmLoading={mutation.isPending} okText="Start trip"
        onOk={() => void startForm.handleSubmit((values) => execute('start', values))()} onCancel={close}>
        {error}
        <Alert type="info" showIcon message="The server records the authoritative actual start time" />
        <Form layout="vertical">
          <Form.Item label="Actual start time"><DatePicker showTime value={dayjs()} disabled className="lifecycle-date" /></Form.Item>
          <Form.Item label="Start odometer (km)" required validateStatus={startForm.formState.errors.startOdometerKm ? 'error' : undefined} help={startForm.formState.errors.startOdometerKm?.message}>
            <Controller name="startOdometerKm" control={startForm.control} render={({ field }) => <InputNumber aria-label="Start odometer (km)" min={0} precision={1} className="lifecycle-number" value={field.value} onChange={field.onChange} />} />
          </Form.Item>
        </Form>
      </Modal>

      <Modal title="Complete trip" open={action === 'complete'} confirmLoading={mutation.isPending} okText="Complete trip"
        onOk={() => void completeForm.handleSubmit((values) => execute('complete', { endOdometerKm: values.endOdometerKm, completionRemarks: values.completionRemarks.trim() || null }))()} onCancel={close}>
        {error}
        <Alert type="info" showIcon message="The server records the authoritative actual completion time" />
        <Form layout="vertical">
          <Form.Item label="Actual completion time"><DatePicker showTime value={dayjs()} disabled className="lifecycle-date" /></Form.Item>
          <Form.Item label="End odometer (km)" required validateStatus={completeForm.formState.errors.endOdometerKm ? 'error' : undefined} help={completeForm.formState.errors.endOdometerKm?.message}>
            <Controller name="endOdometerKm" control={completeForm.control} render={({ field }) => <InputNumber aria-label="End odometer (km)" min={0} precision={1} className="lifecycle-number" value={field.value} onChange={field.onChange} />} />
          </Form.Item>
          <Form.Item label="Completion remarks" validateStatus={completeForm.formState.errors.completionRemarks ? 'error' : undefined} help={completeForm.formState.errors.completionRemarks?.message}>
            <Controller name="completionRemarks" control={completeForm.control} render={({ field }) => <Input.TextArea {...field} aria-label="Completion remarks" rows={3} maxLength={1000} showCount />} />
          </Form.Item>
        </Form>
      </Modal>
    </>
  );
}
