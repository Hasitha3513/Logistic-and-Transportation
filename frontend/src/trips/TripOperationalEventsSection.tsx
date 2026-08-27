import React, { useState } from 'react';
import {
  App,
  Alert,
  Badge,
  Button,
  Card,
  DatePicker,
  Empty,
  Form,
  Input,
  InputNumber,
  Modal,
  Select,
  Space,
  Tag,
  Timeline,
  Typography,
} from 'antd';
import {
  AlertOutlined,
  ClockCircleOutlined,
  EnvironmentOutlined,
  FlagOutlined,
} from '@ant-design/icons';
import dayjs from 'dayjs';
import { useAuth } from '../auth/AuthContext';
import { OfflineOperationActions } from '../features/offlineSync/OfflineOperationActions';
import { OFFLINE_STATUS_PRESENTATION } from '../features/offlineSync/presentation';
import type { OfflineOperation } from '../features/offlineSync/types';
import {
  useLocalTripOperationalEvents,
  useRecordTripCheckpoint,
  useRecordTripDelay,
  useRecordTripIncident,
  useTripOperationalEvents,
} from './useTripOperationalEvents';
import type {
  Trip,
  TripCheckpointType,
  TripIncidentSeverity,
  TripOperationalEvent,
} from './types';

const { Text, Paragraph } = Typography;

type TripOperationalOfflineOperation = Exclude<
  OfflineOperation,
  { operationType: 'VEHICLE_READING_RECORD' }
>;

const dateTimeFormat = new Intl.DateTimeFormat('en-GB', {
  dateStyle: 'medium',
  timeStyle: 'short',
});

function formatEventDate(value?: string | null) {
  if (!value) return '—';
  const d = new Date(value);
  return Number.isNaN(d.getTime()) ? '—' : dateTimeFormat.format(d);
}

const CHECKPOINT_LABELS: Record<TripCheckpointType, { label: string; color: string }> = {
  DEPARTURE: { label: 'Departure', color: 'blue' },
  ARRIVAL: { label: 'Arrival', color: 'green' },
  PICKUP: { label: 'Pickup Point', color: 'cyan' },
  DELIVERY: { label: 'Delivery Point', color: 'geekblue' },
  REST_STOP: { label: 'Rest Stop', color: 'purple' },
  CUSTOM: { label: 'Custom Checkpoint', color: 'default' },
};

const INCIDENT_SEVERITY_TAGS: Record<TripIncidentSeverity, { color: string; label: string }> = {
  LOW: { color: 'blue', label: 'Low' },
  MEDIUM: { color: 'orange', label: 'Medium' },
  HIGH: { color: 'volcano', label: 'High' },
  CRITICAL: { color: 'red', label: 'Critical' },
};

interface TripOperationalEventsSectionProps {
  trip: Trip;
}

export default function TripOperationalEventsSection({ trip }: TripOperationalEventsSectionProps) {
  const { message } = App.useApp();
  const { hasPermission } = useAuth();
  const { data: events, isLoading, isError, refetch } = useTripOperationalEvents(trip.id);
  const { data: localEvents = [] } = useLocalTripOperationalEvents(trip.id);
  const visibleLocalEvents = localEvents.filter((operation) => operation.status !== 'SYNCED');

  const recordCheckpointMutation = useRecordTripCheckpoint(trip.id);
  const recordDelayMutation = useRecordTripDelay(trip.id);
  const recordIncidentMutation = useRecordTripIncident(trip.id);

  const [checkpointModalOpen, setCheckpointModalOpen] = useState(false);
  const [delayModalOpen, setDelayModalOpen] = useState(false);
  const [incidentModalOpen, setIncidentModalOpen] = useState(false);

  const [checkpointForm] = Form.useForm();
  const [delayForm] = Form.useForm();
  const [incidentForm] = Form.useForm();

  const canManage =
    hasPermission('TRIP_LOG_MANAGE') ||
    hasPermission('TRIP_DISPATCH') ||
    hasPermission('TRIP_UPDATE');

  const isTripActiveForEvents = ['DISPATCHED', 'IN_PROGRESS', 'ASSIGNED', 'APPROVED'].includes(
    trip.status
  );

  const handleCheckpointSubmit = async () => {
    try {
      const values = await checkpointForm.validateFields();
      await recordCheckpointMutation.mutateAsync({
        checkpointType: values.checkpointType,
        locationDescription: values.locationDescription,
        occurredAt: values.occurredAt ? values.occurredAt.toISOString() : undefined,
        remarks: values.remarks,
      });
      void message.success('Checkpoint queued for synchronization');
      checkpointForm.resetFields();
      setCheckpointModalOpen(false);
    } catch (error: unknown) {
      if (error instanceof Error) void message.error(error.message);
    }
  };

  const handleDelaySubmit = async () => {
    try {
      const values = await delayForm.validateFields();
      await recordDelayMutation.mutateAsync({
        delayMinutes: values.delayMinutes,
        reason: values.reason,
        locationDescription: values.locationDescription,
        occurredAt: values.occurredAt ? values.occurredAt.toISOString() : undefined,
        remarks: values.remarks,
      });
      void message.success('Delay queued for synchronization');
      delayForm.resetFields();
      setDelayModalOpen(false);
    } catch (error: unknown) {
      if (error instanceof Error) void message.error(error.message);
    }
  };

  const handleIncidentSubmit = async () => {
    try {
      const values = await incidentForm.validateFields();
      await recordIncidentMutation.mutateAsync({
        incidentSeverity: values.incidentSeverity,
        description: values.description,
        locationDescription: values.locationDescription,
        occurredAt: values.occurredAt ? values.occurredAt.toISOString() : undefined,
        remarks: values.remarks,
      });
      void message.success('Incident queued for synchronization');
      incidentForm.resetFields();
      setIncidentModalOpen(false);
    } catch (error: unknown) {
      if (error instanceof Error) void message.error(error.message);
    }
  };

  const renderTimelineItem = (item: TripOperationalEvent) => {
    if (item.eventType === 'CHECKPOINT') {
      const meta = (item.checkpointType && CHECKPOINT_LABELS[item.checkpointType]) || {
        label: item.checkpointType ?? 'Checkpoint',
        color: 'blue',
      };
      return {
        color: meta.color,
        dot: <FlagOutlined style={{ fontSize: 16 }} />,
        children: (
          <div className="trip-operational-event-item">
            <Space wrap size={8}>
              <Tag color={meta.color}>{meta.label}</Tag>
              <Text strong>{item.locationDescription ?? 'En-route Checkpoint'}</Text>
            </Space>
            <div>
              <Text type="secondary">
                {formatEventDate(item.occurredAt)} · Recorded by {item.recordedBy}
              </Text>
            </div>
            {item.remarks && <Paragraph style={{ marginTop: 4 }}>{item.remarks}</Paragraph>}
          </div>
        ),
      };
    }

    if (item.eventType === 'DELAY') {
      return {
        color: 'orange',
        dot: <ClockCircleOutlined style={{ fontSize: 16 }} />,
        children: (
          <div className="trip-operational-event-item">
            <Space wrap size={8}>
              <Tag color="orange">Delay: {item.delayMinutes} mins</Tag>
              <Text strong>{item.reason}</Text>
            </Space>
            <div>
              <Text type="secondary">
                {formatEventDate(item.occurredAt)}
                {item.locationDescription ? ` · ${item.locationDescription}` : ''} · Recorded by{' '}
                {item.recordedBy}
              </Text>
            </div>
            {item.remarks && <Paragraph style={{ marginTop: 4 }}>{item.remarks}</Paragraph>}
          </div>
        ),
      };
    }

    if (item.eventType === 'INCIDENT') {
      const severityMeta = (item.incidentSeverity &&
        INCIDENT_SEVERITY_TAGS[item.incidentSeverity]) || {
        color: 'red',
        label: item.incidentSeverity ?? 'Incident',
      };
      return {
        color: severityMeta.color,
        dot: <AlertOutlined style={{ fontSize: 16 }} />,
        children: (
          <div className="trip-operational-event-item">
            <Space wrap size={8}>
              <Tag color={severityMeta.color}>Incident: {severityMeta.label}</Tag>
              <Text strong>{item.reason}</Text>
            </Space>
            <div>
              <Text type="secondary">
                {formatEventDate(item.occurredAt)}
                {item.locationDescription ? ` · ${item.locationDescription}` : ''} · Reported by{' '}
                {item.recordedBy}
              </Text>
            </div>
            {item.remarks && <Paragraph style={{ marginTop: 4 }}>{item.remarks}</Paragraph>}
          </div>
        ),
      };
    }

    return {
      children: <Text>{item.eventType}</Text>,
    };
  };

  const renderLocalTimelineItem = (operation: TripOperationalOfflineOperation) => {
    const status = OFFLINE_STATUS_PRESENTATION[operation.status];
    let title: React.ReactNode;
    let summary: React.ReactNode;
    let occurredAt: string;
    let location: string | undefined;
    let remarks: string | undefined;
    if (operation.operationType === 'TRIP_CHECKPOINT_RECORD') {
      const meta = CHECKPOINT_LABELS[operation.payload.checkpointType];
      title = <Tag color={meta.color}>{meta.label}</Tag>;
      summary = <Text strong>{operation.payload.locationDescription ?? 'En-route Checkpoint'}</Text>;
      occurredAt = operation.payload.occurredAt;
      location = operation.payload.locationDescription;
      remarks = operation.payload.remarks;
    } else if (operation.operationType === 'TRIP_DELAY_RECORD') {
      title = <Tag color="orange">Delay: {operation.payload.delayMinutes} mins</Tag>;
      summary = <Text strong>{operation.payload.reason}</Text>;
      occurredAt = operation.payload.occurredAt;
      location = operation.payload.locationDescription;
      remarks = operation.payload.remarks;
    } else {
      const meta = INCIDENT_SEVERITY_TAGS[operation.payload.incidentSeverity];
      title = <Tag color={meta.color}>Incident: {meta.label}</Tag>;
      summary = <Text strong>{operation.payload.description}</Text>;
      occurredAt = operation.payload.occurredAt;
      location = operation.payload.locationDescription;
      remarks = operation.payload.remarks;
    }
    return {
      key: operation.operationId,
      color: status.color,
      children: (
        <div className="trip-operational-event-item" data-local-operation={operation.operationId}>
          <Space wrap size={8}>{title}{summary}<Tag color={status.color}>{status.detailLabel}</Tag></Space>
          <div><Text type="secondary">{formatEventDate(occurredAt)}{location ? ` · ${location}` : ''} · Local capture</Text></div>
          {remarks && <Paragraph style={{ marginTop: 4 }}>{remarks}</Paragraph>}
          {(operation.status === 'FAILED' || operation.status === 'CONFLICT') && operation.lastErrorMessage && (
            <Alert type={operation.status === 'CONFLICT' ? 'warning' : 'error'} showIcon
              message={operation.lastErrorMessage} style={{ marginTop: 8 }} />
          )}
          <OfflineOperationActions operation={operation} compact />
        </div>
      ),
    };
  };

  return (
    <Card
      variant="borderless"
      className="trip-detail-card"
      title={
        <Space>
          <EnvironmentOutlined />
          <span>En-Route Checkpoints & Operational Events</span>
          {(events || visibleLocalEvents.length > 0) && (
            <Badge count={(events?.length ?? 0) + visibleLocalEvents.length} showZero color="#1890ff" />
          )}
        </Space>
      }
      extra={
        canManage && (
          <Space wrap>
            <Button
              type="primary"
              icon={<FlagOutlined />}
              disabled={!isTripActiveForEvents}
              onClick={() => {
                checkpointForm.resetFields();
                checkpointForm.setFieldsValue({ occurredAt: dayjs() });
                setCheckpointModalOpen(true);
              }}
            >
              Record Checkpoint
            </Button>
            <Button
              icon={<ClockCircleOutlined />}
              disabled={!isTripActiveForEvents}
              onClick={() => {
                delayForm.resetFields();
                delayForm.setFieldsValue({ occurredAt: dayjs() });
                setDelayModalOpen(true);
              }}
            >
              Record Delay
            </Button>
            <Button
              danger
              icon={<AlertOutlined />}
              disabled={!isTripActiveForEvents}
              onClick={() => {
                incidentForm.resetFields();
                incidentForm.setFieldsValue({ occurredAt: dayjs() });
                setIncidentModalOpen(true);
              }}
            >
              Record Incident
            </Button>
          </Space>
        )
      }
    >
      {!isTripActiveForEvents && (
        <Alert
          type="info"
          showIcon
          message={`Trip is currently in ${trip.status} status. Operational events can be recorded once the trip is dispatched or in-progress.`}
          style={{ marginBottom: 16 }}
        />
      )}

      {isError && (
        <Alert
          type="error"
          showIcon
          message="Operational events could not be loaded"
          action={<Button size="small" onClick={() => refetch()}>Retry</Button>}
          style={{ marginBottom: 16 }}
        />
      )}

      {!isLoading && !isError && (!events || events.length === 0) && visibleLocalEvents.length === 0 && (
        <Empty
          image={Empty.PRESENTED_IMAGE_SIMPLE}
          description="No en-route checkpoints, delays, or incidents have been recorded yet."
        />
      )}

      {((events && events.length > 0) || visibleLocalEvents.length > 0) && (
        <Timeline
          pending={isLoading ? 'Loading operational log...' : undefined}
          items={[
            ...(events?.map(renderTimelineItem) ?? []),
            ...visibleLocalEvents.map(renderLocalTimelineItem),
          ]}
        />
      )}

      {/* Checkpoint Modal */}
      <Modal
        title={
          <Space>
            <FlagOutlined style={{ color: '#1890ff' }} />
            <span>Record En-Route Checkpoint</span>
          </Space>
        }
        open={checkpointModalOpen}
        onOk={handleCheckpointSubmit}
        onCancel={() => setCheckpointModalOpen(false)}
        confirmLoading={recordCheckpointMutation.isPending}
        okText="Record Checkpoint"
      >
        <Form form={checkpointForm} layout="vertical">
          <Form.Item
            name="checkpointType"
            label="Checkpoint Type"
            rules={[{ required: true, message: 'Please select a checkpoint type' }]}
          >
            <Select placeholder="Select checkpoint type">
              <Select.Option value="DEPARTURE">Departure</Select.Option>
              <Select.Option value="ARRIVAL">Arrival</Select.Option>
              <Select.Option value="PICKUP">Pickup Point</Select.Option>
              <Select.Option value="DELIVERY">Delivery Point</Select.Option>
              <Select.Option value="REST_STOP">Rest Stop</Select.Option>
              <Select.Option value="CUSTOM">Custom Waypoint</Select.Option>
            </Select>
          </Form.Item>

          <Form.Item
            name="locationDescription"
            label="Location / Waypoint Description"
            rules={[{ max: 255, message: 'Location cannot exceed 255 characters' }]}
          >
            <Input placeholder="e.g. Colombo Port Gate 4, Expressway Toll Exit" />
          </Form.Item>

          <Form.Item
            name="occurredAt"
            label="Occurred Date & Time"
            rules={[{ required: true, message: 'Please select time' }]}
          >
            <DatePicker showTime format="YYYY-MM-DD HH:mm" style={{ width: '100%' }} />
          </Form.Item>

          <Form.Item name="remarks" label="Remarks / Observations"
            rules={[{ max: 2000, message: 'Remarks cannot exceed 2000 characters' }]}>
            <Input.TextArea rows={3} placeholder="Optional operational notes or handover remarks" />
          </Form.Item>
        </Form>
      </Modal>

      {/* Delay Modal */}
      <Modal
        title={
          <Space>
            <ClockCircleOutlined style={{ color: '#fa8c16' }} />
            <span>Record Operational Delay</span>
          </Space>
        }
        open={delayModalOpen}
        onOk={handleDelaySubmit}
        onCancel={() => setDelayModalOpen(false)}
        confirmLoading={recordDelayMutation.isPending}
        okText="Record Delay"
      >
        <Form form={delayForm} layout="vertical">
          <Form.Item
            name="delayMinutes"
            label="Delay Duration (Minutes)"
            rules={[
              { required: true, message: 'Please enter delay duration in minutes' },
              {
                type: 'number',
                min: 1,
                message: 'Delay duration must be at least 1 minute',
              },
            ]}
          >
            <InputNumber min={1} precision={0} style={{ width: '100%' }} placeholder="e.g. 30" />
          </Form.Item>

          <Form.Item
            name="reason"
            label="Delay Reason"
            rules={[{ required: true, whitespace: true, message: 'Please enter the delay reason' },
              { max: 500, message: 'Reason cannot exceed 500 characters' }]}
          >
            <Input placeholder="e.g. Heavy traffic congestion, Customer loading delay, Road block" />
          </Form.Item>

          <Form.Item name="locationDescription" label="Location / Section"
            rules={[{ max: 255, message: 'Location cannot exceed 255 characters' }]}>
            <Input placeholder="e.g. Expressway Mile 24, Warehouse B Loading Bay" />
          </Form.Item>

          <Form.Item
            name="occurredAt"
            label="Occurred Date & Time"
            rules={[{ required: true, message: 'Please select time' }]}
          >
            <DatePicker showTime format="YYYY-MM-DD HH:mm" style={{ width: '100%' }} />
          </Form.Item>

          <Form.Item name="remarks" label="Remarks / Mitigation"
            rules={[{ max: 2000, message: 'Remarks cannot exceed 2000 characters' }]}>
            <Input.TextArea rows={3} placeholder="Mitigation actions taken or ETA adjustment notes" />
          </Form.Item>
        </Form>
      </Modal>

      {/* Incident Modal */}
      <Modal
        title={
          <Space>
            <AlertOutlined style={{ color: '#ff4d4f' }} />
            <span>Record Operational Incident</span>
          </Space>
        }
        open={incidentModalOpen}
        onOk={handleIncidentSubmit}
        onCancel={() => setIncidentModalOpen(false)}
        confirmLoading={recordIncidentMutation.isPending}
        okText="Record Incident"
      >
        <Form form={incidentForm} layout="vertical">
          <Form.Item
            name="incidentSeverity"
            label="Incident Severity"
            rules={[{ required: true, message: 'Please select incident severity' }]}
          >
            <Select placeholder="Select incident severity">
              <Select.Option value="LOW">Low (Minor delay / Non-critical)</Select.Option>
              <Select.Option value="MEDIUM">Medium (Tire puncture / Minor malfunction)</Select.Option>
              <Select.Option value="HIGH">High (Mechanical breakdown / Collision)</Select.Option>
              <Select.Option value="CRITICAL">Critical (Severe accident / Cargo loss)</Select.Option>
            </Select>
          </Form.Item>

          <Form.Item
            name="description"
            label="Incident Description / Cause"
            rules={[{ required: true, whitespace: true, message: 'Please describe the incident' },
              { max: 500, message: 'Description cannot exceed 500 characters' }]}
          >
            <Input placeholder="e.g. Engine coolant leak, tire puncture, police inspection" />
          </Form.Item>

          <Form.Item name="locationDescription" label="Location of Occurrence"
            rules={[{ max: 255, message: 'Location cannot exceed 255 characters' }]}>
            <Input placeholder="e.g. Kandy Road near Ambepussa" />
          </Form.Item>

          <Form.Item
            name="occurredAt"
            label="Occurred Date & Time"
            rules={[{ required: true, message: 'Please select time' }]}
          >
            <DatePicker showTime format="YYYY-MM-DD HH:mm" style={{ width: '100%' }} />
          </Form.Item>

          <Form.Item name="remarks" label="Action Taken / Support Requested"
            rules={[{ max: 2000, message: 'Remarks cannot exceed 2000 characters' }]}>
            <Input.TextArea rows={3} placeholder="Support requested, recovery vehicle dispatched, etc." />
          </Form.Item>
        </Form>
      </Modal>
    </Card>
  );
}
