import {
  DashboardOutlined,
  EditOutlined,
  ExclamationCircleOutlined,
  FieldTimeOutlined,
  HistoryOutlined,
  PlusOutlined,
  ReloadOutlined,
  WarningOutlined,
} from '@ant-design/icons';
import {
  Alert,
  App as AntApp,
  Button,
  Card,
  Col,
  DatePicker,
  Form,
  Input,
  InputNumber,
  Modal,
  Row,
  Select,
  Space,
  Statistic,
  Table,
  Tabs,
  Tag,
  Typography,
  type TableColumnsType,
} from 'antd';
import dayjs from 'dayjs';
import { useState } from 'react';
import { useAuth } from '../auth/AuthContext';
import type { VehicleMeterReset, VehicleReading, VehicleReadingType } from './types';
import {
  useCorrectVehicleReading,
  useLatestVehicleReadings,
  useRecordManualReading,
  useResetVehicleMeter,
  useVehicleMeterResets,
  useVehicleMileage,
  useVehicleReadings,
} from './useVehicleReadings';

interface VehicleReadingsSectionProps {
  vehicleId: string;
}

export default function VehicleReadingsSection({ vehicleId }: VehicleReadingsSectionProps) {
  const { message } = AntApp.useApp();
  const { hasPermission } = useAuth();

  const [readingTypeFilter, setReadingTypeFilter] = useState<VehicleReadingType | undefined>();
  const [page, setPage] = useState(0);
  const [recordModalOpen, setRecordModalOpen] = useState(false);
  const [resetModalOpen, setResetModalOpen] = useState(false);
  const [correctingReading, setCorrectingReading] = useState<VehicleReading | null>(null);

  const [recordForm] = Form.useForm();
  const [resetForm] = Form.useForm();
  const [correctForm] = Form.useForm();

  const latestQuery = useLatestVehicleReadings(vehicleId);
  const mileageQuery = useVehicleMileage(vehicleId);
  const readingsQuery = useVehicleReadings(vehicleId, { readingType: readingTypeFilter, page, limit: 10 });
  const resetsQuery = useVehicleMeterResets(vehicleId);

  const recordMutation = useRecordManualReading(vehicleId);
  const correctMutation = useCorrectVehicleReading(vehicleId);
  const resetMutation = useResetVehicleMeter(vehicleId);

  const canCreate = hasPermission('VEHICLE_READING_CREATE');
  const canCorrect = hasPermission('VEHICLE_READING_CORRECT');
  const canResetMeter = hasPermission('VEHICLE_READING_RESET_METER');

  const handleRecordSubmit = async () => {
    try {
      const values = await recordForm.validateFields();
      await recordMutation.mutateAsync({
        readingType: values.readingType,
        value: values.value,
        recordedAt: values.recordedAt ? values.recordedAt.toISOString() : dayjs().toISOString(),
        notes: values.notes,
      });
      void message.success('Vehicle reading recorded successfully');
      setRecordModalOpen(false);
      recordForm.resetFields();
    } catch (err: unknown) {
      if (err instanceof Error) {
        void message.error(err.message);
      }
    }
  };

  const handleCorrectSubmit = async () => {
    if (!correctingReading) return;
    try {
      const values = await correctForm.validateFields();
      await correctMutation.mutateAsync({
        readingId: correctingReading.id,
        payload: {
          value: values.value,
          reason: values.reason,
          recordedAt: correctingReading.recordedAt,
        },
      });
      void message.success('Vehicle reading corrected successfully');
      setCorrectingReading(null);
      correctForm.resetFields();
    } catch (err: unknown) {
      if (err instanceof Error) {
        void message.error(err.message);
      }
    }
  };

  const handleResetSubmit = async () => {
    try {
      const values = await resetForm.validateFields();
      await resetMutation.mutateAsync({
        readingType: values.readingType,
        newMeterValue: values.newMeterValue,
        effectiveAt: values.effectiveAt ? values.effectiveAt.toISOString() : dayjs().toISOString(),
        reason: values.reason,
      });
      void message.success('Physical meter reset recorded');
      setResetModalOpen(false);
      resetForm.resetFields();
    } catch (err: unknown) {
      if (err instanceof Error) {
        void message.error(err.message);
      }
    }
  };

  const readingColumns: TableColumnsType<VehicleReading> = [
    {
      title: 'Type',
      dataIndex: 'readingType',
      key: 'readingType',
      render: (type: VehicleReadingType) => (
        <Tag color={type === 'ODOMETER' ? 'blue' : 'purple'}>
          {type === 'ODOMETER' ? 'Odometer' : 'Engine Hours'}
        </Tag>
      ),
    },
    {
      title: 'Value',
      dataIndex: 'value',
      key: 'value',
      render: (value: number, record) => (
        <Space>
          <Typography.Text strong>{value.toFixed(1)}</Typography.Text>
          <Typography.Text type="secondary">{record.unit}</Typography.Text>
          {record.correctionOfReadingId && <Tag color="warning">CORRECTED</Tag>}
        </Space>
      ),
    },
    {
      title: 'Source',
      dataIndex: 'sourceType',
      key: 'sourceType',
      render: (source: string) => <Tag>{source.replace('_', ' ')}</Tag>,
    },
    {
      title: 'Epoch',
      dataIndex: 'meterEpoch',
      key: 'meterEpoch',
      render: (epoch: number) => <Tag color="cyan">E{epoch}</Tag>,
    },
    {
      title: 'Recorded At',
      dataIndex: 'recordedAt',
      key: 'recordedAt',
      render: (val: string) => (val ? dayjs(val).format('YYYY-MM-DD HH:mm') : 'â€”'),
    },
    {
      title: 'Actions',
      key: 'actions',
      render: (_, record) => (
        <Space>
          {canCorrect && !record.correctionOfReadingId && (
            <Button
              type="link"
              size="small"
              icon={<EditOutlined />}
              onClick={() => {
                setCorrectingReading(record);
                correctForm.setFieldsValue({ value: record.value });
              }}
            >
              Correct
            </Button>
          )}
        </Space>
      ),
    },
  ];

  const resetColumns: TableColumnsType<VehicleMeterReset> = [
    {
      title: 'Type',
      dataIndex: 'readingType',
      key: 'readingType',
      render: (type: string) => <Tag color="blue">{type}</Tag>,
    },
    {
      title: 'Epoch Transition',
      key: 'transition',
      render: (_, record) => `E${record.fromEpoch} â†’ E${record.toEpoch}`,
    },
    {
      title: 'Last Reading',
      dataIndex: 'lastReadingValue',
      key: 'lastReadingValue',
      render: (val: number) => val.toFixed(1),
    },
    {
      title: 'New Baseline',
      dataIndex: 'newMeterValue',
      key: 'newMeterValue',
      render: (val: number) => val.toFixed(1),
    },
    {
      title: 'Effective At',
      dataIndex: 'effectiveAt',
      key: 'effectiveAt',
      render: (val: string) => (val ? dayjs(val).format('YYYY-MM-DD HH:mm') : 'â€”'),
    },
    {
      title: 'Reason',
      dataIndex: 'reason',
      key: 'reason',
    },
  ];

  const mileage = mileageQuery.data;
  const latest = latestQuery.data;

  return (
    <Card
      title={
        <Space>
          <DashboardOutlined />
          <span>Vehicle Mileage & Readings</span>
        </Space>
      }
      extra={
        <Space>
          {canCreate && (
            <Button
              type="primary"
              size="small"
              icon={<PlusOutlined />}
              onClick={() => {
                recordForm.setFieldsValue({
                  readingType: 'ODOMETER',
                  recordedAt: dayjs(),
                });
                setRecordModalOpen(true);
              }}
            >
              Record Reading
            </Button>
          )}
          {canResetMeter && (
            <Button
              size="small"
              icon={<HistoryOutlined />}
              onClick={() => {
                resetForm.setFieldsValue({
                  readingType: 'ODOMETER',
                  newMeterValue: 0,
                  effectiveAt: dayjs(),
                });
                setResetModalOpen(true);
              }}
            >
              Reset Meter
            </Button>
          )}
          <Button
            size="small"
            icon={<ReloadOutlined />}
            onClick={() => {
              void latestQuery.refetch();
              void mileageQuery.refetch();
              void readingsQuery.refetch();
              void resetsQuery.refetch();
            }}
          />
        </Space>
      }
    >
      <Row gutter={[16, 16]} style={{ marginBottom: 16 }}>
        <Col span={12}>
          <Card size="small">
            <Statistic
              title="Current Odometer"
              value={latest?.odometer ? latest.odometer.value : 'â€”'}
              precision={1}
              suffix="km"
              prefix={<DashboardOutlined />}
            />
            {latest?.odometer && (
              <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                Epoch {latest.odometer.meterEpoch} â€¢ {dayjs(latest.odometer.recordedAt).format('YYYY-MM-DD HH:mm')}
              </Typography.Text>
            )}
          </Card>
        </Col>
        <Col span={12}>
          <Card size="small">
            <Statistic
              title="Current Engine Hours"
              value={latest?.engineHours ? latest.engineHours.value : 'â€”'}
              precision={1}
              suffix="hrs"
              prefix={<FieldTimeOutlined />}
            />
            {latest?.engineHours && (
              <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                Epoch {latest.engineHours.meterEpoch} â€¢ {dayjs(latest.engineHours.recordedAt).format('YYYY-MM-DD HH:mm')}
              </Typography.Text>
            )}
          </Card>
        </Col>
      </Row>

      {mileage && (
        <Card size="small" style={{ marginBottom: 16 }}>
          <Row gutter={[16, 8]}>
            <Col span={6}>
              <Statistic title="Total Distance" value={mileage.distanceTravelledKm ?? 0} precision={1} suffix="km" />
            </Col>
            <Col span={6}>
              <Statistic title="Hours Used" value={mileage.engineHoursUsed ?? 0} precision={1} suffix="hrs" />
            </Col>
            <Col span={6}>
              <Statistic title="Meter Resets" value={mileage.meterResetCount} />
            </Col>
            <Col span={6}>
              <Typography.Text type="secondary" style={{ display: 'block', marginBottom: 4 }}>
                Status
              </Typography.Text>
              <Space>
                <Tag color={mileage.coverageStatus === 'COMPLETE' ? 'success' : 'warning'}>
                  {mileage.coverageStatus}
                </Tag>
                {mileage.abnormalDetected && (
                  <Tag color="error" icon={<WarningOutlined />}>
                    ABNORMAL JUMP
                  </Tag>
                )}
              </Space>
            </Col>
          </Row>
          {mileage.abnormalDetected && (
            <Alert
              style={{ marginTop: 8 }}
              type="error"
              showIcon
              icon={<ExclamationCircleOutlined />}
              message="Potential Meter Tampering or Abnormal Reading Detected"
              description="A sudden anomalous distance jump or speed spike was detected between consecutive readings."
            />
          )}
        </Card>
      )}

      <Tabs
        defaultActiveKey="readings"
        items={[
          {
            key: 'readings',
            label: 'Reading History',
            children: (
              <div>
                <Space style={{ marginBottom: 12 }}>
                  <Select
                    placeholder="All reading types"
                    allowClear
                    style={{ width: 160 }}
                    value={readingTypeFilter}
                    onChange={setReadingTypeFilter}
                    options={[
                      { value: 'ODOMETER', label: 'Odometer' },
                      { value: 'ENGINE_HOURS', label: 'Engine Hours' },
                    ]}
                  />
                </Space>
                <Table<VehicleReading>
                  rowKey="id"
                  size="small"
                  columns={readingColumns}
                  dataSource={readingsQuery.data?.content ?? []}
                  loading={readingsQuery.isLoading}
                  pagination={{
                    current: page + 1,
                    pageSize: 10,
                    total: readingsQuery.data?.totalElements ?? 0,
                    onChange: (p) => setPage(p - 1),
                  }}
                />
              </div>
            ),
          },
          {
            key: 'resets',
            label: 'Meter Resets',
            children: (
              <Table<VehicleMeterReset>
                rowKey="id"
                size="small"
                columns={resetColumns}
                dataSource={resetsQuery.data ?? []}
                loading={resetsQuery.isLoading}
                pagination={{ pageSize: 5 }}
              />
            ),
          },
        ]}
      />

      {/* Record Reading Modal */}
      <Modal
        title="Record Vehicle Reading"
        open={recordModalOpen}
        onOk={handleRecordSubmit}
        onCancel={() => setRecordModalOpen(false)}
        confirmLoading={recordMutation.isPending}
        destroyOnHidden
      >
        <Form form={recordForm} layout="vertical">
          <Form.Item name="readingType" label="Reading Type" rules={[{ required: true }]}>
            <Select
              options={[
                { value: 'ODOMETER', label: 'Odometer (km)' },
                { value: 'ENGINE_HOURS', label: 'Engine Hours (hrs)' },
              ]}
            />
          </Form.Item>
          <Form.Item
            name="value"
            label="Reading Value"
            rules={[{ required: true, message: 'Value is required' }, { type: 'number', min: 0 }]}
          >
            <InputNumber style={{ width: '100%' }} precision={3} step={1} min={0} />
          </Form.Item>
          <Form.Item name="recordedAt" label="Recorded Time" rules={[{ required: true }]}>
            <DatePicker showTime style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item name="notes" label="Notes">
            <Input.TextArea rows={2} placeholder="Optional operational remarks" />
          </Form.Item>
        </Form>
      </Modal>

      {/* Correct Reading Modal */}
      <Modal
        title="Correct Vehicle Reading"
        open={Boolean(correctingReading)}
        onOk={handleCorrectSubmit}
        onCancel={() => setCorrectingReading(null)}
        confirmLoading={correctMutation.isPending}
        destroyOnHidden
      >
        <Alert
          type="info"
          showIcon
          message="Auditable Correction"
          description="The original reading remains permanently preserved in the immutable audit ledger. This correction creates a linked superseding entry."
          style={{ marginBottom: 16 }}
        />
        <Form form={correctForm} layout="vertical">
          <Form.Item
            name="value"
            label="Corrected Value"
            rules={[{ required: true, message: 'Corrected value is required' }, { type: 'number', min: 0 }]}
          >
            <InputNumber style={{ width: '100%' }} precision={3} step={1} min={0} />
          </Form.Item>
          <Form.Item
            name="reason"
            label="Reason for Correction"
            rules={[{ required: true, message: 'Correction reason is required' }]}
          >
            <Input.TextArea rows={3} placeholder="Explain why this correction is necessary (e.g. driver typo)" />
          </Form.Item>
        </Form>
      </Modal>

      {/* Reset Meter Modal */}
      <Modal
        title="Record Physical Meter Reset / Replacement"
        open={resetModalOpen}
        onOk={handleResetSubmit}
        onCancel={() => setResetModalOpen(false)}
        confirmLoading={resetMutation.isPending}
        destroyOnHidden
      >
        <Alert
          type="warning"
          showIcon
          message="Meter Replacement / Reset"
          description="Advancing the meter epoch allows lower values on new meters while preserving total historical mileage continuity across epoch transitions."
          style={{ marginBottom: 16 }}
        />
        <Form form={resetForm} layout="vertical">
          <Form.Item name="readingType" label="Reading Type" rules={[{ required: true }]}>
            <Select
              options={[
                { value: 'ODOMETER', label: 'Odometer (km)' },
                { value: 'ENGINE_HOURS', label: 'Engine Hours (hrs)' },
              ]}
            />
          </Form.Item>
          <Form.Item
            name="newMeterValue"
            label="New Meter Starting Value"
            rules={[{ required: true, message: 'New meter value is required' }, { type: 'number', min: 0 }]}
          >
            <InputNumber style={{ width: '100%' }} precision={3} step={1} min={0} />
          </Form.Item>
          <Form.Item name="effectiveAt" label="Effective Time" rules={[{ required: true }]}>
            <DatePicker showTime style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item
            name="reason"
            label="Reason for Reset"
            rules={[{ required: true, message: 'Reset reason is required' }]}
          >
            <Input.TextArea rows={3} placeholder="e.g. Physical odometer gauge replaced under maintenance" />
          </Form.Item>
        </Form>
      </Modal>
    </Card>
  );
}