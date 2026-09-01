import React, { useState } from 'react';
import {
  Table,
  Button,
  Tag,
  Space,
  Card,
  Typography,
  Flex,
  Select,
  Drawer,
  Form,
  InputNumber,
  message,
  Descriptions,
  Divider,
  Badge,
  Input,
  Modal,
  Popconfirm,
  Checkbox,
} from 'antd';
import {
  PlusOutlined,
  CheckCircleOutlined,
  StopOutlined,
  EyeOutlined,
  SendOutlined,
  UserAddOutlined,
  ClusterOutlined,
  DeleteOutlined,
  ReloadOutlined,
} from '@ant-design/icons';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import dayjs from 'dayjs';
import { useAuth } from '../../../../auth/AuthContext';
import {
  deliveryBatchApi,
  DeliveryBatch,
  DeliveryBatchStatus,
  DeliveryBatchOrder,
  CreateDeliveryBatchPayload,
  AutoClusterBatchesPayload,
  AssignRiderToBatchPayload,
} from '../api/deliveryBatchApi';
import { deliveryEtaApi } from '../../eta/api/deliveryEtaApi';
import { deliveryZoneApi, DeliveryZone } from '../../zones/api/deliveryZoneApi';
import { deliverySlotApi, DeliverySlot } from '../../slots/api/deliverySlotApi';
import { deliveryRiderApi, DeliveryRider } from '../../riders/api/deliveryRiderApi';

const { Title, Text, Paragraph } = Typography;

export const DeliveryBatchListPage: React.FC = () => {
  const queryClient = useQueryClient();
  const { hasPermission } = useAuth();

  const [selectedZoneId, setSelectedZoneId] = useState<string | undefined>(undefined);
  const [selectedSlotId, setSelectedSlotId] = useState<string | undefined>(undefined);
  const [selectedStatus, setSelectedStatus] = useState<DeliveryBatchStatus | undefined>(undefined);

  const [createModalOpen, setCreateModalOpen] = useState(false);
  const [autoClusterModalOpen, setAutoClusterModalOpen] = useState(false);
  const [detailDrawerOpen, setDetailDrawerOpen] = useState(false);
  const [assignRiderModalOpen, setAssignRiderModalOpen] = useState(false);
  const [addOrdersModalOpen, setAddOrdersModalOpen] = useState(false);

  const [selectedBatch, setSelectedBatch] = useState<DeliveryBatch | null>(null);

  const [createForm] = Form.useForm<CreateDeliveryBatchPayload>();
  const [autoClusterForm] = Form.useForm<AutoClusterBatchesPayload>();
  const [assignRiderForm] = Form.useForm<AssignRiderToBatchPayload>();
  const [addOrdersForm] = Form.useForm<{ orderIds: string }>();

  // Fetch zones for filters and forms
  const { data: zonesData } = useQuery<DeliveryZone[]>({
    queryKey: ['delivery-zones'],
    queryFn: async () => deliveryZoneApi.list(),
  });
  const zones = zonesData || [];
  const zoneMap = new Map(zones.map((z) => [z.id, z.zoneName]));

  // Fetch slots for dropdowns
  const { data: slotsData } = useQuery<DeliverySlot[]>({
    queryKey: ['delivery-slots'],
    queryFn: async () => deliverySlotApi.list(),
  });
  const slots = slotsData || [];
  const slotMap = new Map(slots.map((s) => [s.id, `${s.slotDate} ${s.startTime}-${s.endTime} (${s.slotType})`]));

  // Fetch riders for assignment dropdown
  const { data: ridersData } = useQuery<DeliveryRider[]>({
    queryKey: ['delivery-riders'],
    queryFn: async () => {
      const res = await deliveryRiderApi.getRiders({ status: 'ACTIVE' });
      return res.data;
    },
  });
  const riders = ridersData || [];
  const riderMap = new Map(riders.map((r) => [r.id, `${r.riderCode} (${r.riderType})`]));

  // Fetch batches
  const { data: batchesData, isLoading: batchesLoading } = useQuery({
    queryKey: ['delivery-batches', selectedZoneId, selectedSlotId, selectedStatus],
    queryFn: async () => {
      const res = await deliveryBatchApi.getBatches({
        zoneId: selectedZoneId,
        slotId: selectedSlotId,
        status: selectedStatus,
        size: 50,
      });
      return res;
    },
  });
  const batches = batchesData?.content || [];

  // Fetch orders for selected batch
  const { data: batchOrdersData, isLoading: batchOrdersLoading } = useQuery<DeliveryBatchOrder[]>({
    queryKey: ['delivery-batch-orders', selectedBatch?.id],
    queryFn: async () => {
      if (!selectedBatch) return [];
      return await deliveryBatchApi.getBatchOrders(selectedBatch.id);
    },
    enabled: !!selectedBatch,
  });
  const batchOrders = batchOrdersData || [];

  // Fetch ETA for selected batch
  const { data: batchEtaData, isLoading: batchEtaLoading } = useQuery({
    queryKey: ['delivery-batch-eta', selectedBatch?.id],
    queryFn: async () => {
      if (!selectedBatch) return null;
      return await deliveryEtaApi.getBatchEta(selectedBatch.id);
    },
    enabled: !!selectedBatch && detailDrawerOpen,
  });

  const recalculateEtaMutation = useMutation({
    mutationFn: (batchId: string) => deliveryEtaApi.calculateBatchEta(batchId),
    onSuccess: () => {
      message.success('Batch ETA recalculated');
      queryClient.invalidateQueries({ queryKey: ['delivery-batch-eta', selectedBatch?.id] });
    },
    onError: (err: any) => {
      message.error(err.response?.data?.message || 'Failed to recalculate ETA');
    },
  });

  // Mutations
  const createMutation = useMutation({
    mutationFn: (payload: CreateDeliveryBatchPayload) => deliveryBatchApi.createBatch(payload),
    onSuccess: () => {
      message.success('Delivery batch created successfully');
      setCreateModalOpen(false);
      createForm.resetFields();
      queryClient.invalidateQueries({ queryKey: ['delivery-batches'] });
    },
    onError: (err: any) => {
      message.error(err.response?.data?.message || 'Failed to create delivery batch');
    },
  });

  const autoClusterMutation = useMutation({
    mutationFn: (payload: AutoClusterBatchesPayload) => deliveryBatchApi.autoCluster(payload),
    onSuccess: (data) => {
      message.success(`Successfully auto-clustered ${data.length} batches`);
      setAutoClusterModalOpen(false);
      autoClusterForm.resetFields();
      queryClient.invalidateQueries({ queryKey: ['delivery-batches'] });
    },
    onError: (err: any) => {
      message.error(err.response?.data?.message || 'Auto-clustering failed');
    },
  });

  const assignRiderMutation = useMutation({
    mutationFn: ({ batchId, payload }: { batchId: string; payload: AssignRiderToBatchPayload }) =>
      deliveryBatchApi.assignRider(batchId, payload),
    onSuccess: (updated) => {
      message.success('Rider assigned to batch successfully');
      setAssignRiderModalOpen(false);
      assignRiderForm.resetFields();
      setSelectedBatch(updated);
      queryClient.invalidateQueries({ queryKey: ['delivery-batches'] });
    },
    onError: (err: any) => {
      message.error(err.response?.data?.message || 'Failed to assign rider');
    },
  });

  const markReadyMutation = useMutation({
    mutationFn: (batchId: string) => deliveryBatchApi.markReady(batchId),
    onSuccess: (updated) => {
      message.success('Batch marked as READY');
      setSelectedBatch(updated);
      queryClient.invalidateQueries({ queryKey: ['delivery-batches'] });
    },
    onError: (err: any) => {
      message.error(err.response?.data?.message || 'Failed to mark batch as ready');
    },
  });

  const dispatchMutation = useMutation({
    mutationFn: (batchId: string) => deliveryBatchApi.dispatchBatch(batchId),
    onSuccess: (updated) => {
      message.success('Batch dispatched successfully');
      setSelectedBatch(updated);
      queryClient.invalidateQueries({ queryKey: ['delivery-batches'] });
    },
    onError: (err: any) => {
      message.error(err.response?.data?.message || 'Failed to dispatch batch');
    },
  });

  const cancelMutation = useMutation({
    mutationFn: (batchId: string) => deliveryBatchApi.cancelBatch(batchId),
    onSuccess: (updated) => {
      message.success('Batch cancelled successfully');
      setSelectedBatch(updated);
      queryClient.invalidateQueries({ queryKey: ['delivery-batches'] });
    },
    onError: (err: any) => {
      message.error(err.response?.data?.message || 'Failed to cancel batch');
    },
  });

  const removeOrderMutation = useMutation({
    mutationFn: ({ batchId, orderId }: { batchId: string; orderId: string }) =>
      deliveryBatchApi.removeOrder(batchId, orderId),
    onSuccess: (updated) => {
      message.success('Order removed from batch');
      setSelectedBatch(updated);
      queryClient.invalidateQueries({ queryKey: ['delivery-batches'] });
      queryClient.invalidateQueries({ queryKey: ['delivery-batch-orders', updated.id] });
    },
    onError: (err: any) => {
      message.error(err.response?.data?.message || 'Failed to remove order from batch');
    },
  });

  const addOrdersMutation = useMutation({
    mutationFn: ({ batchId, orderIds }: { batchId: string; orderIds: string[] }) =>
      deliveryBatchApi.addOrders(batchId, { deliveryOrderIds: orderIds }),
    onSuccess: (updated) => {
      message.success('Orders added to batch');
      setAddOrdersModalOpen(false);
      addOrdersForm.resetFields();
      setSelectedBatch(updated);
      queryClient.invalidateQueries({ queryKey: ['delivery-batches'] });
      queryClient.invalidateQueries({ queryKey: ['delivery-batch-orders', updated.id] });
    },
    onError: (err: any) => {
      message.error(err.response?.data?.message || 'Failed to add orders');
    },
  });

  const getStatusTag = (status: DeliveryBatchStatus) => {
    switch (status) {
      case 'DRAFT':
        return <Tag color="default">DRAFT</Tag>;
      case 'READY':
        return <Tag color="blue">READY</Tag>;
      case 'ASSIGNED':
        return <Tag color="orange">ASSIGNED</Tag>;
      case 'DISPATCHED':
        return <Tag color="purple">DISPATCHED</Tag>;
      case 'COMPLETED':
        return <Tag color="green">COMPLETED</Tag>;
      case 'CANCELLED':
        return <Tag color="red">CANCELLED</Tag>;
      default:
        return <Tag>{status}</Tag>;
    }
  };

  const columns = [
    {
      title: 'Batch Code',
      dataIndex: 'batchCode',
      key: 'batchCode',
      render: (code: string, record: DeliveryBatch) => (
        <Space direction="vertical" size={0}>
          <Text strong>{code}</Text>
          <Text type="secondary" style={{ fontSize: 11 }}>
            Created {dayjs(record.createdAt).format('YYYY-MM-DD HH:mm')}
          </Text>
        </Space>
      ),
    },
    {
      title: 'Zone & Slot',
      key: 'zoneSlot',
      render: (_: any, record: DeliveryBatch) => (
        <Space direction="vertical" size={0}>
          <Text>{zoneMap.get(record.deliveryZoneId) || record.deliveryZoneId}</Text>
          {record.deliverySlotId && (
            <Text type="secondary" style={{ fontSize: 12 }}>
              Slot: {slotMap.get(record.deliverySlotId) || record.deliverySlotId}
            </Text>
          )}
        </Space>
      ),
    },
    {
      title: 'Status',
      dataIndex: 'status',
      key: 'status',
      render: (status: DeliveryBatchStatus) => getStatusTag(status),
    },
    {
      title: 'Capacity / Orders',
      key: 'capacity',
      render: (_: any, record: DeliveryBatch) => (
        <Space direction="vertical" size={0}>
          <Badge
            status={record.activeOrderCount >= record.maxBatchSize ? 'error' : 'processing'}
            text={`${record.activeOrderCount} / ${record.maxBatchSize} active orders`}
          />
          <Text type="secondary" style={{ fontSize: 11 }}>
            Total processed: {record.totalOrderCount}
          </Text>
        </Space>
      ),
    },
    {
      title: 'Assigned Rider',
      dataIndex: 'riderId',
      key: 'riderId',
      render: (riderId?: string) =>
        riderId ? (
          <Tag color="cyan">{riderMap.get(riderId) || riderId}</Tag>
        ) : (
          <Text type="secondary">Unassigned</Text>
        ),
    },
    {
      title: 'Actions',
      key: 'actions',
      render: (_: any, record: DeliveryBatch) => (
        <Space wrap>
          <Button
            size="small"
            icon={<EyeOutlined />}
            onClick={() => {
              setSelectedBatch(record);
              setDetailDrawerOpen(true);
            }}
          >
            Details
          </Button>

          {hasPermission('DELIVERY_BATCH_UPDATE') && record.status === 'DRAFT' && (
            <Button
              size="small"
              icon={<CheckCircleOutlined />}
              onClick={() => markReadyMutation.mutate(record.id)}
              loading={markReadyMutation.isPending}
            >
              Ready
            </Button>
          )}

          {hasPermission('DELIVERY_BATCH_ASSIGN') &&
            (record.status === 'DRAFT' || record.status === 'READY') && (
              <Button
                size="small"
                icon={<UserAddOutlined />}
                onClick={() => {
                  setSelectedBatch(record);
                  setAssignRiderModalOpen(true);
                }}
              >
                Assign
              </Button>
            )}

          {hasPermission('DELIVERY_BATCH_DISPATCH') && record.status === 'ASSIGNED' && (
            <Button
              size="small"
              type="primary"
              icon={<SendOutlined />}
              onClick={() => dispatchMutation.mutate(record.id)}
              loading={dispatchMutation.isPending}
            >
              Dispatch
            </Button>
          )}

          {hasPermission('DELIVERY_BATCH_CANCEL') &&
            record.status !== 'CANCELLED' &&
            record.status !== 'COMPLETED' && (
              <Popconfirm
                title="Cancel batch"
                description="Are you sure you want to cancel this batch? Active order memberships will be unbatched."
                onConfirm={() => cancelMutation.mutate(record.id)}
                okText="Yes, Cancel"
                cancelText="No"
              >
                <Button size="small" danger icon={<StopOutlined />}>
                  Cancel
                </Button>
              </Popconfirm>
            )}
        </Space>
      ),
    },
  ];

  return (
    <div style={{ padding: '24px' }}>
      <Flex justify="space-between" align="center" style={{ marginBottom: 20 }}>
        <div>
          <Title level={2} style={{ marginBottom: 0 }}>
            Delivery Batches & Clustering
          </Title>
          <Paragraph type="secondary" style={{ marginBottom: 0 }}>
            Plan, cluster, and assign delivery batches under zone and capacity constraints
          </Paragraph>
        </div>
        <Space>
          {hasPermission('DELIVERY_BATCH_CREATE') && (
            <>
              <Button
                icon={<ClusterOutlined />}
                onClick={() => setAutoClusterModalOpen(true)}
              >
                Auto-Cluster
              </Button>
              <Button
                type="primary"
                icon={<PlusOutlined />}
                onClick={() => setCreateModalOpen(true)}
              >
                Create Batch
              </Button>
            </>
          )}
        </Space>
      </Flex>

      <Card style={{ marginBottom: 20 }}>
        <Flex gap={16} wrap="wrap">
          <Select
            placeholder="Filter by Zone"
            allowClear
            style={{ width: 220 }}
            value={selectedZoneId}
            onChange={(val) => setSelectedZoneId(val)}
            options={zones.map((z) => ({ label: z.zoneName, value: z.id }))}
          />
          <Select
            placeholder="Filter by Slot"
            allowClear
            style={{ width: 220 }}
            value={selectedSlotId}
            onChange={(val) => setSelectedSlotId(val)}
            options={slots.map((s) => ({ label: `${s.slotDate} ${s.startTime}-${s.endTime} (${s.slotType})`, value: s.id }))}
          />
          <Select
            placeholder="Filter by Status"
            allowClear
            style={{ width: 180 }}
            value={selectedStatus}
            onChange={(val) => setSelectedStatus(val)}
            options={[
              { label: 'DRAFT', value: 'DRAFT' },
              { label: 'READY', value: 'READY' },
              { label: 'ASSIGNED', value: 'ASSIGNED' },
              { label: 'DISPATCHED', value: 'DISPATCHED' },
              { label: 'COMPLETED', value: 'COMPLETED' },
              { label: 'CANCELLED', value: 'CANCELLED' },
            ]}
          />
        </Flex>
      </Card>

      <Card>
        <Table
          columns={columns}
          dataSource={batches}
          rowKey="id"
          loading={batchesLoading}
          pagination={{ pageSize: 10, showSizeChanger: true }}
        />
      </Card>

      {/* Manual Create Modal */}
      <Modal
        title="Create Manual Delivery Batch"
        open={createModalOpen}
        onCancel={() => setCreateModalOpen(false)}
        onOk={() => createForm.submit()}
        confirmLoading={createMutation.isPending}
      >
        <Form
          form={createForm}
          layout="vertical"
          onFinish={(values) => createMutation.mutate(values)}
          initialValues={{ maxBatchSize: 5 }}
        >
          <Form.Item
            name="deliveryZoneId"
            label="Delivery Zone"
            rules={[{ required: true, message: 'Delivery zone is required' }]}
          >
            <Select
              placeholder="Select delivery zone"
              options={zones.map((z) => ({ label: `${z.zoneCode} - ${z.zoneName}`, value: z.id }))}
            />
          </Form.Item>

          <Form.Item name="deliverySlotId" label="Delivery Slot (Optional)">
            <Select
              placeholder="Select slot (optional)"
              allowClear
              options={slots.map((s) => ({ label: `${s.slotDate} ${s.startTime}-${s.endTime} (${s.slotType})`, value: s.id }))}
            />
          </Form.Item>

          <Form.Item
            name="maxBatchSize"
            label="Max Batch Size"
            rules={[{ required: true, message: 'Max size is required' }]}
          >
            <InputNumber min={1} max={50} style={{ width: '100%' }} />
          </Form.Item>

          <Form.Item name="riderId" label="Assign Rider (Optional)">
            <Select
              placeholder="Assign rider immediately (optional)"
              allowClear
              options={riders.map((r) => ({ label: `${r.riderCode} (${r.riderType})`, value: r.id }))}
            />
          </Form.Item>
        </Form>
      </Modal>

      {/* Auto-Cluster Modal */}
      <Modal
        title="Auto-Cluster Ready Delivery Orders"
        open={autoClusterModalOpen}
        onCancel={() => setAutoClusterModalOpen(false)}
        onOk={() => autoClusterForm.submit()}
        confirmLoading={autoClusterMutation.isPending}
      >
        <Form
          form={autoClusterForm}
          layout="vertical"
          onFinish={(values) => autoClusterMutation.mutate(values)}
          initialValues={{ maxBatchSize: 5, maxDistanceKm: 10.0 }}
        >
          <Form.Item
            name="deliveryZoneId"
            label="Target Delivery Zone"
            rules={[{ required: true, message: 'Delivery zone is required' }]}
          >
            <Select
              placeholder="Select delivery zone for clustering"
              options={zones.map((z) => ({ label: `${z.zoneCode} - ${z.zoneName}`, value: z.id }))}
            />
          </Form.Item>

          <Form.Item name="deliverySlotId" label="Delivery Slot Filter (Optional)">
            <Select
              placeholder="Filter by slot (optional)"
              allowClear
              options={slots.map((s) => ({ label: `${s.slotDate} ${s.startTime}-${s.endTime} (${s.slotType})`, value: s.id }))}
            />
          </Form.Item>

          <Form.Item name="maxBatchSize" label="Max Batch Size">
            <InputNumber min={1} max={50} style={{ width: '100%' }} />
          </Form.Item>

          <Form.Item name="maxDistanceKm" label="Max Cluster Radius (km)">
            <InputNumber min={0.5} max={100} step={0.5} style={{ width: '100%' }} />
          </Form.Item>
        </Form>
      </Modal>

      {/* Assign Rider Modal */}
      <Modal
        title={`Assign Rider to Batch ${selectedBatch?.batchCode}`}
        open={assignRiderModalOpen}
        onCancel={() => setAssignRiderModalOpen(false)}
        onOk={() => assignRiderForm.submit()}
        confirmLoading={assignRiderMutation.isPending}
      >
        <Form
          form={assignRiderForm}
          layout="vertical"
          onFinish={(values) => {
            if (selectedBatch) {
              assignRiderMutation.mutate({ batchId: selectedBatch.id, payload: values });
            }
          }}
        >
          <Form.Item
            name="riderId"
            label="Select Rider"
            rules={[{ required: true, message: 'Rider is required' }]}
          >
            <Select
              placeholder="Select active rider"
              options={riders.map((r) => ({ label: `${r.riderCode} (${r.riderType})`, value: r.id }))}
            />
          </Form.Item>

          <Form.Item name="override" valuePropName="checked">
            <Checkbox>Override Rider Capacity Limit</Checkbox>
          </Form.Item>

          <Form.Item
            noStyle
            shouldUpdate={(prevValues, currentValues) => prevValues.override !== currentValues.override}
          >
            {({ getFieldValue }) =>
              getFieldValue('override') ? (
                <Form.Item
                  name="overrideReason"
                  label="Override Reason"
                  rules={[{ required: true, message: 'Reason is required for capacity override' }]}
                >
                  <Input.TextArea rows={2} placeholder="Explain reason for capacity override" />
                </Form.Item>
              ) : null
            }
          </Form.Item>
        </Form>
      </Modal>

      {/* Add Orders Modal */}
      <Modal
        title={`Add Orders to Batch ${selectedBatch?.batchCode}`}
        open={addOrdersModalOpen}
        onCancel={() => setAddOrdersModalOpen(false)}
        onOk={() => addOrdersForm.submit()}
        confirmLoading={addOrdersMutation.isPending}
      >
        <Form
          form={addOrdersForm}
          layout="vertical"
          onFinish={(values) => {
            if (selectedBatch) {
              const orderIds = values.orderIds
                .split('\n')
                .map((s) => s.trim())
                .filter((s) => s.length > 0);
              addOrdersMutation.mutate({ batchId: selectedBatch.id, orderIds });
            }
          }}
        >
          <Form.Item
            name="orderIds"
            label="Delivery Order UUIDs (one per line)"
            rules={[{ required: true, message: 'At least one order UUID is required' }]}
          >
            <Input.TextArea rows={4} placeholder="e.g. 9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d" />
          </Form.Item>
        </Form>
      </Modal>

      {/* Batch Details Drawer */}
      <Drawer
        title={`Batch Details: ${selectedBatch?.batchCode}`}
        width={720}
        open={detailDrawerOpen}
        onClose={() => setDetailDrawerOpen(false)}
        extra={
          hasPermission('DELIVERY_BATCH_UPDATE') &&
          (selectedBatch?.status === 'DRAFT' || selectedBatch?.status === 'READY') && (
            <Button
              type="primary"
              size="small"
              icon={<PlusOutlined />}
              onClick={() => setAddOrdersModalOpen(true)}
            >
              Add Orders
            </Button>
          )
        }
      >
        {selectedBatch && (
          <div>
            <Descriptions bordered column={2} size="small">
              <Descriptions.Item label="Batch Code">{selectedBatch.batchCode}</Descriptions.Item>
              <Descriptions.Item label="Status">{getStatusTag(selectedBatch.status)}</Descriptions.Item>
              <Descriptions.Item label="Zone">
                {zoneMap.get(selectedBatch.deliveryZoneId) || selectedBatch.deliveryZoneId}
              </Descriptions.Item>
              <Descriptions.Item label="Slot">
                {selectedBatch.deliverySlotId
                  ? slotMap.get(selectedBatch.deliverySlotId) || selectedBatch.deliverySlotId
                  : 'N/A'}
              </Descriptions.Item>
              <Descriptions.Item label="Max Capacity">{selectedBatch.maxBatchSize}</Descriptions.Item>
              <Descriptions.Item label="Active Orders">{selectedBatch.activeOrderCount}</Descriptions.Item>
              <Descriptions.Item label="Assigned Rider" span={2}>
                {selectedBatch.riderId
                  ? riderMap.get(selectedBatch.riderId) || selectedBatch.riderId
                  : 'None'}
              </Descriptions.Item>
              <Descriptions.Item label="Created At">
                {dayjs(selectedBatch.createdAt).format('YYYY-MM-DD HH:mm:ss')}
              </Descriptions.Item>
              <Descriptions.Item label="Updated At">
                {dayjs(selectedBatch.updatedAt).format('YYYY-MM-DD HH:mm:ss')}
              </Descriptions.Item>
            </Descriptions>

            <Divider orientation="left">
              <Space>
                <span>Estimated Arrival & Route Projection</span>
                {hasPermission('DELIVERY_BATCH_UPDATE') && (
                  <Button
                    size="small"
                    icon={<ReloadOutlined />}
                    loading={recalculateEtaMutation.isPending}
                    onClick={() => recalculateEtaMutation.mutate(selectedBatch.id)}
                  >
                    Recalculate ETA
                  </Button>
                )}
              </Space>
            </Divider>

            {batchEtaLoading ? (
              <Text type="secondary">Loading ETA projection...</Text>
            ) : batchEtaData ? (
              <Card size="small" style={{ marginBottom: 16, backgroundColor: '#fafafa' }}>
                <Descriptions column={3} size="small">
                  <Descriptions.Item label="Completion ETA">
                    <Text strong>{dayjs(batchEtaData.estimatedCompletionAt).format('HH:mm:ss')}</Text>
                  </Descriptions.Item>
                  <Descriptions.Item label="Total Duration">
                    {Math.round(batchEtaData.totalDurationSeconds / 60)} mins
                  </Descriptions.Item>
                  <Descriptions.Item label="Total Distance">
                    {(batchEtaData.totalDistanceMeters / 1000).toFixed(1)} km
                  </Descriptions.Item>
                  <Descriptions.Item label="Source">
                    <Tag>{batchEtaData.source}</Tag>
                  </Descriptions.Item>
                  <Descriptions.Item label="Freshness">
                    {batchEtaData.isStale ? (
                      <Badge status="error" text="Stale (>15m)" />
                    ) : (
                      <Badge status="success" text="Fresh" />
                    )}
                  </Descriptions.Item>
                  <Descriptions.Item label="Calculated At">
                    {dayjs(batchEtaData.calculatedAt).format('HH:mm:ss')}
                  </Descriptions.Item>
                </Descriptions>
              </Card>
            ) : (
              <Text type="secondary">No ETA projection calculated yet.</Text>
            )}

            <Divider orientation="left">Contained Orders ({batchOrders.length})</Divider>

            <Table
              dataSource={batchOrders}
              rowKey="id"
              loading={batchOrdersLoading}
              size="small"
              pagination={false}
              columns={[
                {
                  title: 'Order ID',
                  dataIndex: 'deliveryOrderId',
                  key: 'deliveryOrderId',
                  render: (id: string) => <Text code>{id}</Text>,
                },
                {
                  title: 'Sequence Hint',
                  dataIndex: 'sequenceHint',
                  key: 'sequenceHint',
                  render: (seq?: number) => (seq ? `#${seq}` : '-'),
                },
                {
                  title: 'Stop ETA',
                  key: 'stopEta',
                  render: (_: any, record: DeliveryBatchOrder) => {
                    const stop = batchEtaData?.stops.find((s) => s.deliveryOrderId === record.deliveryOrderId);
                    if (!stop) return <Text type="secondary">-</Text>;
                    return (
                      <Space direction="vertical" size={2}>
                        <Text strong>{dayjs(stop.estimatedArrivalAt).format('HH:mm:ss')}</Text>
                        <Text type="secondary" style={{ fontSize: 11 }}>
                          {(stop.distanceMeters / 1000).toFixed(1)} km ({Math.round(stop.travelDurationSeconds / 60)}m travel + {Math.round(stop.serviceDurationSeconds / 60)}m buffer)
                        </Text>
                        {stop.slaStatus && (
                          <Tag color={stop.slaStatus === 'ON_TIME' ? 'green' : stop.slaStatus === 'AT_RISK' ? 'orange' : 'red'}>
                            {stop.slaStatus}
                          </Tag>
                        )}
                      </Space>
                    );
                  },
                },
                {
                  title: 'Status',
                  dataIndex: 'status',
                  key: 'status',
                  render: (st: string) => (
                    <Tag color={st === 'ACTIVE' ? 'green' : st === 'REMOVED' ? 'red' : 'blue'}>
                      {st}
                    </Tag>
                  ),
                },
                {
                  title: 'Added At',
                  dataIndex: 'addedAt',
                  key: 'addedAt',
                  render: (d: string) => dayjs(d).format('HH:mm:ss'),
                },
                {
                  title: 'Action',
                  key: 'action',
                  render: (_: any, record: DeliveryBatchOrder) =>
                    record.status === 'ACTIVE' &&
                    (selectedBatch.status === 'DRAFT' || selectedBatch.status === 'READY') && (
                      <Button
                        size="small"
                        danger
                        icon={<DeleteOutlined />}
                        onClick={() =>
                          removeOrderMutation.mutate({
                            batchId: selectedBatch.id,
                            orderId: record.deliveryOrderId,
                          })
                        }
                      >
                        Remove
                      </Button>
                    ),
                },
              ]}
            />
          </div>
        )}
      </Drawer>
    </div>
  );
};
