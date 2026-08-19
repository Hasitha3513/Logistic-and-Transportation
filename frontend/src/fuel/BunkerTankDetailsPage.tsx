import { useState } from 'react';
import { useParams, Link, Navigate } from 'react-router-dom';
import {
  Alert,
  Breadcrumb,
  Button,
  Card,
  Col,
  Descriptions,
  Divider,
  Empty,
  Progress,
  Row,
  Space,
  Spin,
  Table,
  Tabs,
  Tag,
  Typography,
} from 'antd';
import {
  ArrowLeftOutlined,
  ExperimentOutlined,
  ControlOutlined,
  SwapOutlined,
  HistoryOutlined,
} from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import { useAuth } from '../auth/AuthContext';
import {
  useBunkerBalance,
  useBunkerDipReadings,
  useBunkerMovements,
  useBunkerTank,
  useBunkerTanks,
} from './hooks/useBunkerTanks';
import { useFuelStations } from './hooks/useFuelIssues';
import type { BunkerStockMovement, DipReading } from './bunkerTypes';
import {
  BunkerMovementTypeTag,
  BunkerStockStatusTag,
  BunkerTankStatusTag,
} from '../components/status/StatusTags';
import { RecordDipModal, StockAdjustmentModal, TransferModal } from './BunkerTankListPage';

const { Title, Text, Paragraph } = Typography;

export default function BunkerTankDetailsPage() {
  const { bunkerTankId } = useParams<{ bunkerTankId: string }>();
  const { hasPermission } = useAuth();
  const [movementPage, setMovementPage] = useState(0);
  const [movementLimit] = useState(15);

  // Modals state
  const [dipModalOpen, setDipModalOpen] = useState(false);
  const [adjustModalOpen, setAdjustModalOpen] = useState(false);
  const [transferModalOpen, setTransferModalOpen] = useState(false);

  const { data: tank, isLoading: tankLoading } = useBunkerTank(bunkerTankId);
  const { data: balance, isLoading: balanceLoading } = useBunkerBalance(bunkerTankId);
  const { data: movementsData, isLoading: movementsLoading } = useBunkerMovements(
    bunkerTankId,
    movementPage,
    movementLimit
  );
  const { data: dipReadings = [], isLoading: dipsLoading } = useBunkerDipReadings(bunkerTankId);
  const { data: stations = [] } = useFuelStations();
  const { data: allTanks = [] } = useBunkerTanks();

  if (!hasPermission('BUNKER_VIEW')) return <Navigate to="/workspace" replace />;

  const canDip = hasPermission('BUNKER_DIP_RECORD');
  const canAdjust = hasPermission('BUNKER_ADJUST');
  const canTransfer = hasPermission('BUNKER_TRANSFER');
  const canViewLedger = hasPermission('BUNKER_LEDGER_VIEW');

  if (tankLoading || balanceLoading) {
    return (
      <div style={{ padding: 48, textAlign: 'center' }}>
        <Spin size="large" />
      </div>
    );
  }

  if (!tank) {
    return (
      <div style={{ padding: 24 }}>
        <Empty description="Bunker tank not found">
          <Link to="/fuel/bunker-tanks">
            <Button type="primary">Back to Tanks</Button>
          </Link>
        </Empty>
      </div>
    );
  }

  const station = stations.find((s) => s.id === tank.fuelStationId);
  const stockLiters = balance ? balance.currentStockLiters : tank.currentStockLiters;
  const capacityLiters = balance ? balance.capacityLiters : tank.capacityLiters;
  const availableLiters = balance ? balance.availableCapacityLiters : tank.availableCapacityLiters;
  const utilizationPct =
    capacityLiters > 0 ? Math.min(100, Math.round((stockLiters / capacityLiters) * 100)) : 0;

  const movementColumns: ColumnsType<BunkerStockMovement> = [
    {
      title: 'Date & Time',
      dataIndex: 'occurredAt',
      key: 'occurredAt',
      width: 170,
      render: (val: string) => (val ? new Date(val).toLocaleString() : '-'),
    },
    {
      title: 'Type',
      dataIndex: 'movementType',
      key: 'movementType',
      width: 160,
      render: (val: string) => <BunkerMovementTypeTag type={val} />,
    },
    {
      title: 'Quantity',
      dataIndex: 'quantityLiters',
      key: 'quantityLiters',
      align: 'right',
      width: 130,
      render: (val: number, record) => {
        const isOut = record.movementType.includes('OUT') || record.movementType === 'FUEL_ISSUE';
        return (
          <Text strong style={{ color: isOut ? '#cf1322' : '#389e0d' }}>
            {isOut ? '-' : '+'}
            {Number(val).toLocaleString(undefined, { minimumFractionDigits: 1 })} L
          </Text>
        );
      },
    },
    {
      title: 'Resulting Balance',
      dataIndex: 'resultingBalanceLiters',
      key: 'resultingBalanceLiters',
      align: 'right',
      width: 150,
      render: (val: number) => (
        <Text strong style={{ color: '#1677ff' }}>
          {Number(val).toLocaleString(undefined, { minimumFractionDigits: 1 })} L
        </Text>
      ),
    },
    {
      title: 'Reference',
      key: 'reference',
      width: 150,
      render: (_, record) => (
        <Space direction="vertical" size={0}>
          <Tag>{record.referenceType}</Tag>
        </Space>
      ),
    },
    {
      title: 'Reason / Description',
      dataIndex: 'reason',
      key: 'reason',
      ellipsis: true,
      render: (val: string) => val || '-',
    },
  ];

  const dipColumns: ColumnsType<DipReading> = [
    {
      title: 'Measured Date/Time',
      dataIndex: 'measuredAt',
      key: 'measuredAt',
      width: 180,
      render: (val: string) => (val ? new Date(val).toLocaleString() : '-'),
    },
    {
      title: 'Physical Dip',
      dataIndex: 'physicalQuantityLiters',
      key: 'physicalQuantityLiters',
      align: 'right',
      render: (val: number) => (
        <Text strong>{Number(val).toLocaleString(undefined, { minimumFractionDigits: 1 })} L</Text>
      ),
    },
    {
      title: 'Book Stock at Measurement',
      dataIndex: 'bookQuantityAtMeasurement',
      key: 'bookQuantityAtMeasurement',
      align: 'right',
      render: (val: number) => `${Number(val).toLocaleString(undefined, { minimumFractionDigits: 1 })} L`,
    },
    {
      title: 'Variance',
      dataIndex: 'varianceQuantityLiters',
      key: 'varianceQuantityLiters',
      align: 'right',
      render: (val: number) => {
        const num = Number(val);
        const color = num === 0 ? '#52c41a' : num > 0 ? '#1890ff' : '#cf1322';
        return (
          <Text strong style={{ color }}>
            {num > 0 ? '+' : ''}
            {num.toLocaleString(undefined, { minimumFractionDigits: 1 })} L
          </Text>
        );
      },
    },
    {
      title: 'Notes',
      dataIndex: 'notes',
      key: 'notes',
      render: (val: string) => val || '-',
    },
  ];

  return (
    <div style={{ padding: 24 }}>
      <Breadcrumb
        style={{ marginBottom: 16 }}
        items={[
          { title: <Link to="/">Home</Link> },
          { title: <Link to="/fuel/bunker-tanks">Bunker Tanks</Link> },
          { title: tank.tankCode },
        ]}
      />

      <Row justify="space-between" align="middle" style={{ marginBottom: 20 }}>
        <Col>
          <Space align="center" size="middle">
            <Link to="/fuel/bunker-tanks">
              <Button icon={<ArrowLeftOutlined />} />
            </Link>
            <div>
              <Space>
                <Title level={3} style={{ margin: 0 }}>
                  {tank.tankCode}
                </Title>
                <BunkerTankStatusTag status={tank.status} />
                <BunkerStockStatusTag status={balance?.stockStatus || (tank.lowStock ? 'LOW_STOCK' : 'NORMAL')} />
              </Space>
              <Text type="secondary" style={{ display: 'block' }}>
                {tank.tankName} â€¢ {station?.name || 'Internal Station'} ({tank.fuelType})
              </Text>
            </div>
          </Space>
        </Col>
        <Col>
          <Space>
            {canDip && (
              <Button
                icon={<ExperimentOutlined />}
                onClick={() => setDipModalOpen(true)}
              >
                Record Physical Dip
              </Button>
            )}
            {canAdjust && (
              <Button
                icon={<ControlOutlined />}
                onClick={() => setAdjustModalOpen(true)}
              >
                Stock Adjustment
              </Button>
            )}
            {canTransfer && (
              <Button
                icon={<SwapOutlined />}
                onClick={() => setTransferModalOpen(true)}
              >
                Transfer Fuel
              </Button>
            )}
          </Space>
        </Col>
      </Row>

      {/* Balance & Variance Cards */}
      <Row gutter={16} style={{ marginBottom: 20 }}>
        <Col xs={24} md={12}>
          <Card title="Authoritative Book Inventory & Capacity" style={{ height: '100%' }}>
            <Row gutter={16} align="middle">
              <Col span={12}>
                <Text type="secondary">Current Book Stock</Text>
                <Title level={2} style={{ margin: '4px 0', color: '#1677ff' }}>
                  {stockLiters.toLocaleString(undefined, { minimumFractionDigits: 1 })} L
                </Title>
                <Text type="secondary">
                  Capacity: {capacityLiters.toLocaleString()} L
                </Text>
              </Col>
              <Col span={12} style={{ textAlign: 'center' }}>
                <Progress
                  type="circle"
                  percent={utilizationPct}
                  size={100}
                  strokeColor={tank.lowStock ? '#fa8c16' : '#52c41a'}
                />
              </Col>
            </Row>
            <Divider style={{ margin: '16px 0' }} />
            <Row justify="space-between">
              <Col>
                <Text type="secondary">Available Ullage (Free Space): </Text>
                <Text strong>{availableLiters.toLocaleString()} L</Text>
              </Col>
              <Col>
                <Text type="secondary">Reorder Threshold: </Text>
                <Text strong>{tank.minimumStockLiters.toLocaleString()} L</Text>
              </Col>
            </Row>
          </Card>
        </Col>

        <Col xs={24} md={12}>
          <Card
            title="Physical Dip Observation & Variance"
            style={{ height: '100%' }}
            extra={<Text type="secondary">Measurement Observation</Text>}
          >
            {balance?.latestDipQuantityLiters !== null && balance?.latestDipQuantityLiters !== undefined ? (
              <div>
                <Row gutter={16}>
                  <Col span={12}>
                    <Text type="secondary">Latest Physical Dip</Text>
                    <Title level={2} style={{ margin: '4px 0', color: '#722ed1' }}>
                      {Number(balance.latestDipQuantityLiters).toLocaleString(undefined, { minimumFractionDigits: 1 })} L
                    </Title>
                    <Text type="secondary">
                      Measured: {balance.latestDipAt ? new Date(balance.latestDipAt).toLocaleString() : '-'}
                    </Text>
                  </Col>
                  <Col span={12}>
                    <Text type="secondary">Observed Variance</Text>
                    <Title
                      level={2}
                      style={{
                        margin: '4px 0',
                        color:
                          Number(balance.latestVarianceLiters) === 0
                            ? '#52c41a'
                            : Number(balance.latestVarianceLiters) > 0
                            ? '#1890ff'
                            : '#cf1322',
                      }}
                    >
                      {Number(balance.latestVarianceLiters) > 0 ? '+' : ''}
                      {Number(balance.latestVarianceLiters).toLocaleString(undefined, { minimumFractionDigits: 1 })} L
                    </Title>
                    <Text type="secondary">
                      {Number(balance.latestVarianceLiters) === 0
                        ? 'Book stock matches dip exactly'
                        : 'Variance noted (not automatically adjusted)'}
                    </Text>
                  </Col>
                </Row>
              </div>
            ) : (
              <div style={{ padding: '24px 0', textAlign: 'center' }}>
                <Text type="secondary" style={{ fontSize: 16 }}>
                  No physical dip recorded
                </Text>
                <Paragraph type="secondary" style={{ marginTop: 8 }}>
                  Perform a physical tank dip measurement and record it to track inventory variance.
                </Paragraph>
              </div>
            )}
            <Divider style={{ margin: '16px 0' }} />
            <Text type="secondary" style={{ fontSize: 12 }}>
              Note: Physical dip readings are observations only. Reconcile variances through the "Stock Adjustment" action when needed.
            </Text>
          </Card>
        </Col>
      </Row>

      {/* Tabs */}
      <Card>
        <Tabs
          defaultActiveKey="ledger"
          items={[
            {
              key: 'ledger',
              label: (
                <span>
                  <HistoryOutlined /> Stock Movement Ledger
                </span>
              ),
              children: (
                <div>
                  {canViewLedger ? (
                    <Table
                      rowKey="id"
                      dataSource={movementsData?.items || []}
                      columns={movementColumns}
                      loading={movementsLoading}
                      pagination={{
                        current: movementPage + 1,
                        pageSize: movementLimit,
                        total: movementsData?.totalElements || 0,
                        onChange: (page) => setMovementPage(page - 1),
                        showTotal: (total) => `Total ${total} ledger entries`,
                      }}
                      locale={{
                        emptyText: 'No ledger movements recorded for this tank yet.',
                      }}
                    />
                  ) : (
                    <Alert
                      type="warning"
                      message="Permission Required"
                      description="You do not have the BUNKER_LEDGER_VIEW permission to inspect the stock ledger."
                    />
                  )}
                </div>
              ),
            },
            {
              key: 'dips',
              label: (
                <span>
                  <ExperimentOutlined /> Dip Reading History
                </span>
              ),
              children: (
                <Table
                  rowKey="id"
                  dataSource={dipReadings}
                  columns={dipColumns}
                  loading={dipsLoading}
                  pagination={{ pageSize: 10, showTotal: (total) => `Total ${total} dip observations` }}
                  locale={{
                    emptyText: 'No physical dip readings recorded yet.',
                  }}
                />
              ),
            },
            {
              key: 'details',
              label: 'Configuration & Properties',
              children: (
                <Descriptions bordered column={{ xs: 1, sm: 2, md: 3 }}>
                  <Descriptions.Item label="Tank Code">{tank.tankCode}</Descriptions.Item>
                  <Descriptions.Item label="Tank Name">{tank.tankName}</Descriptions.Item>
                  <Descriptions.Item label="Fuel Type">{tank.fuelType}</Descriptions.Item>
                  <Descriptions.Item label="Operating Station">{station?.name || tank.fuelStationId}</Descriptions.Item>
                  <Descriptions.Item label="Capacity">{tank.capacityLiters.toLocaleString()} L</Descriptions.Item>
                  <Descriptions.Item label="Reorder Level">{tank.minimumStockLiters.toLocaleString()} L</Descriptions.Item>
                  <Descriptions.Item label="Status">
                    <BunkerTankStatusTag status={tank.status} />
                  </Descriptions.Item>
                  <Descriptions.Item label="Active">{tank.active ? 'Yes' : 'No'}</Descriptions.Item>
                  <Descriptions.Item label="Created At">
                    {tank.createdAt ? new Date(tank.createdAt).toLocaleString() : '-'}
                  </Descriptions.Item>
                </Descriptions>
              ),
            },
          ]}
        />
      </Card>

      {/* Modals */}
      <RecordDipModal
        tank={tank}
        open={dipModalOpen}
        onClose={() => setDipModalOpen(false)}
      />
      <StockAdjustmentModal
        tank={tank}
        open={adjustModalOpen}
        onClose={() => setAdjustModalOpen(false)}
      />
      <TransferModal
        tanks={allTanks}
        open={transferModalOpen}
        onClose={() => setTransferModalOpen(false)}
      />
    </div>
  );
}