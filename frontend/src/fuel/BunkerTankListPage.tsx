import { useState } from 'react';
import { Link, Navigate } from 'react-router-dom';
import {
  Alert,
  App as AntApp,
  Button,
  Card,
  Col,
  Flex,
  Form,
  Input,
  InputNumber,
  Modal,
  Progress,
  Radio,
  Row,
  Select,
  Space,
  Table,
  Typography,
  Tooltip,
} from 'antd';
import {
  PlusOutlined,
  SwapOutlined,
  EyeOutlined,
  EditOutlined,
  ExperimentOutlined,
  ControlOutlined,
  ReloadOutlined,
} from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import { isAxiosError } from 'axios';
import { useAuth } from '../auth/AuthContext';
import {
  useAdjustBunkerStock,
  useBunkerTanks,
  useCreateBunkerTank,
  useRecordDipReading,
  useTransferBunkerStock,
  useUpdateBunkerTank,
} from './hooks/useBunkerTanks';
import { useFuelStations } from './hooks/useFuelIssues';
import type {
  BunkerTank,
  BunkerTankCreatePayload,
  BunkerTankStatus,
  BunkerTankUpdatePayload,
} from './bunkerTypes';
import { BunkerStockStatusTag, BunkerTankStatusTag } from '../components/status/StatusTags';

const { Text } = Typography;

export default function BunkerTankListPage() {
  const { hasPermission } = useAuth();
  const [stationFilter, setStationFilter] = useState<string | undefined>();
  const [fuelTypeFilter, setFuelTypeFilter] = useState<string | undefined>();
  const [activeFilter, setActiveFilter] = useState<boolean | undefined>(true);

  // Modals state
  const [createModalOpen, setCreateModalOpen] = useState(false);
  const [transferModalOpen, setTransferModalOpen] = useState(false);
  const [editTank, setEditTank] = useState<BunkerTank | null>(null);
  const [dipTank, setDipTank] = useState<BunkerTank | null>(null);
  const [adjustTank, setAdjustTank] = useState<BunkerTank | null>(null);

  const { data: stations = [] } = useFuelStations();
  const {
    data: tanks = [],
    isLoading,
    refetch,
    isFetching,
  } = useBunkerTanks({
    fuelStationId: stationFilter,
    fuelType: fuelTypeFilter,
    active: activeFilter,
  });

  if (!hasPermission('BUNKER_VIEW')) return <Navigate to="/workspace" replace />;

  const canCreate = hasPermission('BUNKER_CREATE');
  const canUpdate = hasPermission('BUNKER_UPDATE');
  const canDip = hasPermission('BUNKER_DIP_RECORD');
  const canAdjust = hasPermission('BUNKER_ADJUST');
  const canTransfer = hasPermission('BUNKER_TRANSFER');

  const stationMap = new Map(stations.map((s) => [s.id, s.name || s.code]));

  const columns: ColumnsType<BunkerTank> = [
    {
      title: 'Tank Code',
      dataIndex: 'tankCode',
      key: 'tankCode',
      render: (code: string, record) => (
        <Link to={`/fuel/bunker-tanks/${record.id}`} style={{ fontWeight: 600 }}>
          {code}
        </Link>
      ),
    },
    {
      title: 'Tank Name',
      dataIndex: 'tankName',
      key: 'tankName',
    },
    {
      title: 'Station',
      dataIndex: 'fuelStationId',
      key: 'fuelStationId',
      render: (stnId: string) => stationMap.get(stnId) || stnId,
    },
    {
      title: 'Fuel Type',
      dataIndex: 'fuelType',
      key: 'fuelType',
      render: (ft: string) => <Text strong>{ft}</Text>,
    },
    {
      title: 'Capacity',
      dataIndex: 'capacityLiters',
      key: 'capacityLiters',
      align: 'right',
      render: (val: number) => `${Number(val).toLocaleString(undefined, { minimumFractionDigits: 1 })} L`,
    },
    {
      title: 'Current Stock',
      dataIndex: 'currentStockLiters',
      key: 'currentStockLiters',
      align: 'right',
      render: (val: number) => (
        <Text strong style={{ color: '#1677ff' }}>
          {Number(val).toLocaleString(undefined, { minimumFractionDigits: 1 })} L
        </Text>
      ),
    },
    {
      title: 'Available Capacity',
      dataIndex: 'availableCapacityLiters',
      key: 'availableCapacityLiters',
      align: 'right',
      render: (val: number) => `${Number(val).toLocaleString(undefined, { minimumFractionDigits: 1 })} L`,
    },
    {
      title: 'Utilization',
      key: 'utilization',
      width: 140,
      render: (_, record) => {
        const pct = record.capacityLiters > 0
          ? Math.min(100, Math.round((record.currentStockLiters / record.capacityLiters) * 100))
          : 0;
        let strokeColor = '#52c41a';
        if (record.lowStock) strokeColor = '#fa8c16';
        if (pct >= 95) strokeColor = '#1890ff';
        if (!record.active || record.status !== 'ACTIVE') strokeColor = '#ff4d4f';
        return <Progress percent={pct} size="small" strokeColor={strokeColor} />;
      },
    },
    {
      title: 'Stock State',
      key: 'stockState',
      render: (_, record) => {
        let status = 'NORMAL';
        if (!record.active || record.status !== 'ACTIVE') status = 'OUT_OF_SERVICE';
        else if (record.lowStock) status = 'LOW_STOCK';
        else if (record.capacityLiters > 0 && record.currentStockLiters >= record.capacityLiters * 0.95)
          status = 'NEAR_CAPACITY';
        return <BunkerStockStatusTag status={status} />;
      },
    },
    {
      title: 'Status',
      dataIndex: 'status',
      key: 'status',
      render: (st: string) => <BunkerTankStatusTag status={st} />,
    },
    {
      title: 'Actions',
      key: 'actions',
      render: (_, record) => (
        <Space size="small">
          <Tooltip title="View Tank & Ledger">
            <Link to={`/fuel/bunker-tanks/${record.id}`}>
              <Button size="small" icon={<EyeOutlined />} aria-label="View Tank" />
            </Link>
          </Tooltip>
          {canDip && (
            <Tooltip title="Record Physical Dip">
              <Button
                size="small"
                icon={<ExperimentOutlined />}
                aria-label="Record Dip"
                onClick={() => setDipTank(record)}
              />
            </Tooltip>
          )}
          {canAdjust && (
            <Tooltip title="Adjust Stock">
              <Button
                size="small"
                icon={<ControlOutlined />}
                aria-label="Adjust Stock"
                onClick={() => setAdjustTank(record)}
              />
            </Tooltip>
          )}
          {canUpdate && (
            <Tooltip title="Edit Tank">
              <Button
                size="small"
                icon={<EditOutlined />}
                aria-label="Edit Tank"
                onClick={() => setEditTank(record)}
              />
            </Tooltip>
          )}
        </Space>
      ),
    },
  ];

  return (
    <>
      <Row justify="space-between" align="middle" style={{ marginBottom: 16 }}>
        <Col>
          <Text type="secondary">
            Manage bulk internal depot fuel storage, dip measurements, stock adjustments, and transfers.
          </Text>
        </Col>
        <Col>
          <Space>
            <Button icon={<ReloadOutlined spin={isFetching} />} onClick={() => refetch()}>
              Refresh
            </Button>
            {canTransfer && (
              <Button icon={<SwapOutlined />} onClick={() => setTransferModalOpen(true)}>
                Inter-Tank Transfer
              </Button>
            )}
            {canCreate && (
              <Button type="primary" icon={<PlusOutlined />} onClick={() => setCreateModalOpen(true)}>
                New Tank
              </Button>
            )}
          </Space>
        </Col>
      </Row>

      <Card variant="borderless" style={{ marginBottom: 16 }}>
        <Flex wrap gap={12} align="center">
          <Select
            allowClear
            placeholder="All Stations"
            aria-label="Station filter"
            style={{ minWidth: 220, flex: '1 1 200px', maxWidth: 320 }}
            value={stationFilter}
            onChange={(val) => setStationFilter(val)}
            options={stations.map((s) => ({ label: `${s.name} (${s.code})`, value: s.id }))}
          />
          <Select
            allowClear
            placeholder="All Fuel Types"
            aria-label="Fuel type filter"
            style={{ minWidth: 160, flex: '1 1 150px', maxWidth: 220 }}
            value={fuelTypeFilter}
            onChange={(val) => setFuelTypeFilter(val)}
            options={[
              { label: 'DIESEL', value: 'DIESEL' },
              { label: 'PETROL', value: 'PETROL' },
              { label: 'OCTANE_95', value: 'OCTANE_95' },
              { label: 'SUPER_DIESEL', value: 'SUPER_DIESEL' },
            ]}
          />
          <Select
            aria-label="Active status filter"
            style={{ minWidth: 180, flex: '1 1 160px', maxWidth: 220 }}
            value={activeFilter}
            onChange={(val) => setActiveFilter(val)}
            options={[
              { label: 'Active Tanks Only', value: true },
              { label: 'Inactive / All', value: undefined },
            ]}
          />
        </Flex>
      </Card>

      <Card className="resource-list-card">
        <Table
          rowKey="id"
          dataSource={tanks}
          columns={columns}
          loading={isLoading}
          scroll={{ x: 'max-content' }}
          pagination={{ pageSize: 15, showSizeChanger: true, showTotal: (total) => `Total ${total} bunker tanks` }}
          locale={{
            emptyText: 'No bunker tanks configured. Click "New Tank" to register a fuel tank.',
          }}
        />
      </Card>

      {/* Modals */}
      <CreateTankModal
        open={createModalOpen}
        onClose={() => setCreateModalOpen(false)}
        stations={stations}
      />
      {editTank && (
        <EditTankModal
          tank={editTank}
          open={Boolean(editTank)}
          onClose={() => setEditTank(null)}
        />
      )}
      {dipTank && (
        <RecordDipModal
          tank={dipTank}
          open={Boolean(dipTank)}
          onClose={() => setDipTank(null)}
        />
      )}
      {adjustTank && (
        <StockAdjustmentModal
          tank={adjustTank}
          open={Boolean(adjustTank)}
          onClose={() => setAdjustTank(null)}
        />
      )}
      <TransferModal
        tanks={tanks}
        open={transferModalOpen}
        onClose={() => setTransferModalOpen(false)}
      />
    </>
  );
}

// -------------------------------------------------------------
// Sub-Modals
// -------------------------------------------------------------

function CreateTankModal({
  open,
  onClose,
  stations,
}: {
  open: boolean;
  onClose: () => void;
  stations: Array<{ id: string; name: string; code: string; stationType: string }>;
}) {
  const [form] = Form.useForm();
  const { message } = AntApp.useApp();
  const [errorMsg, setErrorMsg] = useState<string | null>(null);
  const createMutation = useCreateBunkerTank();

  const handleFinish = async (values: {
    fuelStationId: string;
    tankCode: string;
    tankName: string;
    fuelType: string;
    capacityLiters: number;
    minimumStockLiters?: number;
    openingBalanceLiters?: number;
    commissionedAt?: string;
  }) => {
    setErrorMsg(null);
    try {
      const payload: BunkerTankCreatePayload = {
        fuelStationId: values.fuelStationId,
        tankCode: values.tankCode.trim().toUpperCase(),
        tankName: values.tankName.trim(),
        fuelType: values.fuelType.trim().toUpperCase(),
        capacityLiters: Number(values.capacityLiters),
        minimumStockLiters: values.minimumStockLiters ? Number(values.minimumStockLiters) : undefined,
        openingBalanceLiters: values.openingBalanceLiters ? Number(values.openingBalanceLiters) : undefined,
        commissionedAt: values.commissionedAt ? new Date(values.commissionedAt).toISOString() : undefined,
      };
      await createMutation.mutateAsync(payload);
      message.success(`Bunker tank ${payload.tankCode} created successfully`);
      form.resetFields();
      onClose();
    } catch (err: unknown) {
      const msg = isAxiosError(err)
        ? (err.response?.data as { message?: string })?.message || err.message
        : err instanceof Error
        ? err.message
        : 'Failed to create bunker tank';
      setErrorMsg(msg);
      message.error(msg);
    }
  };

  const internalStations = stations.filter((s) => s.stationType === 'INTERNAL');

  return (
    <Modal
      title="Create New Bunker Tank"
      open={open}
      onCancel={() => {
        setErrorMsg(null);
        onClose();
      }}
      onOk={() => form.submit()}
      confirmLoading={createMutation.isPending}
      destroyOnHidden
    >
      {errorMsg && <Alert type="error" showIcon message={errorMsg} style={{ marginBottom: 16 }} />}
      <Form form={form} layout="vertical" onFinish={handleFinish}>
        <Form.Item
          name="fuelStationId"
          label="Internal Fuel Station"
          rules={[{ required: true, message: 'Please select a station' }]}
        >
          <Select
            aria-label="Internal Fuel Station"
            placeholder="Select internal station"
            options={internalStations.map((s) => ({ label: `${s.name} (${s.code})`, value: s.id }))}
          />
        </Form.Item>

        <Row gutter={16}>
          <Col span={12}>
            <Form.Item
              name="tankCode"
              label="Tank Code"
              rules={[{ required: true, message: 'Required (e.g. BNK-DSL-01)' }]}
            >
              <Input placeholder="BNK-DSL-01" />
            </Form.Item>
          </Col>
          <Col span={12}>
            <Form.Item
              name="fuelType"
              label="Fuel Type"
              rules={[{ required: true, message: 'Select fuel type' }]}
            >
              <Select
                aria-label="Fuel Type"
                placeholder="Select fuel type"
                options={[
                  { label: 'DIESEL', value: 'DIESEL' },
                  { label: 'PETROL', value: 'PETROL' },
                  { label: 'OCTANE_95', value: 'OCTANE_95' },
                  { label: 'SUPER_DIESEL', value: 'SUPER_DIESEL' },
                ]}
              />
            </Form.Item>
          </Col>
        </Row>

        <Form.Item
          name="tankName"
          label="Tank Name / Description"
          rules={[{ required: true, message: 'Required' }]}
        >
          <Input placeholder="Main Depot Diesel Tank" />
        </Form.Item>

        <Row gutter={16}>
          <Col span={12}>
            <Form.Item
              name="capacityLiters"
              label="Total Capacity (Liters)"
              rules={[{ required: true, message: 'Required' }]}
            >
              <InputNumber min={1} style={{ width: '100%' }} placeholder="10000" />
            </Form.Item>
          </Col>
          <Col span={12}>
            <Form.Item name="minimumStockLiters" label="Minimum / Reorder Level (L)">
              <InputNumber min={0} style={{ width: '100%' }} placeholder="500" />
            </Form.Item>
          </Col>
        </Row>

        <Form.Item name="openingBalanceLiters" label="Opening Balance (Liters, optional)">
          <InputNumber min={0} style={{ width: '100%' }} placeholder="0" />
        </Form.Item>
      </Form>
    </Modal>
  );
}

function EditTankModal({
  tank,
  open,
  onClose,
}: {
  tank: BunkerTank;
  open: boolean;
  onClose: () => void;
}) {
  const [form] = Form.useForm();
  const { message } = AntApp.useApp();
  const [errorMsg, setErrorMsg] = useState<string | null>(null);
  const updateMutation = useUpdateBunkerTank(tank.id);

  const handleFinish = async (values: {
    tankName: string;
    minimumStockLiters?: number;
    status: BunkerTankStatus;
    active: boolean;
  }) => {
    setErrorMsg(null);
    try {
      const payload: BunkerTankUpdatePayload = {
        tankName: values.tankName,
        minimumStockLiters: values.minimumStockLiters ? Number(values.minimumStockLiters) : undefined,
        status: values.status,
        active: values.active,
      };
      await updateMutation.mutateAsync(payload);
      message.success(`Tank ${tank.tankCode} updated`);
      onClose();
    } catch (err: unknown) {
      const msg = isAxiosError(err)
        ? (err.response?.data as { message?: string })?.message || err.message
        : err instanceof Error
        ? err.message
        : 'Failed to update tank';
      setErrorMsg(msg);
      message.error(msg);
    }
  };

  return (
    <Modal
      title={`Edit Tank ${tank.tankCode}`}
      open={open}
      onCancel={() => {
        setErrorMsg(null);
        onClose();
      }}
      onOk={() => form.submit()}
      confirmLoading={updateMutation.isPending}
      destroyOnHidden
    >
      {errorMsg && <Alert type="error" showIcon message={errorMsg} style={{ marginBottom: 16 }} />}
      <Alert
        type="info"
        showIcon
        message="Stock is protected"
        description="Current stock cannot be directly edited here. Inventory balances change strictly through Receipts, Issues, Transfers, or Adjustments."
        style={{ marginBottom: 16 }}
      />
      <Form
        form={form}
        layout="vertical"
        initialValues={{
          tankName: tank.tankName,
          minimumStockLiters: tank.minimumStockLiters,
          status: tank.status,
          active: tank.active,
        }}
        onFinish={handleFinish}
      >
        <Form.Item name="tankName" label="Tank Name" rules={[{ required: true }]}>
          <Input />
        </Form.Item>
        <Form.Item name="minimumStockLiters" label="Minimum Stock Level (Liters)">
          <InputNumber min={0} style={{ width: '100%' }} />
        </Form.Item>
        <Form.Item name="status" label="Operating Status" rules={[{ required: true }]}>
          <Select
            aria-label="Operating Status"
            options={[
              { label: 'Active', value: 'ACTIVE' },
              { label: 'Inactive', value: 'INACTIVE' },
              { label: 'Decommissioned', value: 'DECOMMISSIONED' },
            ]}
          />
        </Form.Item>
        <Form.Item name="active" label="Enabled / Active" valuePropName="checked">
          <Radio.Group>
            <Radio value={true}>Active</Radio>
            <Radio value={false}>Inactive</Radio>
          </Radio.Group>
        </Form.Item>
      </Form>
    </Modal>
  );
}

export function RecordDipModal({
  tank,
  open,
  onClose,
}: {
  tank: BunkerTank;
  open: boolean;
  onClose: () => void;
}) {
  const [form] = Form.useForm();
  const { message } = AntApp.useApp();
  const [errorMsg, setErrorMsg] = useState<string | null>(null);
  const dipMutation = useRecordDipReading(tank.id);

  const handleFinish = async (values: {
    physicalQuantityLiters: number;
    notes?: string;
  }) => {
    setErrorMsg(null);
    try {
      await dipMutation.mutateAsync({
        physicalQuantityLiters: Number(values.physicalQuantityLiters),
        notes: values.notes,
      });
      message.success('Physical dip reading recorded successfully');
      form.resetFields();
      onClose();
    } catch (err: unknown) {
      const msg = isAxiosError(err)
        ? (err.response?.data as { message?: string })?.message || err.message
        : err instanceof Error
        ? err.message
        : 'Failed to record dip reading';
      setErrorMsg(msg);
      message.error(msg);
    }
  };

  return (
    <Modal
      title={`Record Physical Dip - ${tank.tankCode}`}
      open={open}
      onCancel={() => {
        setErrorMsg(null);
        onClose();
      }}
      onOk={() => form.submit()}
      confirmLoading={dipMutation.isPending}
      destroyOnHidden
    >
      {errorMsg && <Alert type="error" showIcon message={errorMsg} style={{ marginBottom: 16 }} />}
      <Alert
        type="warning"
        showIcon
        message="Observation Only"
        description="Recording a physical dip captures an observational measurement and calculates variance against the book stock. It does NOT alter the book inventory balance."
        style={{ marginBottom: 16 }}
      />
      <div style={{ marginBottom: 16 }}>
        <Text type="secondary">Current Book Balance: </Text>
        <Text strong>{tank.currentStockLiters.toLocaleString()} L</Text>
      </div>
      <Form form={form} layout="vertical" onFinish={handleFinish}>
        <Form.Item
          name="physicalQuantityLiters"
          label="Physical Dip Measurement (Liters)"
          rules={[{ required: true, message: 'Please enter physical dip reading' }]}
        >
          <InputNumber
            min={0}
            max={tank.capacityLiters * 1.2}
            step={0.1}
            style={{ width: '100%' }}
            placeholder="e.g. 5240.5"
          />
        </Form.Item>
        <Form.Item name="notes" label="Observation Notes / Remarks">
          <Input.TextArea rows={3} placeholder="Dip stick gauge reading, temperature, etc." />
        </Form.Item>
      </Form>
    </Modal>
  );
}

export function StockAdjustmentModal({
  tank,
  open,
  onClose,
}: {
  tank: BunkerTank;
  open: boolean;
  onClose: () => void;
}) {
  const [form] = Form.useForm();
  const { message } = AntApp.useApp();
  const [direction, setDirection] = useState<'INCREASE' | 'DECREASE'>('DECREASE');
  const [amount, setAmount] = useState<number | null>(null);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);
  const adjustMutation = useAdjustBunkerStock(tank.id);

  const resultingStock =
    amount !== null && amount > 0
      ? direction === 'INCREASE'
        ? tank.currentStockLiters + amount
        : tank.currentStockLiters - amount
      : tank.currentStockLiters;

  const handleFinish = async (values: {
    amount: number;
    reason: string;
  }) => {
    setErrorMsg(null);
    try {
      const delta = direction === 'INCREASE' ? Math.abs(values.amount) : -Math.abs(values.amount);
      await adjustMutation.mutateAsync({
        quantityDeltaLiters: delta,
        reason: values.reason.trim(),
      });
      message.success('Stock adjustment committed and posted to ledger');
      form.resetFields();
      onClose();
    } catch (err: unknown) {
      const msg = isAxiosError(err)
        ? (err.response?.data as { message?: string })?.message || err.message
        : err instanceof Error
        ? err.message
        : 'Failed to adjust stock';
      setErrorMsg(msg);
      message.error(msg);
    }
  };

  return (
    <Modal
      title={`Adjust Bunker Stock - ${tank.tankCode}`}
      open={open}
      onCancel={() => {
        setErrorMsg(null);
        onClose();
      }}
      onOk={() => form.submit()}
      confirmLoading={adjustMutation.isPending}
      destroyOnHidden
    >
      {errorMsg && <Alert type="error" showIcon message={errorMsg} style={{ marginBottom: 16 }} />}
      <Alert
        type="error"
        showIcon
        message="Authoritative Inventory Change"
        description="A stock adjustment directly mutates the book inventory balance and creates an audit movement in the stock ledger. Ensure this adjustment is authorized."
        style={{ marginBottom: 16 }}
      />
      <div style={{ marginBottom: 16, background: '#fafafa', padding: 12, borderRadius: 6 }}>
        <Row justify="space-between">
          <Col>
            <Text type="secondary">Current Book Balance: </Text>
            <Text strong>{tank.currentStockLiters.toLocaleString()} L</Text>
          </Col>
          <Col>
            <Text type="secondary">Resulting Balance: </Text>
            <Text
              strong
              style={{
                color: resultingStock < 0 || resultingStock > tank.capacityLiters ? '#ff4d4f' : '#52c41a',
              }}
            >
              {resultingStock.toLocaleString()} L
            </Text>
          </Col>
        </Row>
      </div>
      <Form form={form} layout="vertical" onFinish={handleFinish}>
        <Form.Item label="Adjustment Direction" required>
          <Radio.Group
            value={direction}
            onChange={(e) => setDirection(e.target.value)}
            buttonStyle="solid"
          >
            <Radio.Button value="INCREASE" style={{ color: '#52c41a' }}>
              Stock Increase (+ IN)
            </Radio.Button>
            <Radio.Button value="DECREASE" style={{ color: '#ff4d4f' }}>
              Stock Decrease (- OUT)
            </Radio.Button>
          </Radio.Group>
        </Form.Item>

        <Form.Item
          name="amount"
          label={`Adjustment Quantity (${direction === 'INCREASE' ? '+' : '-'} Liters)`}
          rules={[{ required: true, message: 'Please enter a non-zero adjustment quantity' }]}
        >
          <InputNumber
            min={0.001}
            step={0.1}
            style={{ width: '100%' }}
            placeholder="Quantity in Liters"
            onChange={(val) => setAmount(val)}
          />
        </Form.Item>

        <Form.Item
          name="reason"
          label="Mandatory Reason / Justification"
          rules={[{ required: true, min: 3, message: 'Please provide an adjustment reason' }]}
        >
          <Input.TextArea rows={3} placeholder="Variance reconciliation, calibration adjustment, etc." />
        </Form.Item>
      </Form>
    </Modal>
  );
}

export function TransferModal({
  tanks,
  open,
  onClose,
}: {
  tanks: BunkerTank[];
  open: boolean;
  onClose: () => void;
}) {
  const [form] = Form.useForm();
  const { message } = AntApp.useApp();
  const [sourceId, setSourceId] = useState<string | undefined>();
  const [destId, setDestId] = useState<string | undefined>();
  const [errorMsg, setErrorMsg] = useState<string | null>(null);
  const transferMutation = useTransferBunkerStock();

  const sourceTank = tanks.find((t) => t.id === sourceId);
  const destTank = tanks.find((t) => t.id === destId);

  const handleFinish = async (values: {
    sourceTankId: string;
    destinationTankId: string;
    quantityLiters: number;
    reason?: string;
  }) => {
    setErrorMsg(null);
    try {
      await transferMutation.mutateAsync({
        sourceTankId: values.sourceTankId,
        destinationTankId: values.destinationTankId,
        quantityLiters: Number(values.quantityLiters),
        reason: values.reason,
      });
      message.success('Fuel transfer completed successfully');
      form.resetFields();
      setSourceId(undefined);
      setDestId(undefined);
      onClose();
    } catch (err: unknown) {
      const msg = isAxiosError(err)
        ? (err.response?.data as { message?: string })?.message || err.message
        : err instanceof Error
        ? err.message
        : 'Transfer failed';
      setErrorMsg(msg);
      message.error(msg);
    }
  };

  const eligibleDestinations = tanks.filter(
    (t) => t.id !== sourceId && (!sourceTank || t.fuelType.toUpperCase() === sourceTank.fuelType.toUpperCase())
  );

  return (
    <Modal
      title="Inter-Tank Fuel Transfer"
      open={open}
      onCancel={() => {
        setErrorMsg(null);
        onClose();
      }}
      onOk={() => form.submit()}
      confirmLoading={transferMutation.isPending}
      destroyOnHidden
    >
      {errorMsg && <Alert type="error" showIcon message={errorMsg} style={{ marginBottom: 16 }} />}
      <Alert
        type="info"
        showIcon
        message="Atomic Transfer"
        description="Transfers fuel between tanks of matching fuel type under dual-tank row locks. Both transfer-out and transfer-in audit movements will be recorded."
        style={{ marginBottom: 16 }}
      />
      <Form form={form} layout="vertical" onFinish={handleFinish}>
        <Form.Item
          name="sourceTankId"
          label="Source Tank (Transfer OUT)"
          rules={[{ required: true, message: 'Please select source tank' }]}
        >
          <Select
            aria-label="Source Tank"
            placeholder="Select source tank"
            onChange={(val) => {
              setSourceId(val);
              form.setFieldValue('destinationTankId', undefined);
              setDestId(undefined);
            }}
            options={tanks.map((t) => ({
              label: `${t.tankCode} (${t.fuelType} - Available: ${t.currentStockLiters.toLocaleString()} L)`,
              value: t.id,
            }))}
          />
        </Form.Item>

        <Form.Item
          name="destinationTankId"
          label="Destination Tank (Transfer IN)"
          rules={[{ required: true, message: 'Please select destination tank' }]}
        >
          <Select
            aria-label="Destination Tank"
            placeholder="Select destination tank"
            disabled={!sourceId}
            onChange={(val) => setDestId(val)}
            options={eligibleDestinations.map((t) => ({
              label: `${t.tankCode} (${t.fuelType} - Free Space: ${t.availableCapacityLiters.toLocaleString()} L)`,
              value: t.id,
            }))}
          />
        </Form.Item>

        {sourceTank && destTank && (
          <div style={{ marginBottom: 16, background: '#fafafa', padding: 12, borderRadius: 6 }}>
            <Row gutter={16}>
              <Col span={12}>
                <Text type="secondary">Source Stock: </Text>
                <Text strong>{sourceTank.currentStockLiters.toLocaleString()} L</Text>
              </Col>
              <Col span={12}>
                <Text type="secondary">Dest Free Space: </Text>
                <Text strong>{destTank.availableCapacityLiters.toLocaleString()} L</Text>
              </Col>
            </Row>
          </div>
        )}

        <Form.Item
          name="quantityLiters"
          label="Transfer Quantity (Liters)"
          rules={[{ required: true, message: 'Please enter transfer quantity' }]}
        >
          <InputNumber
            min={0.001}
            max={sourceTank ? sourceTank.currentStockLiters : undefined}
            step={0.1}
            style={{ width: '100%' }}
            placeholder="Quantity in Liters"
          />
        </Form.Item>

        <Form.Item name="reason" label="Transfer Reason / Note">
          <Input.TextArea rows={2} placeholder="Operational tank rebalancing, maintenance transfer, etc." />
        </Form.Item>
      </Form>
    </Modal>
  );
}