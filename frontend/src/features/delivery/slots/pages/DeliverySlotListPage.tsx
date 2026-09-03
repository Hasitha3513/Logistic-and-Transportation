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
  DatePicker,
  Drawer,
  Form,
  InputNumber,
  TimePicker,
  message,
  Popconfirm,
  Descriptions,
  Divider,
  Progress,
  Badge,
  Input,
  Switch,
  Modal,
} from 'antd';
import {
  PlusOutlined,
  CheckCircleOutlined,
  StopOutlined,
  CloseCircleOutlined,
  EyeOutlined,
  EditOutlined,
  SearchOutlined,
  UserAddOutlined,
  DeleteOutlined,
} from '@ant-design/icons';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import dayjs, { Dayjs } from 'dayjs';
import { useAuth } from '../../../../auth/AuthContext';
import {
  deliverySlotApi,
  DeliverySlot,
  DeliverySlotStatus,
  DeliverySlotType,
  CreateDeliverySlotPayload,
  UpdateDeliverySlotPayload,
  DeliverySlotReservation,
} from '../api/deliverySlotApi';
import { deliveryZoneApi, DeliveryZone } from '../../zones/api/deliveryZoneApi';

const { Title, Text, Paragraph } = Typography;

export const DeliverySlotListPage: React.FC = () => {
  const queryClient = useQueryClient();
  const { hasPermission } = useAuth();

  const [selectedZoneId, setSelectedZoneId] = useState<string | undefined>(undefined);
  const [selectedDate, setSelectedDate] = useState<Dayjs | null>(dayjs());

  const [drawerOpen, setDrawerOpen] = useState(false);
  const [drawerMode, setDrawerMode] = useState<'create' | 'edit' | 'view'>('create');
  const [selectedSlot, setSelectedSlot] = useState<DeliverySlot | null>(null);

  // Assign Order Modal state
  const [assignModalOpen, setAssignModalOpen] = useState(false);
  const [targetSlotForAssign, setTargetSlotForAssign] = useState<DeliverySlot | null>(null);
  const [assignForm] = Form.useForm();

  const [form] = Form.useForm();

  // Load Zones for selection & names
  const { data: zones = [] } = useQuery({
    queryKey: ['delivery-zones'],
    queryFn: () => deliveryZoneApi.list(),
  });

  const zoneMap = new Map<string, DeliveryZone>(zones.map((z) => [z.id, z]));

  // Load Slots
  const { data: slots = [], isLoading } = useQuery({
    queryKey: ['delivery-slots', selectedZoneId, selectedDate?.format('YYYY-MM-DD')],
    queryFn: () =>
      deliverySlotApi.list({
        zoneId: selectedZoneId,
        date: selectedDate?.format('YYYY-MM-DD'),
      }),
  });

  // Load Reservations for drawer view
  const { data: reservations = [], isLoading: isLoadingReservations } = useQuery({
    queryKey: ['delivery-slot-reservations', selectedSlot?.id],
    queryFn: () => (selectedSlot ? deliverySlotApi.listReservations(selectedSlot.id) : Promise.resolve([])),
    enabled: Boolean(selectedSlot && drawerOpen && drawerMode === 'view'),
  });

  // Mutations
  const createMutation = useMutation({
    mutationFn: (payload: CreateDeliverySlotPayload) => deliverySlotApi.create(payload),
    onSuccess: () => {
      message.success('Delivery slot created successfully');
      queryClient.invalidateQueries({ queryKey: ['delivery-slots'] });
      setDrawerOpen(false);
      form.resetFields();
    },
    onError: (err: any) => {
      message.error(err?.response?.data?.message || 'Failed to create delivery slot');
    },
  });

  const updateMutation = useMutation({
    mutationFn: ({ id, payload }: { id: string; payload: UpdateDeliverySlotPayload }) =>
      deliverySlotApi.update(id, payload),
    onSuccess: () => {
      message.success('Delivery slot updated successfully');
      queryClient.invalidateQueries({ queryKey: ['delivery-slots'] });
      setDrawerOpen(false);
      form.resetFields();
    },
    onError: (err: any) => {
      message.error(err?.response?.data?.message || 'Failed to update delivery slot');
    },
  });

  const activateMutation = useMutation({
    mutationFn: (id: string) => deliverySlotApi.activate(id),
    onSuccess: () => {
      message.success('Delivery slot activated');
      queryClient.invalidateQueries({ queryKey: ['delivery-slots'] });
    },
    onError: (err: any) => {
      message.error(err?.response?.data?.message || 'Failed to activate delivery slot');
    },
  });

  const deactivateMutation = useMutation({
    mutationFn: (id: string) => deliverySlotApi.deactivate(id),
    onSuccess: () => {
      message.success('Delivery slot deactivated');
      queryClient.invalidateQueries({ queryKey: ['delivery-slots'] });
    },
    onError: (err: any) => {
      message.error(err?.response?.data?.message || 'Failed to deactivate delivery slot');
    },
  });

  const closeMutation = useMutation({
    mutationFn: (id: string) => deliverySlotApi.close(id),
    onSuccess: () => {
      message.success('Delivery slot closed');
      queryClient.invalidateQueries({ queryKey: ['delivery-slots'] });
    },
    onError: (err: any) => {
      message.error(err?.response?.data?.message || 'Failed to close delivery slot');
    },
  });

  const assignOrderMutation = useMutation({
    mutationFn: ({ slotId, payload }: { slotId: string; payload: any }) =>
      deliverySlotApi.assignOrder(slotId, payload),
    onSuccess: () => {
      message.success('Order assigned to slot successfully');
      queryClient.invalidateQueries({ queryKey: ['delivery-slots'] });
      queryClient.invalidateQueries({ queryKey: ['delivery-slot-reservations'] });
      setAssignModalOpen(false);
      assignForm.resetFields();
    },
    onError: (err: any) => {
      message.error(err?.response?.data?.message || 'Failed to assign order');
    },
  });

  const releaseOrderMutation = useMutation({
    mutationFn: ({ slotId, orderId }: { slotId: string; orderId: string }) =>
      deliverySlotApi.releaseOrder(slotId, orderId),
    onSuccess: () => {
      message.success('Order reservation released');
      queryClient.invalidateQueries({ queryKey: ['delivery-slots'] });
      queryClient.invalidateQueries({ queryKey: ['delivery-slot-reservations'] });
    },
    onError: (err: any) => {
      message.error(err?.response?.data?.message || 'Failed to release reservation');
    },
  });

  const handleOpenCreate = () => {
    setDrawerMode('create');
    setSelectedSlot(null);
    form.resetFields();
    form.setFieldsValue({
      slotDate: selectedDate || dayjs(),
      deliveryZoneId: selectedZoneId,
      slotType: 'STANDARD',
      maxCapacity: 10,
      bufferMinutes: 0,
    });
    setDrawerOpen(true);
  };

  const handleOpenEdit = (slot: DeliverySlot) => {
    setDrawerMode('edit');
    setSelectedSlot(slot);
    form.resetFields();
    form.setFieldsValue({
      timeRange: [dayjs(slot.startTime, 'HH:mm:ss'), dayjs(slot.endTime, 'HH:mm:ss')],
      slotType: slot.slotType,
      maxCapacity: slot.maxCapacity,
      bufferMinutes: slot.bufferMinutes,
      cutoffTime: slot.cutoffTime ? dayjs(slot.cutoffTime) : undefined,
    });
    setDrawerOpen(true);
  };

  const handleOpenView = (slot: DeliverySlot) => {
    setDrawerMode('view');
    setSelectedSlot(slot);
    setDrawerOpen(true);
  };

  const handleFormSubmit = async () => {
    try {
      const values = await form.validateFields();
      if (drawerMode === 'create') {
        const payload: CreateDeliverySlotPayload = {
          deliveryZoneId: values.deliveryZoneId,
          slotDate: values.slotDate.format('YYYY-MM-DD'),
          startTime: values.timeRange[0].format('HH:mm:ss'),
          endTime: values.timeRange[1].format('HH:mm:ss'),
          slotType: values.slotType,
          maxCapacity: values.maxCapacity,
          bufferMinutes: values.bufferMinutes || 0,
          cutoffTime: values.cutoffTime ? values.cutoffTime.toISOString() : undefined,
        };
        createMutation.mutate(payload);
      } else if (drawerMode === 'edit' && selectedSlot) {
        const payload: UpdateDeliverySlotPayload = {
          startTime: values.timeRange[0].format('HH:mm:ss'),
          endTime: values.timeRange[1].format('HH:mm:ss'),
          slotType: values.slotType,
          maxCapacity: values.maxCapacity,
          bufferMinutes: values.bufferMinutes || 0,
          cutoffTime: values.cutoffTime ? values.cutoffTime.toISOString() : undefined,
          expectedVersion: selectedSlot.version,
        };
        updateMutation.mutate({ id: selectedSlot.id, payload });
      }
    } catch {
      // Form validation error
    }
  };

  const handleAssignSubmit = async () => {
    try {
      const values = await assignForm.validateFields();
      if (targetSlotForAssign) {
        assignOrderMutation.mutate({
          slotId: targetSlotForAssign.id,
          payload: {
            deliveryOrderId: values.deliveryOrderId,
            managerOverride: values.managerOverride || false,
            overrideReason: values.overrideReason,
          },
        });
      }
    } catch {
      // validation error
    }
  };

  const getSlotTypeColor = (type: DeliverySlotType) => {
    switch (type) {
      case 'STANDARD':
        return 'blue';
      case 'EXPRESS':
        return 'gold';
      case 'SAME_DAY':
        return 'volcano';
      case 'PEAK_WINDOW':
        return 'purple';
      default:
        return 'default';
    }
  };

  const getStatusBadge = (status: DeliverySlotStatus) => {
    switch (status) {
      case 'ACTIVE':
        return <Badge status="success" text="Active" />;
      case 'INACTIVE':
        return <Badge status="warning" text="Inactive" />;
      case 'CLOSED':
        return <Badge status="error" text="Closed" />;
      default:
        return <Badge status="default" text={status} />;
    }
  };

  const columns = [
    {
      title: 'Time Window',
      key: 'window',
      render: (_: any, record: DeliverySlot) => (
        <Space direction="vertical" size={2}>
          <Text strong>
            {record.startTime.substring(0, 5)} - {record.endTime.substring(0, 5)}
          </Text>
          <Text type="secondary" style={{ fontSize: 12 }}>
            Date: {record.slotDate}
          </Text>
        </Space>
      ),
    },
    {
      title: 'Zone',
      key: 'zone',
      render: (_: any, record: DeliverySlot) => {
        const zone = zoneMap.get(record.deliveryZoneId);
        return (
          <Space direction="vertical" size={2}>
            <Text strong>{zone ? zone.zoneName : 'Zone'}</Text>
            <Text type="secondary" style={{ fontSize: 12 }}>
              {zone?.zoneCode || record.deliveryZoneId}
            </Text>
          </Space>
        );
      },
    },
    {
      title: 'Type',
      dataIndex: 'slotType',
      key: 'slotType',
      render: (type: DeliverySlotType) => <Tag color={getSlotTypeColor(type)}>{type}</Tag>,
    },
    {
      title: 'Capacity Utilization',
      key: 'capacity',
      render: (_: any, record: DeliverySlot) => {
        const percent = Math.min(100, Math.round((record.reservedCapacity / Math.max(1, record.maxCapacity)) * 100));
        let statusColor: 'success' | 'normal' | 'exception' = 'normal';
        if (percent >= 100) statusColor = 'exception';
        else if (percent > 75) statusColor = 'normal';
        else statusColor = 'success';

        return (
          <div style={{ minWidth: 160 }}>
            <Flex justify="space-between">
              <Text style={{ fontSize: 12 }}>
                {record.reservedCapacity} / {record.maxCapacity} booked
              </Text>
              <Text style={{ fontSize: 12 }} type="secondary">
                {record.remainingCapacity} left
              </Text>
            </Flex>
            <Progress percent={percent} size="small" status={statusColor} />
          </div>
        );
      },
    },
    {
      title: 'Status',
      dataIndex: 'status',
      key: 'status',
      render: (status: DeliverySlotStatus) => getStatusBadge(status),
    },
    {
      title: 'Actions',
      key: 'actions',
      render: (_: any, record: DeliverySlot) => (
        <Space size="middle">
          <Button
            type="text"
            icon={<EyeOutlined />}
            onClick={() => handleOpenView(record)}
            data-testid={`view-slot-${record.id}`}
          />
          {hasPermission('DELIVERY_SLOT_UPDATE') && (
            <Button
              type="text"
              icon={<EditOutlined />}
              onClick={() => handleOpenEdit(record)}
              data-testid={`edit-slot-${record.id}`}
            />
          )}
          {hasPermission('DELIVERY_SLOT_ASSIGN') && record.status === 'ACTIVE' && (
            <Button
              type="link"
              size="small"
              icon={<UserAddOutlined />}
              onClick={() => {
                setTargetSlotForAssign(record);
                assignForm.resetFields();
                setAssignModalOpen(true);
              }}
              data-testid={`assign-slot-${record.id}`}
            >
              Assign
            </Button>
          )}
          {hasPermission('DELIVERY_SLOT_ACTIVATE') && record.status === 'INACTIVE' && (
            <Popconfirm
              title="Activate Delivery Slot"
              description="Make this slot open for booking?"
              onConfirm={() => activateMutation.mutate(record.id)}
            >
              <Button type="text" icon={<CheckCircleOutlined />} data-testid={`activate-slot-${record.id}`} />
            </Popconfirm>
          )}
          {hasPermission('DELIVERY_SLOT_ACTIVATE') && record.status === 'ACTIVE' && (
            <Popconfirm
              title="Deactivate Delivery Slot"
              description="Disable new bookings for this slot?"
              onConfirm={() => deactivateMutation.mutate(record.id)}
            >
              <Button type="text" danger icon={<StopOutlined />} data-testid={`deactivate-slot-${record.id}`} />
            </Popconfirm>
          )}
          {hasPermission('DELIVERY_SLOT_ACTIVATE') && record.status !== 'CLOSED' && (
            <Popconfirm
              title="Close Delivery Slot"
              description="Permanently close this slot from operations?"
              onConfirm={() => closeMutation.mutate(record.id)}
            >
              <Button type="text" danger icon={<CloseCircleOutlined />} data-testid={`close-slot-${record.id}`} />
            </Popconfirm>
          )}
        </Space>
      ),
    },
  ];

  return (
    <div style={{ padding: 24 }} data-testid="delivery-slot-list-page">
      <Card>
        <Flex justify="space-between" align="center" style={{ marginBottom: 20 }}>
          <div>
            <Title level={3} style={{ margin: 0 }}>
              Delivery Slots & Capacity Management
            </Title>
            <Paragraph type="secondary" style={{ margin: 0 }}>
              Configure time windows, capacity thresholds, cutoff constraints, and order assignments per zone.
            </Paragraph>
          </div>
          {hasPermission('DELIVERY_SLOT_CREATE') && (
            <Button
              type="primary"
              icon={<PlusOutlined />}
              onClick={handleOpenCreate}
              data-testid="create-slot-button"
            >
              New Delivery Slot
            </Button>
          )}
        </Flex>

        {/* Filter Toolbar */}
        <Flex gap="middle" style={{ marginBottom: 16 }} wrap="wrap">
          <Select
            placeholder="Filter by Zone"
            allowClear
            style={{ width: 220 }}
            value={selectedZoneId}
            onChange={(val) => setSelectedZoneId(val)}
            options={zones.map((z) => ({ label: `${z.zoneName} (${z.zoneCode})`, value: z.id }))}
            data-testid="zone-filter-select"
          />
          <DatePicker
            value={selectedDate}
            onChange={(date) => setSelectedDate(date)}
            allowClear
            placeholder="Filter by Date"
            data-testid="date-filter-picker"
          />
        </Flex>

        {/* Slots Table */}
        <Table
          columns={columns}
          dataSource={slots}
          rowKey="id"
          loading={isLoading}
          pagination={{ pageSize: 10 }}
          data-testid="delivery-slot-table"
        />
      </Card>

      {/* Drawer for Create / Edit / View */}
      <Drawer
        title={
          drawerMode === 'create'
            ? 'Create Delivery Slot'
            : drawerMode === 'edit'
            ? 'Edit Delivery Slot'
            : 'Delivery Slot Details'
        }
        width={560}
        open={drawerOpen}
        onClose={() => setDrawerOpen(false)}
        extra={
          drawerMode !== 'view' && (
            <Button
              type="primary"
              onClick={handleFormSubmit}
              loading={createMutation.isPending || updateMutation.isPending}
              data-testid="save-slot-button"
            >
              Save
            </Button>
          )
        }
      >
        {drawerMode === 'view' && selectedSlot ? (
          <div>
            <Descriptions column={1} bordered size="small">
              <Descriptions.Item label="Slot ID">{selectedSlot.id}</Descriptions.Item>
              <Descriptions.Item label="Delivery Zone">
                {zoneMap.get(selectedSlot.deliveryZoneId)?.zoneName || selectedSlot.deliveryZoneId}
              </Descriptions.Item>
              <Descriptions.Item label="Date">{selectedSlot.slotDate}</Descriptions.Item>
              <Descriptions.Item label="Time Window">
                {selectedSlot.startTime} - {selectedSlot.endTime}
              </Descriptions.Item>
              <Descriptions.Item label="Slot Type">
                <Tag color={getSlotTypeColor(selectedSlot.slotType)}>{selectedSlot.slotType}</Tag>
              </Descriptions.Item>
              <Descriptions.Item label="Max Capacity">{selectedSlot.maxCapacity}</Descriptions.Item>
              <Descriptions.Item label="Reserved Capacity">{selectedSlot.reservedCapacity}</Descriptions.Item>
              <Descriptions.Item label="Remaining Capacity">{selectedSlot.remainingCapacity}</Descriptions.Item>
              <Descriptions.Item label="Cutoff Time">
                {selectedSlot.cutoffTime ? dayjs(selectedSlot.cutoffTime).format('YYYY-MM-DD HH:mm:ss') : 'None'}
              </Descriptions.Item>
              <Descriptions.Item label="Buffer (Minutes)">{selectedSlot.bufferMinutes}</Descriptions.Item>
              <Descriptions.Item label="Status">{getStatusBadge(selectedSlot.status)}</Descriptions.Item>
              <Descriptions.Item label="Version">{selectedSlot.version}</Descriptions.Item>
              <Descriptions.Item label="Created At">
                {dayjs(selectedSlot.createdAt).format('YYYY-MM-DD HH:mm:ss')} by {selectedSlot.createdBy}
              </Descriptions.Item>
            </Descriptions>

            <Divider>Assigned Order Reservations</Divider>
            <Table
              size="small"
              rowKey="id"
              loading={isLoadingReservations}
              dataSource={reservations}
              pagination={false}
              columns={[
                {
                  title: 'Order ID',
                  dataIndex: 'deliveryOrderId',
                  key: 'deliveryOrderId',
                  render: (id: string) => <Text code>{id.substring(0, 8)}...</Text>,
                },
                {
                  title: 'Status',
                  dataIndex: 'status',
                  key: 'status',
                  render: (status: string) => <Tag color={status === 'ACTIVE' ? 'green' : 'default'}>{status}</Tag>,
                },
                {
                  title: 'Override',
                  key: 'override',
                  render: (_: any, r: DeliverySlotReservation) => (r.override ? <Tag color="volcano">Override</Tag> : <Text type="secondary">Normal</Text>),
                },
                {
                  title: 'Action',
                  key: 'action',
                  render: (_: any, r: DeliverySlotReservation) =>
                    r.status === 'ACTIVE' && (
                      <Popconfirm
                        title="Release reservation?"
                        onConfirm={() => releaseOrderMutation.mutate({ slotId: selectedSlot.id, orderId: r.deliveryOrderId })}
                      >
                        <Button type="link" danger size="small" icon={<DeleteOutlined />}>
                          Release
                        </Button>
                      </Popconfirm>
                    ),
                },
              ]}
            />
          </div>
        ) : (
          <Form form={form} layout="vertical">
            {drawerMode === 'create' && (
              <>
                <Form.Item
                  name="deliveryZoneId"
                  label="Delivery Zone"
                  rules={[{ required: true, message: 'Please select a delivery zone' }]}
                >
                  <Select
                    placeholder="Select delivery zone"
                    options={zones.map((z) => ({ label: `${z.zoneName} (${z.zoneCode})`, value: z.id }))}
                    data-testid="slot-zone-select"
                  />
                </Form.Item>
                <Form.Item
                  name="slotDate"
                  label="Slot Date"
                  rules={[{ required: true, message: 'Please select a date' }]}
                >
                  <DatePicker style={{ width: '100%' }} data-testid="slot-date-picker" />
                </Form.Item>
              </>
            )}

            <Form.Item
              name="timeRange"
              label="Time Window (Start - End)"
              rules={[{ required: true, message: 'Please select time window' }]}
            >
              <TimePicker.RangePicker format="HH:mm" style={{ width: '100%' }} data-testid="slot-time-range" />
            </Form.Item>

            <Form.Item
              name="slotType"
              label="Slot Type"
              rules={[{ required: true, message: 'Please select slot type' }]}
            >
              <Select
                options={[
                  { label: 'Standard', value: 'STANDARD' },
                  { label: 'Express', value: 'EXPRESS' },
                  { label: 'Same Day', value: 'SAME_DAY' },
                  { label: 'Peak Window', value: 'PEAK_WINDOW' },
                ]}
                data-testid="slot-type-select"
              />
            </Form.Item>

            <Form.Item
              name="maxCapacity"
              label="Max Capacity (Orders)"
              rules={[{ required: true, message: 'Please enter max capacity' }]}
            >
              <InputNumber min={1} max={1000} style={{ width: '100%' }} data-testid="slot-max-capacity" />
            </Form.Item>

            <Form.Item name="bufferMinutes" label="Buffer Time (Minutes)">
              <InputNumber min={0} max={120} style={{ width: '100%' }} data-testid="slot-buffer-minutes" />
            </Form.Item>

            <Form.Item name="cutoffTime" label="Cutoff Date & Time (Optional)">
              <DatePicker showTime style={{ width: '100%' }} data-testid="slot-cutoff-picker" />
            </Form.Item>
          </Form>
        )}
      </Drawer>

      {/* Modal for Assigning Order to Slot */}
      <Modal
        title="Assign Delivery Order to Slot"
        open={assignModalOpen}
        onCancel={() => setAssignModalOpen(false)}
        onOk={handleAssignSubmit}
        confirmLoading={assignOrderMutation.isPending}
        data-testid="assign-order-modal"
      >
        <Form form={assignForm} layout="vertical">
          <Form.Item
            name="deliveryOrderId"
            label="Delivery Order UUID"
            rules={[{ required: true, message: 'Please enter order UUID' }]}
          >
            <Input placeholder="UUID of delivery order" data-testid="assign-order-id-input" />
          </Form.Item>

          <Form.Item name="managerOverride" valuePropName="checked" label="Manager Override (Overbooking)">
            <Switch data-testid="assign-manager-override-switch" />
          </Form.Item>

          <Form.Item
            noStyle
            shouldUpdate={(prevValues, currentValues) => prevValues.managerOverride !== currentValues.managerOverride}
          >
            {({ getFieldValue }) =>
              getFieldValue('managerOverride') ? (
                <Form.Item
                  name="overrideReason"
                  label="Override Reason"
                  rules={[{ required: true, message: 'Reason is required when overriding capacity' }]}
                >
                  <Input.TextArea rows={2} placeholder="Explain why overbooking is authorized" data-testid="assign-override-reason-input" />
                </Form.Item>
              ) : null
            }
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
};
export default DeliverySlotListPage;
