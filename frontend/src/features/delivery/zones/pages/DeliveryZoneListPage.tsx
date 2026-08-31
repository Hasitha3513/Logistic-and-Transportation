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
  Input,
  Drawer,
  Form,
  InputNumber,
  Switch,
  message,
  Popconfirm,
  Descriptions,
  Divider,
} from 'antd';
import {
  PlusOutlined,
  CheckCircleOutlined,
  StopOutlined,
  EyeOutlined,
  EditOutlined,
  SearchOutlined,
} from '@ant-design/icons';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useAuth } from '../../../../auth/AuthContext';
import {
  deliveryZoneApi,
  DeliveryZone,
  DeliveryZoneStatus,
  DeliveryZoneType,
  CreateDeliveryZonePayload,
  UpdateDeliveryZonePayload,
  DeliveryZoneCoordinate,
} from '../api/deliveryZoneApi';

const { Title, Text, Paragraph } = Typography;

export const DeliveryZoneListPage: React.FC = () => {
  const queryClient = useQueryClient();
  const { hasPermission } = useAuth();

  const [statusFilter, setStatusFilter] = useState<DeliveryZoneStatus | undefined>(undefined);
  const [serviceableFilter, setServiceableFilter] = useState<boolean | undefined>(undefined);
  const [searchTerm, setSearchTerm] = useState('');

  const [drawerOpen, setDrawerOpen] = useState(false);
  const [drawerMode, setDrawerMode] = useState<'create' | 'edit' | 'view'>('create');
  const [selectedZone, setSelectedZone] = useState<DeliveryZone | null>(null);

  const [form] = Form.useForm();

  const { data: zones = [], isLoading } = useQuery({
    queryKey: ['delivery-zones', statusFilter, serviceableFilter],
    queryFn: () => deliveryZoneApi.list(statusFilter, serviceableFilter),
  });

  const createMutation = useMutation({
    mutationFn: (payload: CreateDeliveryZonePayload) => deliveryZoneApi.create(payload),
    onSuccess: () => {
      void message.success('Delivery zone created successfully');
      void queryClient.invalidateQueries({ queryKey: ['delivery-zones'] });
      setDrawerOpen(false);
    },
    onError: (err: any) => {
      void message.error(err.response?.data?.message || 'Failed to create delivery zone');
    },
  });

  const updateMutation = useMutation({
    mutationFn: ({ id, payload }: { id: string; payload: UpdateDeliveryZonePayload }) =>
      deliveryZoneApi.update(id, payload),
    onSuccess: () => {
      void message.success('Delivery zone updated successfully');
      void queryClient.invalidateQueries({ queryKey: ['delivery-zones'] });
      setDrawerOpen(false);
    },
    onError: (err: any) => {
      void message.error(err.response?.data?.message || 'Failed to update delivery zone');
    },
  });

  const activateMutation = useMutation({
    mutationFn: (id: string) => deliveryZoneApi.activate(id),
    onSuccess: () => {
      void message.success('Delivery zone activated');
      void queryClient.invalidateQueries({ queryKey: ['delivery-zones'] });
    },
    onError: (err: any) => {
      void message.error(err.response?.data?.message || 'Failed to activate zone');
    },
  });

  const deactivateMutation = useMutation({
    mutationFn: (id: string) => deliveryZoneApi.deactivate(id),
    onSuccess: () => {
      void message.success('Delivery zone deactivated');
      void queryClient.invalidateQueries({ queryKey: ['delivery-zones'] });
    },
    onError: (err: any) => {
      void message.error(err.response?.data?.message || 'Failed to deactivate zone');
    },
  });

  const handleOpenCreate = () => {
    form.resetFields();
    form.setFieldsValue({
      zoneType: 'URBAN_DENSE',
      serviceable: true,
      priority: 0,
      coordinatesJson: '[\n  {"longitude": 79.8450, "latitude": 6.9271},\n  {"longitude": 79.8600, "latitude": 6.9271},\n  {"longitude": 79.8600, "latitude": 6.9400},\n  {"longitude": 79.8450, "latitude": 6.9400},\n  {"longitude": 79.8450, "latitude": 6.9271}\n]',
    });
    setDrawerMode('create');
    setSelectedZone(null);
    setDrawerOpen(true);
  };

  const handleOpenEdit = (zone: DeliveryZone) => {
    setSelectedZone(zone);
    form.resetFields();
    form.setFieldsValue({
      zoneCode: zone.zoneCode,
      zoneName: zone.zoneName,
      description: zone.description,
      zoneType: zone.zoneType,
      serviceable: zone.serviceable,
      dailyCapacity: zone.dailyCapacity,
      priority: zone.priority,
      coordinatesJson: JSON.stringify(zone.coordinates, null, 2),
    });
    setDrawerMode('edit');
    setDrawerOpen(true);
  };

  const handleOpenView = (zone: DeliveryZone) => {
    setSelectedZone(zone);
    setDrawerMode('view');
    setDrawerOpen(true);
  };

  const handleFormSubmit = async (values: any) => {
    let coordinates: DeliveryZoneCoordinate[];
    try {
      coordinates = JSON.parse(values.coordinatesJson);
      if (!Array.isArray(coordinates) || coordinates.length < 4) {
        throw new Error('Coordinates must be an array of at least 4 coordinates (closed polygon)');
      }
    } catch (e: any) {
      void message.error('Invalid GeoJSON coordinates format: ' + e.message);
      return;
    }

    if (drawerMode === 'create') {
      createMutation.mutate({
        zoneCode: values.zoneCode,
        zoneName: values.zoneName,
        description: values.description,
        zoneType: values.zoneType,
        serviceable: values.serviceable,
        dailyCapacity: values.dailyCapacity,
        depotLocationId: values.depotLocationId,
        coordinates,
        priority: values.priority,
      });
    } else if (drawerMode === 'edit' && selectedZone) {
      updateMutation.mutate({
        id: selectedZone.id,
        payload: {
          zoneName: values.zoneName,
          description: values.description,
          zoneType: values.zoneType,
          serviceable: values.serviceable,
          dailyCapacity: values.dailyCapacity,
          depotLocationId: values.depotLocationId,
          coordinates,
          priority: values.priority,
          expectedVersion: selectedZone.version,
        },
      });
    }
  };

  const filteredZones = zones.filter((z) => {
    if (!searchTerm) return true;
    const term = searchTerm.toLowerCase();
    return (
      z.zoneCode.toLowerCase().includes(term) ||
      z.zoneName.toLowerCase().includes(term) ||
      (z.description && z.description.toLowerCase().includes(term))
    );
  });

  const columns = [
    {
      title: 'Zone Code',
      dataIndex: 'zoneCode',
      key: 'zoneCode',
      render: (code: string, record: DeliveryZone) => (
        <Button type="link" onClick={() => handleOpenView(record)} style={{ padding: 0 }}>
          {code}
        </Button>
      ),
    },
    {
      title: 'Zone Name',
      dataIndex: 'zoneName',
      key: 'zoneName',
    },
    {
      title: 'Type',
      dataIndex: 'zoneType',
      key: 'zoneType',
      render: (type: DeliveryZoneType) => {
        const colorMap: Record<DeliveryZoneType, string> = {
          URBAN_DENSE: 'purple',
          SUBURBAN: 'blue',
          RURAL: 'green',
          SPECIAL_SECURITY: 'volcano',
        };
        return <Tag color={colorMap[type]}>{type.replace('_', ' ')}</Tag>;
      },
    },
    {
      title: 'Priority',
      dataIndex: 'priority',
      key: 'priority',
      sorter: (a: DeliveryZone, b: DeliveryZone) => a.priority - b.priority,
    },
    {
      title: 'Status',
      dataIndex: 'status',
      key: 'status',
      render: (status: DeliveryZoneStatus) => (
        <Tag color={status === 'ACTIVE' ? 'success' : 'default'}>{status}</Tag>
      ),
    },
    {
      title: 'Serviceable',
      dataIndex: 'serviceable',
      key: 'serviceable',
      render: (serviceable: boolean) => (
        <Tag color={serviceable ? 'green' : 'red'}>{serviceable ? 'YES' : 'NO'}</Tag>
      ),
    },
    {
      title: 'Capacity',
      dataIndex: 'dailyCapacity',
      key: 'dailyCapacity',
      render: (cap: number | undefined) => (cap !== null && cap !== undefined ? `${cap} orders/day` : 'Unlimited'),
    },
    {
      title: 'Actions',
      key: 'actions',
      render: (_: any, record: DeliveryZone) => (
        <Space size="small">
          <Button
            size="small"
            icon={<EyeOutlined />}
            onClick={() => handleOpenView(record)}
          />
          {hasPermission('DELIVERY_ZONE_UPDATE') && (
            <Button
              size="small"
              icon={<EditOutlined />}
              onClick={() => handleOpenEdit(record)}
            />
          )}
          {hasPermission('DELIVERY_ZONE_ACTIVATE') && record.status === 'ACTIVE' && (
            <Popconfirm
              title="Deactivate zone"
              description="Are you sure you want to deactivate this delivery zone?"
              onConfirm={() => deactivateMutation.mutate(record.id)}
            >
              <Button size="small" danger icon={<StopOutlined />} />
            </Popconfirm>
          )}
          {hasPermission('DELIVERY_ZONE_ACTIVATE') && record.status === 'INACTIVE' && (
            <Button
              size="small"
              type="primary"
              ghost
              icon={<CheckCircleOutlined />}
              onClick={() => activateMutation.mutate(record.id)}
            />
          )}
        </Space>
      ),
    },
  ];

  return (
    <div style={{ padding: 24 }}>
      <Flex justify="space-between" align="center" style={{ marginBottom: 16 }}>
        <div>
          <Title level={3} style={{ margin: 0 }}>
            Delivery Zones
          </Title>
          <Text type="secondary">
            Manage polygon-bounded delivery territories, priority resolution, and serviceability.
          </Text>
        </div>
        {hasPermission('DELIVERY_ZONE_CREATE') && (
          <Button type="primary" icon={<PlusOutlined />} onClick={handleOpenCreate}>
            Create Delivery Zone
          </Button>
        )}
      </Flex>

      <Card style={{ marginBottom: 16 }}>
        <Space orientation="horizontal" size="middle" wrap>
          <Input
            placeholder="Search zones..."
            prefix={<SearchOutlined />}
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            style={{ width: 220 }}
          />
          <Select
            placeholder="Status"
            allowClear
            value={statusFilter}
            onChange={(val) => setStatusFilter(val)}
            style={{ width: 140 }}
            options={[
              { label: 'ACTIVE', value: 'ACTIVE' },
              { label: 'INACTIVE', value: 'INACTIVE' },
            ]}
          />
          <Select
            placeholder="Serviceability"
            allowClear
            value={serviceableFilter}
            onChange={(val) => setServiceableFilter(val)}
            style={{ width: 150 }}
            options={[
              { label: 'Serviceable', value: true },
              { label: 'Non-Serviceable', value: false },
            ]}
          />
        </Space>
      </Card>

      <Table
        dataSource={filteredZones}
        columns={columns}
        rowKey="id"
        loading={isLoading}
        pagination={{ pageSize: 10 }}
      />

      <Drawer
        title={
          drawerMode === 'create'
            ? 'Create Delivery Zone'
            : drawerMode === 'edit'
            ? `Edit Delivery Zone: ${selectedZone?.zoneCode}`
            : `Delivery Zone Details: ${selectedZone?.zoneCode}`
        }
        width={600}
        open={drawerOpen}
        onClose={() => setDrawerOpen(false)}
        extra={
          drawerMode !== 'view' && (
            <Space>
              <Button onClick={() => setDrawerOpen(false)}>Cancel</Button>
              <Button
                type="primary"
                onClick={() => form.submit()}
                loading={createMutation.isPending || updateMutation.isPending}
              >
                Save
              </Button>
            </Space>
          )
        }
      >
        {drawerMode === 'view' && selectedZone ? (
          <div>
            <Descriptions bordered column={1} size="small">
              <Descriptions.Item label="Zone Code">{selectedZone.zoneCode}</Descriptions.Item>
              <Descriptions.Item label="Zone Name">{selectedZone.zoneName}</Descriptions.Item>
              <Descriptions.Item label="Description">{selectedZone.description || '-'}</Descriptions.Item>
              <Descriptions.Item label="Type">
                <Tag>{selectedZone.zoneType}</Tag>
              </Descriptions.Item>
              <Descriptions.Item label="Status">
                <Tag color={selectedZone.status === 'ACTIVE' ? 'success' : 'default'}>
                  {selectedZone.status}
                </Tag>
              </Descriptions.Item>
              <Descriptions.Item label="Serviceable">
                <Tag color={selectedZone.serviceable ? 'green' : 'red'}>
                  {selectedZone.serviceable ? 'YES' : 'NO'}
                </Tag>
              </Descriptions.Item>
              <Descriptions.Item label="Priority">{selectedZone.priority}</Descriptions.Item>
              <Descriptions.Item label="Daily Capacity">
                {selectedZone.dailyCapacity ? `${selectedZone.dailyCapacity} orders` : 'Unlimited'}
              </Descriptions.Item>
              <Descriptions.Item label="Bounding Box (Lat, Lon)">
                [{selectedZone.minLatitude.toFixed(4)}, {selectedZone.maxLatitude.toFixed(4)}] × [
                {selectedZone.minLongitude.toFixed(4)}, {selectedZone.maxLongitude.toFixed(4)}]
              </Descriptions.Item>
              <Descriptions.Item label="Approximate Area">
                {selectedZone.approximateArea.toFixed(6)} sq deg
              </Descriptions.Item>
              <Descriptions.Item label="Version">{selectedZone.version}</Descriptions.Item>
              <Descriptions.Item label="Created By">{selectedZone.createdBy}</Descriptions.Item>
              <Descriptions.Item label="Created At">{selectedZone.createdAt}</Descriptions.Item>
            </Descriptions>

            <Divider>Boundary Coordinates ({selectedZone.coordinates.length} vertices)</Divider>
            <Paragraph>
              <pre style={{ maxHeight: 200, overflow: 'auto', background: '#f5f5f5', padding: 12, borderRadius: 6 }}>
                {JSON.stringify(selectedZone.coordinates, null, 2)}
              </pre>
            </Paragraph>
          </div>
        ) : (
          <Form form={form} layout="vertical" onFinish={handleFormSubmit}>
            {drawerMode === 'create' && (
              <Form.Item
                name="zoneCode"
                label="Zone Code"
                rules={[{ required: true, message: 'Please enter zone code' }]}
              >
                <Input placeholder="e.g. ZONE-COL-01" />
              </Form.Item>
            )}

            <Form.Item
              name="zoneName"
              label="Zone Name"
              rules={[{ required: true, message: 'Please enter zone name' }]}
            >
              <Input placeholder="e.g. Colombo Central Commercial" />
            </Form.Item>

            <Form.Item name="description" label="Description">
              <Input.TextArea rows={2} placeholder="Optional operational notes..." />
            </Form.Item>

            <Form.Item
              name="zoneType"
              label="Zone Type"
              rules={[{ required: true, message: 'Please select zone type' }]}
            >
              <Select
                options={[
                  { label: 'Urban Dense', value: 'URBAN_DENSE' },
                  { label: 'Suburban', value: 'SUBURBAN' },
                  { label: 'Rural', value: 'RURAL' },
                  { label: 'Special Security', value: 'SPECIAL_SECURITY' },
                ]}
              />
            </Form.Item>

            <Form.Item name="priority" label="Priority (Higher number takes precedence in overlap)">
              <InputNumber style={{ width: '100%' }} min={0} max={999} />
            </Form.Item>

            <Form.Item name="dailyCapacity" label="Daily Capacity Limit (Optional)">
              <InputNumber style={{ width: '100%' }} min={0} placeholder="Leave blank for unlimited" />
            </Form.Item>

            <Form.Item name="serviceable" label="Serviceable" valuePropName="checked">
              <Switch checkedChildren="Serviceable" unCheckedChildren="Restricted" />
            </Form.Item>

            <Form.Item
              name="coordinatesJson"
              label="Polygon Coordinates (JSON Array of {longitude, latitude})"
              rules={[{ required: true, message: 'Please enter polygon coordinates' }]}
              extra="Must be a closed ring where first and last coordinates match (minimum 4 coordinates)."
            >
              <Input.TextArea rows={8} style={{ fontFamily: 'monospace' }} />
            </Form.Item>
          </Form>
        )}
      </Drawer>
    </div>
  );
};

export default DeliveryZoneListPage;
