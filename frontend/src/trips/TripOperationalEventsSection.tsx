import React, { useState } from 'react';
import {
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
import {
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
  const { hasPermission } = useAuth();
  const { data: events, isLoading, isError, refetch } = useTripOperationalEvents(trip.id);

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
      checkpointForm.resetFields();
      setCheckpointModalOpen(false);
    } catch {
      // validation error handled by form
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
      delayForm.resetFields();
      setDelayModalOpen(false);
    } catch {
      // validation error handled by form
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
      incidentForm.resetFields();
      setIncidentModalOpen(false);
    } catch {
      // validation error handled by form
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

  return (
    <Card
      variant="borderless"
      className="trip-detail-card"
      title={
        <Space>
          <EnvironmentOutlined />
          <span>En-Route Checkpoints & Operational Events</span>
          {events && events.length > 0 && <Badge count={events.length} showZero color="#1890ff" />}
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

      {!isLoading && !isError && (!events || events.length === 0) && (
        <Empty
          image={Empty.PRESENTED_IMAGE_SIMPLE}
          description="No en-route checkpoints, delays, or incidents have been recorded yet."
        />
      )}

      {events && events.length > 0 && (
        <Timeline
          pending={isLoading ? 'Loading operational log...' : undefined}
          items={events.map(renderTimelineItem)}
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
            rules={[{ required: true, message: 'Please enter location description' }]}
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

          <Form.Item name="remarks" label="Remarks / Observations">
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
            <InputNumber min={1} max={1440} style={{ width: '100%' }} placeholder="e.g. 30" />
          </Form.Item>

          <Form.Item
            name="reason"
            label="Delay Reason"
            rules={[{ required: true, message: 'Please enter the delay reason' }]}
          >
            <Input placeholder="e.g. Heavy traffic congestion, Customer loading delay, Road block" />
          </Form.Item>

          <Form.Item name="locationDescription" label="Location / Section">
            <Input placeholder="e.g. Expressway Mile 24, Warehouse B Loading Bay" />
          </Form.Item>

          <Form.Item
            name="occurredAt"
            label="Occurred Date & Time"
            rules={[{ required: true, message: 'Please select time' }]}
          >
            <DatePicker showTime format="YYYY-MM-DD HH:mm" style={{ width: '100%' }} />
          </Form.Item>

          <Form.Item name="remarks" label="Remarks / Mitigation">
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
            rules={[{ required: true, message: 'Please describe the incident' }]}
          >
            <Input placeholder="e.g. Engine coolant leak, tire puncture, police inspection" />
          </Form.Item>

          <Form.Item name="locationDescription" label="Location of Occurrence">
            <Input placeholder="e.g. Kandy Road near Ambepussa" />
          </Form.Item>

          <Form.Item
            name="occurredAt"
            label="Occurred Date & Time"
            rules={[{ required: true, message: 'Please select time' }]}
          >
            <DatePicker showTime format="YYYY-MM-DD HH:mm" style={{ width: '100%' }} />
          </Form.Item>

          <Form.Item name="remarks" label="Action Taken / Support Requested">
            <Input.TextArea rows={3} placeholder="Support requested, recovery vehicle dispatched, etc." />
          </Form.Item>
        </Form>
      </Modal>
    </Card>
  );
}
