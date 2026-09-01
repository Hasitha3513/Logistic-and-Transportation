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
  TimePicker,
  message,
  Descriptions,
  Divider,
  Badge,
  Input,
  Modal,
  Tabs,
  DatePicker,
} from 'antd';
import {
  PlusOutlined,
  CheckCircleOutlined,
  StopOutlined,
  EyeOutlined,
  EditOutlined,
  CalendarOutlined,
  UserOutlined,
  SwapOutlined,
  SearchOutlined,
} from '@ant-design/icons';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import dayjs, { Dayjs } from 'dayjs';
import { useAuth } from '../../../../auth/AuthContext';
import {
  deliveryRiderApi,
  DeliveryRider,
  DeliveryRiderStatus,
  DeliveryRiderType,
  DeliveryRiderShift,
  OnboardRiderPayload,
  UpdateRiderProfilePayload,
  ScheduleShiftPayload,
} from '../api/deliveryRiderApi';
import { deliveryZoneApi, DeliveryZone } from '../../zones/api/deliveryZoneApi';

const { Title, Text, Paragraph } = Typography;

export const DeliveryRiderListPage: React.FC = () => {
  const queryClient = useQueryClient();
  const { hasPermission } = useAuth();

  const [selectedZoneId, setSelectedZoneId] = useState<string | undefined>(undefined);
  const [selectedStatus, setSelectedStatus] = useState<DeliveryRiderStatus | undefined>(undefined);
  const [searchFilter, setSearchFilter] = useState<string>('');

  const [onboardModalOpen, setOnboardModalOpen] = useState(false);
  const [editDrawerOpen, setEditDrawerOpen] = useState(false);
  const [detailDrawerOpen, setDetailDrawerOpen] = useState(false);
  const [shiftDrawerOpen, setShiftDrawerOpen] = useState(false);

  const [selectedRider, setSelectedRider] = useState<DeliveryRider | null>(null);

  const [onboardForm] = Form.useForm<OnboardRiderPayload>();
  const [editForm] = Form.useForm<UpdateRiderProfilePayload>();
  const [shiftForm] = Form.useForm<ScheduleShiftPayload>();

  // Fetch zones for dropdowns
  const { data: zonesData } = useQuery<DeliveryZone[]>({
    queryKey: ['delivery-zones'],
    queryFn: async () => {
      return await deliveryZoneApi.list();
    },
  });
  const zones = zonesData || [];
  const zoneMap = new Map(zones.map((z) => [z.id, z.zoneName]));

  // Fetch riders
  const { data: ridersData, isLoading: ridersLoading } = useQuery<DeliveryRider[]>({
    queryKey: ['delivery-riders', selectedZoneId, selectedStatus, searchFilter],
    queryFn: async () => {
      const res = await deliveryRiderApi.getRiders({
        zoneId: selectedZoneId,
        status: selectedStatus,
        search: searchFilter || undefined,
      });
      return res.data;
    },
  });
  const riders = ridersData || [];

  // Fetch shifts for selected rider
  const { data: riderShiftsData, isLoading: shiftsLoading } = useQuery<DeliveryRiderShift[]>({
    queryKey: ['delivery-rider-shifts', selectedRider?.id],
    queryFn: async () => {
      if (!selectedRider) return [];
      const res = await deliveryRiderApi.getRiderShifts(selectedRider.id);
      return res.data;
    },
    enabled: !!selectedRider && (detailDrawerOpen || shiftDrawerOpen),
  });
  const riderShifts = riderShiftsData || [];

  // Mutations
  const onboardMutation = useMutation({
    mutationFn: (payload: OnboardRiderPayload) => deliveryRiderApi.onboardRider(payload),
    onSuccess: () => {
      message.success('Delivery rider onboarded successfully');
      setOnboardModalOpen(false);
      onboardForm.resetFields();
      queryClient.invalidateQueries({ queryKey: ['delivery-riders'] });
    },
    onError: (err: any) => {
      message.error(err.response?.data?.message || 'Failed to onboard rider');
    },
  });

  const updateMutation = useMutation({
    mutationFn: ({ id, payload }: { id: string; payload: UpdateRiderProfilePayload }) =>
      deliveryRiderApi.updateRider(id, payload),
    onSuccess: () => {
      message.success('Delivery rider updated successfully');
      setEditDrawerOpen(false);
      queryClient.invalidateQueries({ queryKey: ['delivery-riders'] });
    },
    onError: (err: any) => {
      message.error(err.response?.data?.message || 'Failed to update rider');
    },
  });

  const activateMutation = useMutation({
    mutationFn: (id: string) => deliveryRiderApi.activateRider(id),
    onSuccess: () => {
      message.success('Rider activated');
      queryClient.invalidateQueries({ queryKey: ['delivery-riders'] });
    },
    onError: (err: any) => {
      message.error(err.response?.data?.message || 'Failed to activate rider');
    },
  });

  const deactivateMutation = useMutation({
    mutationFn: (id: string) => deliveryRiderApi.deactivateRider(id),
    onSuccess: () => {
      message.success('Rider deactivated');
      queryClient.invalidateQueries({ queryKey: ['delivery-riders'] });
    },
    onError: (err: any) => {
      message.error(err.response?.data?.message || 'Failed to deactivate rider');
    },
  });

  const suspendMutation = useMutation({
    mutationFn: (id: string) => deliveryRiderApi.suspendRider(id),
    onSuccess: () => {
      message.success('Rider suspended');
      queryClient.invalidateQueries({ queryKey: ['delivery-riders'] });
    },
    onError: (err: any) => {
      message.error(err.response?.data?.message || 'Failed to suspend rider');
    },
  });

  const scheduleShiftMutation = useMutation({
    mutationFn: ({ riderId, payload }: { riderId: string; payload: ScheduleShiftPayload }) =>
      deliveryRiderApi.scheduleShift(riderId, payload),
    onSuccess: () => {
      message.success('Shift scheduled successfully');
      setShiftDrawerOpen(false);
      shiftForm.resetFields();
      queryClient.invalidateQueries({ queryKey: ['delivery-rider-shifts'] });
    },
    onError: (err: any) => {
      message.error(err.response?.data?.message || 'Failed to schedule shift');
    },
  });

  const startShiftMutation = useMutation({
    mutationFn: ({ riderId, shiftId }: { riderId: string; shiftId: string }) =>
      deliveryRiderApi.startShift(riderId, shiftId),
    onSuccess: () => {
      message.success('Shift started');
      queryClient.invalidateQueries({ queryKey: ['delivery-rider-shifts'] });
    },
    onError: (err: any) => {
      message.error(err.response?.data?.message || 'Failed to start shift');
    },
  });

  const endShiftMutation = useMutation({
    mutationFn: ({ riderId, shiftId }: { riderId: string; shiftId: string }) =>
      deliveryRiderApi.endShift(riderId, shiftId),
    onSuccess: () => {
      message.success('Shift completed');
      queryClient.invalidateQueries({ queryKey: ['delivery-rider-shifts'] });
    },
    onError: (err: any) => {
      message.error(err.response?.data?.message || 'Failed to end shift');
    },
  });

  const cancelShiftMutation = useMutation({
    mutationFn: ({ riderId, shiftId }: { riderId: string; shiftId: string }) =>
      deliveryRiderApi.cancelShift(riderId, shiftId),
    onSuccess: () => {
      message.success('Shift cancelled');
      queryClient.invalidateQueries({ queryKey: ['delivery-rider-shifts'] });
    },
    onError: (err: any) => {
      message.error(err.response?.data?.message || 'Failed to cancel shift');
    },
  });

  const columns = [
    {
      title: 'Rider Code',
      dataIndex: 'riderCode',
      key: 'riderCode',
      render: (text: string) => <Text strong>{text}</Text>,
    },
    {
      title: 'Driver ID',
      dataIndex: 'driverId',
      key: 'driverId',
      render: (id: string) => <Text copyable={{ text: id }}>{id.substring(0, 8)}...</Text>,
    },
    {
      title: 'Type',
      dataIndex: 'riderType',
      key: 'riderType',
      render: (type: DeliveryRiderType) => {
        const color = type === 'FULL_TIME' ? 'blue' : type === 'PART_TIME' ? 'cyan' : type === 'CONTRACTOR' ? 'purple' : 'gold';
        return <Tag color={color}>{type}</Tag>;
      },
    },
    {
      title: 'Primary Zone',
      dataIndex: 'primaryZoneId',
      key: 'primaryZoneId',
      render: (zoneId: string) => zoneMap.get(zoneId) || <Text type="secondary">{zoneId.substring(0, 8)}...</Text>,
    },
    {
      title: 'Max Deliveries',
      dataIndex: 'maxConcurrentDeliveries',
      key: 'maxConcurrentDeliveries',
      render: (val: number) => <Tag color="geekblue">{val} concurrent</Tag>,
    },
    {
      title: 'Status',
      dataIndex: 'status',
      key: 'status',
      render: (status: DeliveryRiderStatus) => {
        const color = status === 'ACTIVE' ? 'success' : status === 'SUSPENDED' ? 'error' : 'default';
        return <Badge status={color as any} text={status} />;
      },
    },
    {
      title: 'Actions',
      key: 'actions',
      render: (_: any, record: DeliveryRider) => (
        <Space size="small">
          <Button
            type="text"
            icon={<EyeOutlined />}
            onClick={() => {
              setSelectedRider(record);
              setDetailDrawerOpen(true);
            }}
          />
          {hasPermission('DELIVERY_RIDER_UPDATE') && (
            <Button
              type="text"
              icon={<EditOutlined />}
              onClick={() => {
                setSelectedRider(record);
                editForm.setFieldsValue({
                  riderType: record.riderType,
                  transportMode: record.transportMode ?? undefined,
                  primaryZoneId: record.primaryZoneId,
                  secondaryZoneIds: record.secondaryZoneIds,
                  maxConcurrentDeliveries: record.maxConcurrentDeliveries,
                  expectedVersion: record.version,
                });
                setEditDrawerOpen(true);
              }}
            />
          )}
          {hasPermission('DELIVERY_RIDER_ACTIVATE') && record.status !== 'ACTIVE' && (
            <Button
              type="link"
              size="small"
              icon={<CheckCircleOutlined />}
              onClick={() => activateMutation.mutate(record.id)}
            >
              Activate
            </Button>
          )}
          {hasPermission('DELIVERY_RIDER_ACTIVATE') && record.status === 'ACTIVE' && (
            <Button
              type="link"
              size="small"
              danger
              icon={<StopOutlined />}
              onClick={() => deactivateMutation.mutate(record.id)}
            >
              Deactivate
            </Button>
          )}
          <Button
            type="link"
            size="small"
            icon={<CalendarOutlined />}
            onClick={() => {
              setSelectedRider(record);
              setShiftDrawerOpen(true);
            }}
          >
            Shifts
          </Button>
        </Space>
      ),
    },
  ];

  const shiftColumns = [
    {
      title: 'Shift Date',
      dataIndex: 'shiftDate',
      key: 'shiftDate',
    },
    {
      title: 'Time Window',
      key: 'window',
      render: (_: any, r: DeliveryRiderShift) => `${r.startTime} - ${r.endTime}`,
    },
    {
      title: 'Capacity',
      dataIndex: 'maxCapacity',
      key: 'maxCapacity',
    },
    {
      title: 'Status',
      dataIndex: 'status',
      key: 'status',
      render: (st: string) => {
        const color = st === 'IN_PROGRESS' ? 'processing' : st === 'COMPLETED' ? 'success' : st === 'CANCELLED' ? 'default' : 'warning';
        return <Tag color={color}>{st}</Tag>;
      },
    },
    {
      title: 'Shift Actions',
      key: 'actions',
      render: (_: any, r: DeliveryRiderShift) => (
        <Space size="small">
          {r.status === 'SCHEDULED' && (
            <>
              <Button
                type="link"
                size="small"
                onClick={() => startShiftMutation.mutate({ riderId: r.riderId, shiftId: r.id })}
              >
                Start Duty
              </Button>
              <Button
                type="link"
                size="small"
                danger
                onClick={() => cancelShiftMutation.mutate({ riderId: r.riderId, shiftId: r.id })}
              >
                Cancel
              </Button>
            </>
          )}
          {r.status === 'IN_PROGRESS' && (
            <Button
              type="link"
              size="small"
              onClick={() => endShiftMutation.mutate({ riderId: r.riderId, shiftId: r.id })}
            >
              End Duty
            </Button>
          )}
        </Space>
      ),
    },
  ];

  return (
    <Card bordered={false} style={{ margin: 16 }}>
      <Flex justify="space-between" align="center" style={{ marginBottom: 20 }}>
        <div>
          <Title level={4} style={{ margin: 0 }}>
            Delivery Riders Roster & Duty Scheduling (US-65)
          </Title>
          <Text type="secondary">
            Manage delivery rider profiles, zones, capacity limits, and active shifts.
          </Text>
        </div>
        {hasPermission('DELIVERY_RIDER_CREATE') && (
          <Button
            type="primary"
            icon={<PlusOutlined />}
            onClick={() => setOnboardModalOpen(true)}
          >
            Onboard Rider
          </Button>
        )}
      </Flex>

      <Card size="small" style={{ marginBottom: 16 }}>
        <Flex gap="middle" wrap="wrap" align="center">
          <Select
            placeholder="Filter by Zone"
            allowClear
            style={{ width: 220 }}
            value={selectedZoneId}
            onChange={setSelectedZoneId}
            options={zones.map((z) => ({ label: z.zoneName, value: z.id }))}
          />
          <Select
            placeholder="Filter by Status"
            allowClear
            style={{ width: 160 }}
            value={selectedStatus}
            onChange={setSelectedStatus}
            options={[
              { label: 'ACTIVE', value: 'ACTIVE' },
              { label: 'INACTIVE', value: 'INACTIVE' },
              { label: 'SUSPENDED', value: 'SUSPENDED' },
            ]}
          />
          <Input
            placeholder="Search code or driver ID"
            prefix={<SearchOutlined />}
            style={{ width: 240 }}
            value={searchFilter}
            onChange={(e) => setSearchFilter(e.target.value)}
          />
        </Flex>
      </Card>

      <Table
        dataSource={riders}
        columns={columns}
        rowKey="id"
        loading={ridersLoading}
        pagination={{ defaultPageSize: 10, showSizeChanger: true }}
      />

      {/* Onboard Rider Modal */}
      <Modal
        title="Onboard New Delivery Rider"
        open={onboardModalOpen}
        onCancel={() => setOnboardModalOpen(false)}
        footer={null}
        destroyOnClose
      >
        <Form
          form={onboardForm}
          layout="vertical"
          onFinish={(values) => onboardMutation.mutate(values)}
          initialValues={{
            riderType: 'FULL_TIME',
            maxConcurrentDeliveries: 3,
          }}
        >
          <Form.Item
            name="driverId"
            label="Driver ID (UUID)"
            rules={[{ required: true, message: 'Driver ID is required' }]}
          >
            <Input placeholder="Enter eligible Fleet Driver UUID" />
          </Form.Item>

          <Form.Item name="riderCode" label="Rider Code (Optional)">
            <Input placeholder="Leave blank to auto-generate" />
          </Form.Item>

          <Form.Item
            name="riderType"
            label="Rider Type"
            rules={[{ required: true, message: 'Rider type is required' }]}
          >
            <Select
              options={[
                { label: 'Full Time', value: 'FULL_TIME' },
                { label: 'Part Time', value: 'PART_TIME' },
                { label: 'Contractor', value: 'CONTRACTOR' },
                { label: 'Gig Worker', value: 'GIG' },
              ]}
            />
          </Form.Item>

          <Form.Item
            name="transportMode"
            label="Transport Mode"
            rules={[{ required: true, message: 'Transport mode is required' }]}
          >
            <Select
              placeholder="Select transport mode"
              options={[
                { label: 'Bicycle', value: 'BICYCLE' },
                { label: 'Motorbike', value: 'MOTORBIKE' },
                { label: 'Van', value: 'VAN' },
                { label: 'Car', value: 'CAR' },
                { label: 'Walker', value: 'WALKER' },
              ]}
            />
          </Form.Item>

          <Form.Item
            name="primaryZoneId"
            label="Primary Zone"
            rules={[{ required: true, message: 'Primary zone is required' }]}
          >
            <Select
              placeholder="Select primary delivery zone"
              options={zones.map((z) => ({ label: z.zoneName, value: z.id }))}
            />
          </Form.Item>

          <Form.Item name="secondaryZoneIds" label="Secondary Zones">
            <Select
              mode="multiple"
              placeholder="Select secondary zones (optional)"
              options={zones.map((z) => ({ label: z.zoneName, value: z.id }))}
            />
          </Form.Item>

          <Form.Item
            name="maxConcurrentDeliveries"
            label="Max Concurrent Deliveries (Capacity Limit)"
            rules={[{ required: true, message: 'Capacity limit is required' }]}
          >
            <InputNumber min={1} max={20} style={{ width: '100%' }} />
          </Form.Item>

          <Flex justify="flex-end" gap="small" style={{ marginTop: 20 }}>
            <Button onClick={() => setOnboardModalOpen(false)}>Cancel</Button>
            <Button type="primary" htmlType="submit" loading={onboardMutation.isPending}>
              Onboard Rider
            </Button>
          </Flex>
        </Form>
      </Modal>

      {/* Edit Profile Drawer */}
      <Drawer
        title={`Edit Rider Profile: ${selectedRider?.riderCode}`}
        open={editDrawerOpen}
        onClose={() => setEditDrawerOpen(false)}
        width={400}
        destroyOnClose
      >
        <Form
          form={editForm}
          layout="vertical"
          onFinish={(values) => {
            if (selectedRider) {
              updateMutation.mutate({
                id: selectedRider.id,
                payload: { ...values, expectedVersion: selectedRider.version },
              });
            }
          }}
        >
          <Form.Item name="riderType" label="Rider Type">
            <Select
              options={[
                { label: 'Full Time', value: 'FULL_TIME' },
                { label: 'Part Time', value: 'PART_TIME' },
                { label: 'Contractor', value: 'CONTRACTOR' },
                { label: 'Gig Worker', value: 'GIG' },
              ]}
            />
          </Form.Item>

          <Form.Item
            name="transportMode"
            label="Transport Mode"
            rules={[{ required: true, message: 'Transport mode is required' }]}
          >
            <Select
              placeholder="Transport mode is not configured"
              options={[
                { label: 'Bicycle', value: 'BICYCLE' },
                { label: 'Motorbike', value: 'MOTORBIKE' },
                { label: 'Van', value: 'VAN' },
                { label: 'Car', value: 'CAR' },
                { label: 'Walker', value: 'WALKER' },
              ]}
            />
          </Form.Item>

          <Form.Item name="primaryZoneId" label="Primary Zone">
            <Select
              options={zones.map((z) => ({ label: z.zoneName, value: z.id }))}
            />
          </Form.Item>

          <Form.Item name="secondaryZoneIds" label="Secondary Zones">
            <Select
              mode="multiple"
              options={zones.map((z) => ({ label: z.zoneName, value: z.id }))}
            />
          </Form.Item>

          <Form.Item name="maxConcurrentDeliveries" label="Max Concurrent Deliveries">
            <InputNumber min={1} max={20} style={{ width: '100%' }} />
          </Form.Item>

          <Flex justify="flex-end" gap="small" style={{ marginTop: 20 }}>
            <Button onClick={() => setEditDrawerOpen(false)}>Cancel</Button>
            <Button type="primary" htmlType="submit" loading={updateMutation.isPending}>
              Save Changes
            </Button>
          </Flex>
        </Form>
      </Drawer>

      {/* Rider Shifts Drawer */}
      <Drawer
        title={`Shifts & Duty Schedule: ${selectedRider?.riderCode}`}
        open={shiftDrawerOpen}
        onClose={() => setShiftDrawerOpen(false)}
        width={650}
      >
        <Card size="small" title="Schedule New Shift" style={{ marginBottom: 16 }}>
          <Form
            form={shiftForm}
            layout="vertical"
            onFinish={(values: any) => {
              if (selectedRider) {
                scheduleShiftMutation.mutate({
                  riderId: selectedRider.id,
                  payload: {
                    shiftDate: values.shiftDate.format('YYYY-MM-DD'),
                    startTime: values.times[0].format('HH:mm:ss'),
                    endTime: values.times[1].format('HH:mm:ss'),
                    maxCapacity: values.maxCapacity,
                  },
                });
              }
            }}
            initialValues={{
              shiftDate: dayjs(),
              maxCapacity: selectedRider?.maxConcurrentDeliveries || 5,
            }}
          >
            <Flex gap="middle">
              <Form.Item
                name="shiftDate"
                label="Date"
                rules={[{ required: true }]}
                style={{ flex: 1 }}
              >
                <DatePicker style={{ width: '100%' }} />
              </Form.Item>
              <Form.Item
                name="times"
                label="Window"
                rules={[{ required: true }]}
                style={{ flex: 1.5 }}
              >
                <TimePicker.RangePicker format="HH:mm" style={{ width: '100%' }} />
              </Form.Item>
              <Form.Item
                name="maxCapacity"
                label="Max Orders"
                rules={[{ required: true }]}
                style={{ flex: 1 }}
              >
                <InputNumber min={1} max={20} style={{ width: '100%' }} />
              </Form.Item>
            </Flex>
            <Button
              type="primary"
              htmlType="submit"
              loading={scheduleShiftMutation.isPending}
            >
              Add Shift
            </Button>
          </Form>
        </Card>

        <Table
          dataSource={riderShifts}
          columns={shiftColumns}
          rowKey="id"
          loading={shiftsLoading}
          pagination={false}
          size="small"
        />
      </Drawer>

      {/* Rider Detail Drawer */}
      <Drawer
        title={`Rider Details: ${selectedRider?.riderCode}`}
        open={detailDrawerOpen}
        onClose={() => setDetailDrawerOpen(false)}
        width={500}
      >
        {selectedRider && (
          <Descriptions column={1} bordered size="small">
            <Descriptions.Item label="Rider Code">{selectedRider.riderCode}</Descriptions.Item>
            <Descriptions.Item label="Driver ID">{selectedRider.driverId}</Descriptions.Item>
            <Descriptions.Item label="Rider Type">
              <Tag color="blue">{selectedRider.riderType}</Tag>
            </Descriptions.Item>
            <Descriptions.Item label="Transport Mode">
              {selectedRider.transportMode ? <Tag color="purple">{selectedRider.transportMode}</Tag> : <Text type="warning">Not configured</Text>}
            </Descriptions.Item>
            <Descriptions.Item label="Primary Zone">
              {zoneMap.get(selectedRider.primaryZoneId) || selectedRider.primaryZoneId}
            </Descriptions.Item>
            <Descriptions.Item label="Secondary Zones">
              {selectedRider.secondaryZoneIds.map((zid) => (
                <Tag key={zid}>{zoneMap.get(zid) || zid.substring(0, 8)}</Tag>
              ))}
            </Descriptions.Item>
            <Descriptions.Item label="Max Concurrent Deliveries">
              {selectedRider.maxConcurrentDeliveries}
            </Descriptions.Item>
            <Descriptions.Item label="Status">
              <Badge
                status={selectedRider.status === 'ACTIVE' ? 'success' : 'error'}
                text={selectedRider.status}
              />
            </Descriptions.Item>
            <Descriptions.Item label="Created At">
              {dayjs(selectedRider.createdAt).format('YYYY-MM-DD HH:mm')}
            </Descriptions.Item>
            <Descriptions.Item label="Created By">{selectedRider.createdBy}</Descriptions.Item>
            <Descriptions.Item label="Version">{selectedRider.version}</Descriptions.Item>
          </Descriptions>
        )}
      </Drawer>
    </Card>
  );
};
