import {
  BarChartOutlined,
  CalendarOutlined,
  CheckCircleOutlined,
  DashboardOutlined,
  EditOutlined,
  ExclamationCircleOutlined,
  HistoryOutlined,
  InfoCircleOutlined,
  PlusOutlined,
  ReloadOutlined,
  SwapOutlined,
} from '@ant-design/icons';
import {
  Alert,
  App as AntApp,
  Badge,
  Button,
  Card,
  Col,
  Descriptions,
  Flex,
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
  Tooltip,
  Typography,
  type TableColumnsType,
} from 'antd';
import { useState } from 'react';
import { useAuth } from '../auth/AuthContext';
import type {
  CorrectionRequest,
  CoverageStatus,
  ManualReadingRequest,
  MeterResetRequest,
  MeterResetResponse,
  VehicleReadingResponse,
  VehicleReadingType,
} from './types';
import {
  useCorrectReading,
  useLatestVehicleReadings,
  useRecordManualReading,
  useResetVehicleMeter,
  useVehicleMeterResets,
  useVehicleMileageSummary,
  useVehicleReadings,
} from './useVehicleReadings';

interface VehicleReadingsSectionProps {
  vehicleId: string;
  vehicleRegistration?: string;
}

const statusTag = (status: string) => {
  switch (status) {
    case 'ACTIVE':
      return <Tag color="success">Active</Tag>;
    case 'CORRECTED':
      return <Tag color="default">Corrected</Tag>;
    case 'CORRECTION':
      return <Tag color="processing">Correction</Tag>;
    default:
      return <Tag>{status}</Tag>;
  }
};

const sourceTag = (source: string) => {
  switch (source) {
    case 'MANUAL':
      return <Tag color="blue">Manual</Tag>;
    case 'TRIP_START':
      return <Tag color="cyan">Trip Start</Tag>;
    case 'TRIP_END':
      return <Tag color="geekblue">Trip End</Tag>;
    case 'FUEL_ISSUE':
      return <Tag color="orange">Fuel Issue</Tag>;
    case 'BASELINE':
      return <Tag color="purple">Baseline</Tag>;
    case 'METER_RESET':
      return <Tag color="magenta">Meter Reset</Tag>;
    default:
      return <Tag>{source}</Tag>;
  }
};

const coverageBadge = (status?: CoverageStatus) => {
  switch (status) {
    case 'COMPLETE':
      return (
        <Tag color="success" icon={<CheckCircleOutlined />}>
          Complete Coverage
        </Tag>
      );
    case 'PARTIAL':
      return (
        <Tag color="warning" icon={<ExclamationCircleOutlined />}>
          Partial Coverage
        </Tag>
      );
    case 'NO_DATA':
      return (
        <Tag color="default" icon={<InfoCircleOutlined />}>
          No Data
        </Tag>
      );
    default:
      return null;
  }
};

const formatDate = (iso: string | null | undefined) => {
  if (!iso) return '—';
  try {
    return new Date(iso).toLocaleString();
  } catch {
    return iso;
  }
};

export default function VehicleReadingsSection({ vehicleId, vehicleRegistration }: VehicleReadingsSectionProps) {
  const { message } = AntApp.useApp();
  const { hasPermission } = useAuth();

  const [typeFilter, setTypeFilter] = useState<VehicleReadingType | undefined>();
  const [activeTab, setActiveTab] = useState<'readings' | 'mileage' | 'resets'>('readings');

  // Summary Date Range (defaults to last 30 days)
  const defaultFrom = new Date(Date.now() - 30 * 24 * 60 * 60 * 1000).toISOString().slice(0, 10) + 'T00:00:00Z';
  const defaultTo = new Date().toISOString().slice(0, 10) + 'T23:59:59Z';
  const [summaryFrom, setSummaryFrom] = useState(defaultFrom);
  const [summaryTo, setSummaryTo] = useState(defaultTo);

  // Modals
  const [isAddOpen, setIsAddOpen] = useState(false);
  const [correctingReading, setCorrectingReading] = useState<VehicleReadingResponse | null>(null);
  const [isResetOpen, setIsResetOpen] = useState(false);

  // Forms
  const [addForm] = Form.useForm();
  const [correctForm] = Form.useForm();
  const [resetForm] = Form.useForm();

  // Queries & Mutations
  const readingsQuery = useVehicleReadings(vehicleId, typeFilter);
  const latestQuery = useLatestVehicleReadings(vehicleId);
  const resetsQuery = useVehicleMeterResets(vehicleId);
  const mileageSummaryQuery = useVehicleMileageSummary(vehicleId, summaryFrom, summaryTo, true);

  const recordManual = useRecordManualReading(vehicleId);
  const correctReading = useCorrectReading(vehicleId);
  const resetMeter = useResetVehicleMeter(vehicleId);

  // Permissions
  const canView = hasPermission('VEHICLE_READING_VIEW');
  const canCreate = hasPermission('VEHICLE_READING_CREATE');
  const canCorrect = hasPermission('VEHICLE_READING_CORRECT');
  const canResetMeter = hasPermission('VEHICLE_READING_RESET_METER');

  if (!canView) {
    return null;
  }

  const onAddSubmit = async (values: {
    readingType: VehicleReadingType;
    value: number;
    recordedAt: string;
    notes?: string;
  }) => {
    try {
      const payload: ManualReadingRequest = {
        readingType: values.readingType,
        value: values.value,
        recordedAt: new Date(values.recordedAt).toISOString(),
        idempotencyKey: `MANUAL-${vehicleId}-${values.readingType}-${Date.now()}`,
        notes: values.notes,
      };
      await recordManual.mutateAsync(payload);
      void message.success('Manual reading recorded successfully');
      setIsAddOpen(false);
      addForm.resetFields();
    } catch (err: unknown) {
      const errorMsg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message ?? 'Failed to record manual reading';
      void message.error(errorMsg);
    }
  };

  const onCorrectSubmit = async (values: {
    value: number;
    reason: string;
    notes?: string;
  }) => {
    if (!correctingReading) return;
    try {
      const payload: CorrectionRequest = {
        value: values.value,
        reason: values.reason,
        idempotencyKey: `CORR-${correctingReading.id}-${Date.now()}`,
        notes: values.notes,
      };
      await correctReading.mutateAsync({ readingId: correctingReading.id, payload });
      void message.success('Correction recorded successfully');
      setCorrectingReading(null);
      correctForm.resetFields();
    } catch (err: unknown) {
      const errorMsg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message ?? 'Failed to record correction';
      void message.error(errorMsg);
    }
  };

  const onResetSubmit = async (values: {
    readingType: VehicleReadingType;
    newMeterValue: number;
    effectiveAt: string;
    reason: string;
    notes?: string;
  }) => {
    try {
      const payload: MeterResetRequest = {
        readingType: values.readingType,
        newMeterValue: values.newMeterValue,
        effectiveAt: new Date(values.effectiveAt).toISOString(),
        reason: values.reason,
        notes: values.notes,
      };
      await resetMeter.mutateAsync(payload);
      void message.success('Meter replacement event recorded successfully. New meter epoch initiated.');
      setIsResetOpen(false);
      resetForm.resetFields();
    } catch (err: unknown) {
      const errorMsg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message ?? 'Failed to record meter replacement';
      void message.error(errorMsg);
    }
  };

  const readingColumns: TableColumnsType<VehicleReadingResponse> = [
    {
      title: 'Recorded At',
      dataIndex: 'recordedAt',
      key: 'recordedAt',
      width: 170,
      render: (val: string) => formatDate(val),
    },
    {
      title: 'Type',
      dataIndex: 'readingType',
      key: 'readingType',
      width: 120,
      render: (type: VehicleReadingType) => (
        <Tag color={type === 'ODOMETER' ? 'cyan' : 'gold'}>
          {type === 'ODOMETER' ? 'Odometer' : 'Engine Hours'}
        </Tag>
      ),
    },
    {
      title: 'Value',
      dataIndex: 'value',
      key: 'value',
      width: 130,
      render: (val: number, record) => (
        <Typography.Text strong delete={record.status === 'CORRECTED'}>
          {Number(val).toLocaleString(undefined, { minimumFractionDigits: 1, maximumFractionDigits: 3 })}{' '}
          <Typography.Text type="secondary" style={{ fontSize: 11 }}>
            {record.unit === 'KILOMETER' ? 'km' : 'hrs'}
          </Typography.Text>
        </Typography.Text>
      ),
    },
    {
      title: 'Epoch',
      dataIndex: 'meterEpoch',
      key: 'meterEpoch',
      width: 80,
      render: (epoch: number) => <Tag>v{epoch}</Tag>,
    },
    {
      title: 'Source',
      dataIndex: 'sourceType',
      key: 'sourceType',
      width: 120,
      render: (src: string) => sourceTag(src),
    },
    {
      title: 'Status',
      dataIndex: 'status',
      key: 'status',
      width: 110,
      render: (status: string) => statusTag(status),
    },
    {
      title: 'Notes / Reason',
      key: 'notes',
      ellipsis: true,
      render: (_, record) => (
        <Space direction="vertical" size={0}>
          {record.correctionReason && (
            <Typography.Text type="danger" style={{ fontSize: 12 }}>
              Reason: {record.correctionReason}
            </Typography.Text>
          )}
          {record.notes && (
            <Typography.Text type="secondary" style={{ fontSize: 12 }}>
              {record.notes}
            </Typography.Text>
          )}
        </Space>
      ),
    },
    {
      title: 'Action',
      key: 'action',
      width: 100,
      render: (_, record) => {
        if (record.status === 'ACTIVE' && canCorrect) {
          return (
            <Tooltip title="Submit corrected value for this reading">
              <Button
                size="small"
                type="link"
                icon={<EditOutlined />}
                onClick={() => {
                  setCorrectingReading(record);
                  correctForm.setFieldsValue({
                    value: record.value,
                    reason: '',
                    notes: '',
                  });
                }}
              >
                Correct
              </Button>
            </Tooltip>
          );
        }
        return null;
      },
    },
  ];

  const resetColumns: TableColumnsType<MeterResetResponse> = [
    {
      title: 'Effective At',
      dataIndex: 'effectiveAt',
      key: 'effectiveAt',
      width: 170,
      render: (val: string) => formatDate(val),
    },
    {
      title: 'Type',
      dataIndex: 'readingType',
      key: 'readingType',
      width: 120,
      render: (type: VehicleReadingType) => (
        <Tag color={type === 'ODOMETER' ? 'cyan' : 'gold'}>
          {type === 'ODOMETER' ? 'Odometer' : 'Engine Hours'}
        </Tag>
      ),
    },
    {
      title: 'Previous Value',
      dataIndex: 'previousMeterValue',
      key: 'previousMeterValue',
      width: 130,
      render: (val: number, record) => (
        <Typography.Text>
          {Number(val).toLocaleString(undefined, { minimumFractionDigits: 1, maximumFractionDigits: 3 })}{' '}
          {record.readingType === 'ODOMETER' ? 'km' : 'hrs'}
        </Typography.Text>
      ),
    },
    {
      title: 'New Initial Value',
      dataIndex: 'newMeterValue',
      key: 'newMeterValue',
      width: 130,
      render: (val: number, record) => (
        <Typography.Text strong type="success">
          {Number(val).toLocaleString(undefined, { minimumFractionDigits: 1, maximumFractionDigits: 3 })}{' '}
          {record.readingType === 'ODOMETER' ? 'km' : 'hrs'}
        </Typography.Text>
      ),
    },
    {
      title: 'Reason',
      dataIndex: 'reason',
      key: 'reason',
      ellipsis: true,
    },
    {
      title: 'Notes',
      dataIndex: 'notes',
      key: 'notes',
      ellipsis: true,
      render: (val: string | null) => val || '—',
    },
  ];

  const latestOdo = latestQuery.data?.odometer;
  const latestEngine = latestQuery.data?.engineHours;
  const summary = mileageSummaryQuery.data;

  const toLocalIsoDefault = () => {
    const d = new Date();
    d.setMinutes(d.getMinutes() - d.getTimezoneOffset());
    return d.toISOString().slice(0, 16);
  };

  return (
    <Card
      size="small"
      title={
        <Space>
          <DashboardOutlined />
          <span>Vehicle Readings, Mileage &amp; Utilization</span>
        </Space>
      }
      extra={
        <Space wrap>
          {canCreate && (
            <Button
              size="small"
              type="primary"
              icon={<PlusOutlined />}
              onClick={() => {
                addForm.setFieldsValue({
                  readingType: 'ODOMETER',
                  recordedAt: toLocalIsoDefault(),
                });
                setIsAddOpen(true);
              }}
            >
              Add Reading
            </Button>
          )}
          {canResetMeter && (
            <Button
              size="small"
              icon={<SwapOutlined />}
              onClick={() => {
                resetForm.setFieldsValue({
                  readingType: 'ODOMETER',
                  newMeterValue: 0,
                  effectiveAt: toLocalIsoDefault(),
                });
                setIsResetOpen(true);
              }}
            >
              Record Meter Replacement
            </Button>
          )}
          <Button
            size="small"
            icon={<ReloadOutlined />}
            loading={readingsQuery.isFetching || latestQuery.isFetching || mileageSummaryQuery.isFetching}
            onClick={() => {
              void readingsQuery.refetch();
              void latestQuery.refetch();
              void resetsQuery.refetch();
              void mileageSummaryQuery.refetch();
            }}
          />
        </Space>
      }
      style={{ marginTop: 16 }}
    >
      {/* Metrics Banner */}
      <Row gutter={[16, 16]} style={{ marginBottom: 16 }}>
        <Col xs={24} sm={12}>
          <Card size="small" bordered style={{ background: '#fafafa' }}>
            <Statistic
              title="Authoritative Odometer"
              value={latestOdo ? Number(latestOdo.value) : '—'}
              precision={1}
              suffix={
                latestOdo && (
                  <Space size={4}>
                    <Typography.Text type="secondary" style={{ fontSize: 13 }}>
                      km
                    </Typography.Text>
                    <Tag style={{ marginLeft: 6 }}>Epoch v{latestOdo.meterEpoch}</Tag>
                  </Space>
                )
              }
            />
            {latestOdo && (
              <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                Last updated: {formatDate(latestOdo.recordedAt)} ({latestOdo.sourceType})
              </Typography.Text>
            )}
          </Card>
        </Col>
        <Col xs={24} sm={12}>
          <Card size="small" bordered style={{ background: '#fafafa' }}>
            <Statistic
              title="Authoritative Engine Hours"
              value={latestEngine ? Number(latestEngine.value) : '—'}
              precision={1}
              suffix={
                latestEngine && (
                  <Space size={4}>
                    <Typography.Text type="secondary" style={{ fontSize: 13 }}>
                      hrs
                    </Typography.Text>
                    <Tag style={{ marginLeft: 6 }}>Epoch v{latestEngine.meterEpoch}</Tag>
                  </Space>
                )
              }
            />
            {latestEngine && (
              <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                Last updated: {formatDate(latestEngine.recordedAt)} ({latestEngine.sourceType})
              </Typography.Text>
            )}
          </Card>
        </Col>
      </Row>

      {/* Tabs: Readings Ledger vs Mileage Summary vs Meter Resets */}
      <Tabs
        activeKey={activeTab}
        onChange={(k) => setActiveTab(k as 'readings' | 'mileage' | 'resets')}
        items={[
          {
            key: 'readings',
            label: (
              <Space>
                <HistoryOutlined />
                <span>Readings Ledger ({readingsQuery.data?.totalElements ?? 0})</span>
              </Space>
            ),
            children: (
              <Flex vertical gap={12}>
                <Flex justify="space-between" align="center" wrap gap={8}>
                  <Space>
                    <Typography.Text type="secondary">Filter by type:</Typography.Text>
                    <Select
                      size="small"
                      allowClear
                      placeholder="All Types"
                      style={{ width: 140 }}
                      value={typeFilter}
                      onChange={(v) => setTypeFilter(v)}
                      options={[
                        { value: 'ODOMETER', label: 'Odometer' },
                        { value: 'ENGINE_HOURS', label: 'Engine Hours' },
                      ]}
                    />
                  </Space>
                  <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                    Append-only ledger with chronology &amp; epoch enforcement
                  </Typography.Text>
                </Flex>

                {readingsQuery.isError && (
                  <Alert type="error" showIcon message="Vehicle readings could not be loaded" />
                )}

                <Table<VehicleReadingResponse>
                  rowKey="id"
                  size="small"
                  columns={readingColumns}
                  dataSource={readingsQuery.data?.content ?? []}
                  loading={readingsQuery.isLoading}
                  pagination={{
                    pageSize: 10,
                    showSizeChanger: true,
                    showTotal: (total) => `${total} readings`,
                  }}
                  scroll={{ x: 750 }}
                  locale={{ emptyText: 'No readings recorded for this vehicle' }}
                />
              </Flex>
            ),
          },
          {
            key: 'mileage',
            label: (
              <Space>
                <BarChartOutlined />
                <span>Period Mileage &amp; Utilization</span>
              </Space>
            ),
            children: (
              <Flex vertical gap={16}>
                {/* Date Controls */}
                <Card size="small" style={{ background: '#fafafa' }}>
                  <Flex justify="space-between" align="center" wrap gap={12}>
                    <Space wrap align="center">
                      <CalendarOutlined />
                      <Typography.Text strong>Period:</Typography.Text>
                      <Input
                        type="date"
                        size="small"
                        style={{ width: 140 }}
                        value={summaryFrom.slice(0, 10)}
                        onChange={(e) => setSummaryFrom(e.target.value ? `${e.target.value}T00:00:00Z` : defaultFrom)}
                      />
                      <span>to</span>
                      <Input
                        type="date"
                        size="small"
                        style={{ width: 140 }}
                        value={summaryTo.slice(0, 10)}
                        onChange={(e) => setSummaryTo(e.target.value ? `${e.target.value}T23:59:59Z` : defaultTo)}
                      />
                      <Button
                        size="small"
                        onClick={() => {
                          setSummaryFrom(defaultFrom);
                          setSummaryTo(defaultTo);
                        }}
                      >
                        Reset (30d)
                      </Button>
                    </Space>
                    <Space>
                      {coverageBadge(summary?.coverageStatus)}
                    </Space>
                  </Flex>
                </Card>

                {/* Coverage Warning if PARTIAL */}
                {summary?.coverageStatus === 'PARTIAL' && (
                  <Alert
                    type="warning"
                    showIcon
                    message="Partial Period Coverage"
                    description={summary.coverageReason || 'Readings do not span the complete requested start and end dates. Distance is computed across available readings within the period.'}
                  />
                )}

                {/* Period Metric Cards */}
                <Row gutter={[16, 16]}>
                  <Col xs={24} sm={12} md={6}>
                    <Card size="small" bordered>
                      <Statistic
                        title="Distance Traveled"
                        value={summary ? Number(summary.distanceKm) : 0}
                        precision={1}
                        suffix="km"
                        valueStyle={{ color: '#1677ff' }}
                      />
                      <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                        Open: {summary?.openingOdometer != null ? `${Number(summary.openingOdometer).toLocaleString()} km` : '—'} | Close: {summary?.closingOdometer != null ? `${Number(summary.closingOdometer).toLocaleString()} km` : '—'}
                      </Typography.Text>
                    </Card>
                  </Col>
                  <Col xs={24} sm={12} md={6}>
                    <Card size="small" bordered>
                      <Statistic
                        title="Engine Hours Used"
                        value={summary ? Number(summary.engineHoursUsed) : 0}
                        precision={1}
                        suffix="hrs"
                        valueStyle={{ color: '#52c41a' }}
                      />
                      <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                        Open: {summary?.openingEngineHours != null ? `${Number(summary.openingEngineHours).toLocaleString()} hrs` : '—'} | Close: {summary?.closingEngineHours != null ? `${Number(summary.closingEngineHours).toLocaleString()} hrs` : '—'}
                      </Typography.Text>
                    </Card>
                  </Col>
                  <Col xs={24} sm={12} md={6}>
                    <Card size="small" bordered>
                      <Statistic
                        title="Readings in Period"
                        value={summary?.readingCount ?? 0}
                        suffix={
                          summary && summary.correctionCount > 0 ? (
                            <Typography.Text type="warning" style={{ fontSize: 13, marginLeft: 4 }}>
                              ({summary.correctionCount} corr.)
                            </Typography.Text>
                          ) : null
                        }
                      />
                      <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                        Authoritative ledger points
                      </Typography.Text>
                    </Card>
                  </Col>
                  <Col xs={24} sm={12} md={6}>
                    <Card size="small" bordered>
                      <Statistic
                        title="Meter Resets"
                        value={summary?.meterResetCount ?? 0}
                        valueStyle={{ color: summary?.meterResetCount ? '#eb2f96' : undefined }}
                      />
                      <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                        Physical replacements
                      </Typography.Text>
                    </Card>
                  </Col>
                </Row>

                {/* Source Breakdown */}
                {summary?.sourceCounts && Object.keys(summary.sourceCounts).length > 0 && (
                  <Card size="small" title="Reading Sources Breakdown" bordered>
                    <Space wrap size={[8, 8]}>
                      {Object.entries(summary.sourceCounts).map(([source, count]) => (
                        <Badge
                          key={source}
                          count={count}
                          overflowCount={999}
                          style={{ backgroundColor: '#108ee9' }}
                        >
                          {sourceTag(source)}
                        </Badge>
                      ))}
                    </Space>
                  </Card>
                )}
              </Flex>
            ),
          },
          {
            key: 'resets',
            label: (
              <Space>
                <SwapOutlined />
                <span>Meter Reset History ({resetsQuery.data?.length ?? 0})</span>
              </Space>
            ),
            children: (
              <Flex vertical gap={12}>
                <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                  Physical meter replacement events that incremented meter epochs.
                </Typography.Text>
                {resetsQuery.isError && (
                  <Alert type="error" showIcon message="Meter reset history could not be loaded" />
                )}
                <Table<MeterResetResponse>
                  rowKey="id"
                  size="small"
                  columns={resetColumns}
                  dataSource={resetsQuery.data ?? []}
                  loading={resetsQuery.isLoading}
                  pagination={{ pageSize: 5 }}
                  locale={{ emptyText: 'No meter replacements recorded for this vehicle' }}
                />
              </Flex>
            ),
          },
        ]}
      />

      {/* Add Manual Reading Modal */}
      <Modal
        title={`Record Manual Reading - ${vehicleRegistration || 'Vehicle'}`}
        open={isAddOpen}
        onCancel={() => {
          setIsAddOpen(false);
          addForm.resetFields();
        }}
        onOk={() => addForm.submit()}
        confirmLoading={recordManual.isPending}
        destroyOnClose
      >
        <Form form={addForm} layout="vertical" onFinish={onAddSubmit}>
          <Form.Item
            name="readingType"
            label="Reading Type"
            rules={[{ required: true, message: 'Please select reading type' }]}
          >
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
            rules={[
              { required: true, message: 'Please input reading value' },
              { type: 'number', min: 0, message: 'Value must be non-negative' },
            ]}
          >
            <InputNumber style={{ width: '100%' }} precision={3} placeholder="e.g. 10250.5" />
          </Form.Item>

          <Form.Item
            name="recordedAt"
            label="Recorded Time"
            rules={[{ required: true, message: 'Please select recorded time' }]}
          >
            <Input type="datetime-local" />
          </Form.Item>

          <Form.Item name="notes" label="Notes (Optional)">
            <Input.TextArea rows={2} placeholder="Optional operational notes" maxLength={500} />
          </Form.Item>
        </Form>
      </Modal>

      {/* Correct Reading Modal */}
      <Modal
        title={
          <Space>
            <EditOutlined />
            <span>Correct Reading #{correctingReading?.id?.substring(0, 8)}</span>
          </Space>
        }
        open={Boolean(correctingReading)}
        onCancel={() => {
          setCorrectingReading(null);
          correctForm.resetFields();
        }}
        onOk={() => correctForm.submit()}
        confirmLoading={correctReading.isPending}
        destroyOnClose
      >
        {correctingReading && (
          <Flex vertical gap={12}>
            <Alert
              type="info"
              showIcon
              message="Correction Behavior"
              description="This reading is immutable. Saving a correction will mark the original reading as CORRECTED and append a new CORRECTION reading in the same meter epoch and recorded timestamp."
            />

            <Descriptions size="small" bordered column={1}>
              <Descriptions.Item label="Reading Type">
                {correctingReading.readingType === 'ODOMETER' ? 'Odometer' : 'Engine Hours'}
              </Descriptions.Item>
              <Descriptions.Item label="Current Value">
                <Typography.Text delete>
                  {Number(correctingReading.value).toLocaleString()}{' '}
                  {correctingReading.unit === 'KILOMETER' ? 'km' : 'hrs'}
                </Typography.Text>
              </Descriptions.Item>
              <Descriptions.Item label="Epoch">
                v{correctingReading.meterEpoch}
              </Descriptions.Item>
              <Descriptions.Item label="Recorded At">
                {formatDate(correctingReading.recordedAt)}
              </Descriptions.Item>
              <Descriptions.Item label="Source">
                {sourceTag(correctingReading.sourceType)}
              </Descriptions.Item>
            </Descriptions>

            <Form form={correctForm} layout="vertical" onFinish={onCorrectSubmit}>
              <Form.Item
                name="value"
                label="Correct Value"
                rules={[
                  { required: true, message: 'Please input correct reading value' },
                  { type: 'number', min: 0, message: 'Value must be non-negative' },
                ]}
              >
                <InputNumber style={{ width: '100%' }} precision={3} placeholder="e.g. 10250.0" />
              </Form.Item>

              <Form.Item
                name="reason"
                label="Correction Reason"
                rules={[{ required: true, message: 'Please explain why this reading is being corrected' }]}
              >
                <Input.TextArea
                  rows={3}
                  placeholder="Mandatory audit explanation (e.g. Typo in manual entry, meter slip inverted)"
                  maxLength={500}
                />
              </Form.Item>

              <Form.Item name="notes" label="Additional Notes (Optional)">
                <Input.TextArea rows={2} placeholder="Optional operational context" maxLength={500} />
              </Form.Item>
            </Form>
          </Flex>
        )}
      </Modal>

      {/* Meter Reset / Replacement Modal */}
      <Modal
        title={
          <Space>
            <SwapOutlined />
            <span>Record Meter Replacement - {vehicleRegistration || 'Vehicle'}</span>
          </Space>
        }
        open={isResetOpen}
        onCancel={() => {
          setIsResetOpen(false);
          resetForm.resetFields();
        }}
        onOk={() => resetForm.submit()}
        confirmLoading={resetMeter.isPending}
        destroyOnClose
      >
        <Flex vertical gap={12}>
          <Alert
            type="warning"
            showIcon
            message="Meter Replacement Policy"
            description="Recording a physical meter reset will increment the vehicle meter epoch (e.g. v0 -> v1). All future readings must advance monotonically from this new initial value."
          />

          <Form form={resetForm} layout="vertical" onFinish={onResetSubmit}>
            <Form.Item
              name="readingType"
              label="Reading Type"
              rules={[{ required: true, message: 'Please select reading type' }]}
            >
              <Select
                options={[
                  { value: 'ODOMETER', label: 'Odometer (km)' },
                  { value: 'ENGINE_HOURS', label: 'Engine Hours (hrs)' },
                ]}
              />
            </Form.Item>

            <Form.Item
              name="newMeterValue"
              label="New Physical Meter Starting Value"
              rules={[
                { required: true, message: 'Please input starting value of new meter' },
                { type: 'number', min: 0, message: 'Value must be non-negative' },
              ]}
              extra="Typically 0.000 for brand new hardware or current value if pre-calibrated"
            >
              <InputNumber style={{ width: '100%' }} precision={3} placeholder="0.000" />
            </Form.Item>

            <Form.Item
              name="effectiveAt"
              label="Replacement Effective Time"
              rules={[{ required: true, message: 'Please select replacement timestamp' }]}
            >
              <Input type="datetime-local" />
            </Form.Item>

            <Form.Item
              name="reason"
              label="Replacement Reason"
              rules={[{ required: true, message: 'Please provide reason for meter replacement' }]}
            >
              <Input.TextArea
                rows={3}
                placeholder="e.g. Cluster replaced after hardware failure under warranty"
                maxLength={500}
              />
            </Form.Item>

            <Form.Item name="notes" label="Work Order / Notes (Optional)">
              <Input.TextArea rows={2} placeholder="Optional work order or technician reference" maxLength={500} />
            </Form.Item>
          </Form>
        </Flex>
      </Modal>
    </Card>
  );
}
