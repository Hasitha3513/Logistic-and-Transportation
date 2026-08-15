import { useQuery } from '@tanstack/react-query';
import {
  Alert,
  Badge,
  Card,
  Col,
  Progress,
  Row,
  Skeleton,
  Space,
  Statistic,
  Table,
  Typography,
  type TableColumnsType,
} from 'antd';
import { api } from '../api/client';

const { Paragraph, Text, Title } = Typography;

interface DashboardAlert {
  id: string;
  title: string;
  detail?: string | null;
  severity?: string | null;
  dueDate?: string | null;
}

interface OperationsDashboard {
  date: string;
  status: string;
  vehicles?: {
    available?: number;
    allocated?: number;
    maintenance?: number;
    outOfService?: number;
    availabilityPercent?: number;
  };
  drivers?: {
    available?: number;
    assigned?: number;
    availabilityPercent?: number;
  };
  trips?: {
    draft?: number;
    pendingApproval?: number;
    approved?: number;
    assigned?: number;
    dispatched?: number;
    inProgress?: number;
    completed?: number;
    completionPercent?: number;
  };
  alerts?: {
    expiringDocuments?: DashboardAlert[];
    criticalExceptions?: DashboardAlert[];
  };
}

interface MetricDefinition {
  key: string;
  label: string;
  value?: number;
}

function MetricValue({ metric }: { metric: MetricDefinition }) {
  return metric.value == null
    ? <Space direction="vertical" size={2}><Text strong>{metric.label}</Text><Text type="secondary">Not supplied by reporting API</Text></Space>
    : <Statistic title={metric.label} value={metric.value} />;
}

function MetricGroup({ title, metrics, progress, progressLabel }: {
  title: string;
  metrics: MetricDefinition[];
  progress?: number;
  progressLabel: string;
}) {
  return (
    <Card title={title} variant="borderless" className="dashboard-section-card">
      <Row gutter={[16, 20]}>
        {metrics.map((metric) => <Col xs={24} sm={12} key={metric.key}><MetricValue metric={metric} /></Col>)}
      </Row>
      <div className="dashboard-progress">
        <Text type="secondary">{progressLabel}</Text>
        {progress == null
          ? <Text type="secondary">Not supplied by reporting API</Text>
          : <Progress percent={progress} status="active" />}
      </div>
    </Card>
  );
}

const alertColumns: TableColumnsType<DashboardAlert> = [
  { title: 'Alert', dataIndex: 'title', key: 'title', render: (value: string) => <Text strong>{value}</Text> },
  { title: 'Details', dataIndex: 'detail', key: 'detail', responsive: ['md'], render: (value?: string) => value || '—' },
  { title: 'Severity', dataIndex: 'severity', key: 'severity', width: 120, render: (value?: string) => value ? <Badge status={value.toUpperCase() === 'CRITICAL' ? 'error' : 'warning'} text={value.replaceAll('_', ' ')} /> : '—' },
  { title: 'Due', dataIndex: 'dueDate', key: 'dueDate', width: 130, responsive: ['lg'], render: (value?: string) => value || '—' },
];

function AlertTable({ title, data }: { title: string; data?: DashboardAlert[] }) {
  return (
    <Card title={title} variant="borderless" className="dashboard-alert-card">
      <Table<DashboardAlert>
        rowKey="id"
        columns={alertColumns}
        dataSource={data ?? []}
        pagination={false}
        size="small"
        locale={{ emptyText: data ? 'No alerts returned' : 'Not supplied by reporting API' }}
      />
    </Card>
  );
}

export default function DashboardPage() {
  const dashboard = useQuery({
    queryKey: ['dashboard', 'operations'],
    queryFn: async () => (await api.get<OperationsDashboard>('/dashboard/operations')).data,
  });

  if (dashboard.isLoading) return <Card variant="borderless" className="dashboard-loading"><Skeleton active paragraph={{ rows: 8 }} /></Card>;
  if (dashboard.isError || !dashboard.data) return <Alert type="error" showIcon message="Operations dashboard could not be loaded" description="The reporting service did not return dashboard data." />;

  const data = dashboard.data;
  const vehicles: MetricDefinition[] = [
    { key: 'available', label: 'Available', value: data.vehicles?.available },
    { key: 'allocated', label: 'Allocated', value: data.vehicles?.allocated },
    { key: 'maintenance', label: 'Maintenance', value: data.vehicles?.maintenance },
    { key: 'outOfService', label: 'Out of service', value: data.vehicles?.outOfService },
  ];
  const drivers: MetricDefinition[] = [
    { key: 'available', label: 'Available', value: data.drivers?.available },
    { key: 'assigned', label: 'Assigned', value: data.drivers?.assigned },
  ];
  const trips: MetricDefinition[] = [
    { key: 'draft', label: 'Draft', value: data.trips?.draft },
    { key: 'pendingApproval', label: 'Pending approval', value: data.trips?.pendingApproval },
    { key: 'approved', label: 'Approved', value: data.trips?.approved },
    { key: 'assigned', label: 'Assigned', value: data.trips?.assigned },
    { key: 'dispatched', label: 'Dispatched', value: data.trips?.dispatched },
    { key: 'inProgress', label: 'In progress', value: data.trips?.inProgress },
    { key: 'completed', label: 'Completed', value: data.trips?.completed },
  ];

  return (
    <Space direction="vertical" size={24} className="operations-dashboard">
      <div>
        <Title level={3}>Operations Overview</Title>
        <Paragraph type="secondary">Fleet, driver, trip, and operational alert metrics supplied by the reporting module.</Paragraph>
      </div>

      <Row gutter={[18, 18]}>
        <Col xs={24} md={12}>
          <Card variant="borderless" className="metric-card dashboard-overview-card">
            <Statistic title="Reporting date" value={data.date} />
          </Card>
        </Col>
        <Col xs={24} md={12}>
          <Card variant="borderless" className="metric-card dashboard-overview-card">
            <Text type="secondary">Reporting API status</Text>
            <div className="dashboard-api-status"><Badge status={data.status === 'READY' ? 'success' : 'warning'} text={data.status} /></div>
          </Card>
        </Col>
      </Row>

      <Row gutter={[18, 18]} align="stretch">
        <Col xs={24} xl={8}>
          <MetricGroup title="Vehicles" metrics={vehicles} progress={data.vehicles?.availabilityPercent} progressLabel="Availability" />
        </Col>
        <Col xs={24} xl={8}>
          <MetricGroup title="Drivers" metrics={drivers} progress={data.drivers?.availabilityPercent} progressLabel="Availability" />
        </Col>
        <Col xs={24} xl={8}>
          <MetricGroup title="Trips" metrics={trips} progress={data.trips?.completionPercent} progressLabel="Completion" />
        </Col>
      </Row>

      <div>
        <Title level={4}>Alerts</Title>
        <Row gutter={[18, 18]}>
          <Col xs={24} xl={12}><AlertTable title="Expiring Documents" data={data.alerts?.expiringDocuments} /></Col>
          <Col xs={24} xl={12}><AlertTable title="Critical Exceptions" data={data.alerts?.criticalExceptions} /></Col>
        </Row>
      </div>
    </Space>
  );
}
