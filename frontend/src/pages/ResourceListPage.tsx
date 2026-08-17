import { DeleteOutlined, EditOutlined, EyeOutlined, PlusOutlined, ReloadOutlined, SafetyCertificateOutlined } from '@ant-design/icons';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { Alert, App as AntApp, Button, Card, Descriptions, Drawer, Flex, Space, Spin, Table, Tag, Typography, type TableColumnsType } from 'antd';
import { api } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import VehicleReadingsSection from '../fleet/VehicleReadingsSection';
import ResourceEditorModal, { type ResourceField, type ResourceValues } from './ResourceEditorModal';

type ResourceRecord = Record<string, unknown> & { id: string };

interface ResourceListPageProps {
  endpoint: string;
  queryKey: string;
  title: string;
  description: string;
  columns: TableColumnsType<ResourceRecord>;
  controlPermissions?: string[];
  createPermission?: string;
  updatePermission?: string;
  deactivatePermission?: string;
  fields?: ResourceField[];
  related?: { title: string; permission: string; fields: ResourceField[] };
}

interface EditorState {
  title: string;
  endpoint: string;
  method: 'post' | 'put' | 'patch';
  fields: ResourceField[];
  initial?: ResourceValues;
  queryKey: string;
}

const text = (value: unknown) => value == null || value === '' ? '—' : String(value);
const shortId = (value: unknown) => typeof value === 'string' ? value.slice(0, 8).toUpperCase() : '—';
const state = (value: unknown) => <Tag color={value === true ? 'success' : 'default'}>{value === true ? 'Active' : 'Inactive'}</Tag>;
const status = (value: unknown) => {
  const label = text(value).replaceAll('_', ' ');
  const color = ['AVAILABLE', 'ACTIVE'].includes(String(value)) ? 'success' : ['OUT_OF_SERVICE', 'INACTIVE'].includes(String(value)) ? 'error' : 'processing';
  return <Tag color={color}>{label}</Tag>;
};

const fieldLabel = (key: string) => key
  .replace(/([a-z0-9])([A-Z])/g, '$1 $2')
  .replaceAll('_', ' ')
  .replace(/^./, (letter) => letter.toUpperCase());

function detailValue(value: unknown) {
  if (value == null || value === '') return '—';
  if (typeof value === 'boolean') return state(value);
  if (Array.isArray(value)) {
    if (!value.length) return '—';
    return <Space size={[4, 6]} wrap>{value.map((item) => <Tag key={String(item)}>{String(item).replaceAll('_', ' ')}</Tag>)}</Space>;
  }
  if (typeof value === 'object') return <Typography.Text code>{JSON.stringify(value)}</Typography.Text>;
  return String(value);
}

export default function ResourceListPage({ endpoint, queryKey, title, description, columns, controlPermissions = [],
  createPermission, updatePermission, deactivatePermission, fields = [], related: relatedConfig }: ResourceListPageProps) {
  const { message, modal } = AntApp.useApp();
  const { hasPermission } = useAuth();
  const queryClient = useQueryClient();
  const [selected, setSelected] = useState<ResourceRecord>();
  const [editor, setEditor] = useState<EditorState>();
  const resources = useQuery({
    queryKey: [queryKey],
    queryFn: async () => (await api.get<ResourceRecord[]>(endpoint)).data,
  });
  const details = useQuery({
    queryKey: [queryKey, selected?.id],
    queryFn: async () => (await api.get<ResourceRecord>(`${endpoint}/${selected?.id}`)).data,
    enabled: Boolean(selected?.id),
  });
  const relatedEndpoint = endpoint === '/vehicles'
    ? `${endpoint}/${selected?.id}/documents`
    : endpoint === '/drivers' ? `${endpoint}/${selected?.id}/licenses` : undefined;
  const related = useQuery({
    queryKey: [queryKey, selected?.id, 'compliance'],
    queryFn: async () => (await api.get<ResourceRecord[]>(relatedEndpoint!)).data,
    enabled: Boolean(selected?.id && relatedEndpoint),
  });
  const fullControl = controlPermissions.length > 0 && controlPermissions.every(hasPermission);
  const detail = details.data ?? selected;
  const openCreate = () => setEditor({ title: `Create ${title}`, endpoint, method: 'post', fields, queryKey });
  const openEdit = (record: ResourceRecord) => setEditor({
    title: `Edit ${title}`, endpoint: `${endpoint}/${record.id}`, method: 'put', fields,
    initial: { ...record, stops: record.stopLocationIds ?? record.stops }, queryKey,
  });
  const deactivate = (record: ResourceRecord) => modal.confirm({
    title: `Deactivate ${title}`,
    content: 'This action preserves history but removes the record from operational use.',
    okText: 'Deactivate', okButtonProps: { danger: true },
    onOk: async () => {
      await api.delete(`${endpoint}/${record.id}`);
      await queryClient.invalidateQueries({ queryKey: [queryKey] });
      setSelected(undefined);
      void message.success(`${title} deactivated`);
    },
  });
  const tableColumns: TableColumnsType<ResourceRecord> = [
    ...columns,
    {
      title: 'Actions', key: 'actions', fixed: 'right', width: 280,
      render: (_value, row) => <Space size={2}>
        <Button type="link" icon={<EyeOutlined />} onClick={() => setSelected(row)}>View details</Button>
        {fields.length > 0 && updatePermission && hasPermission(updatePermission) &&
          <Button type="link" icon={<EditOutlined />} onClick={() => openEdit(row)}>Edit</Button>}
        {deactivatePermission && hasPermission(deactivatePermission) &&
          <Button type="link" danger icon={<DeleteOutlined />} onClick={() => deactivate(row)}>Deactivate</Button>}
      </Space>,
    },
  ];

  return (
    <Flex vertical gap={18}>
      <Flex align="flex-start" justify="space-between" gap={16} wrap>
        <div>
          <Typography.Title level={3} className="resource-list__title">{title}</Typography.Title>
          <Space direction="vertical" size={6}>
            <Typography.Text type="secondary">{description}</Typography.Text>
            {controlPermissions.length > 0 && (
              <Tag icon={<SafetyCertificateOutlined />} color={fullControl ? 'success' : 'warning'}>
                {fullControl ? 'Full management access' : 'Read-only access'}
              </Tag>
            )}
          </Space>
        </div>
        <Space>
          {fields.length > 0 && createPermission && hasPermission(createPermission) &&
            <Button type="primary" icon={<PlusOutlined />} onClick={openCreate}>Create</Button>}
          <Button icon={<ReloadOutlined />} loading={resources.isFetching} onClick={() => void resources.refetch()}>Refresh</Button>
        </Space>
      </Flex>
      {resources.isError && <Alert type="error" showIcon message={`${title} could not be loaded`} description="Check your permission and backend connection, then retry." />}
      <Card className="resource-list-card">
        <Table<ResourceRecord>
          rowKey="id"
          columns={tableColumns}
          dataSource={resources.data ?? []}
          loading={resources.isLoading}
          pagination={{ pageSize: 10, showSizeChanger: true, showTotal: (total) => `${total} records` }}
          scroll={{ x: 760 }}
          locale={{ emptyText: `No ${title.toLowerCase()} found` }}
        />
      </Card>
      <Drawer
        title={`${title} details`}
        width={760}
        open={Boolean(selected)}
        onClose={() => setSelected(undefined)}
        destroyOnHidden
      >
        {details.isLoading && <Flex justify="center"><Spin aria-label="Loading details" /></Flex>}
        {details.isError && <Alert type="error" showIcon message="Details could not be loaded" />}
        {detail && (
          <Flex vertical gap={20}>
            <Descriptions bordered size="small" column={1} items={Object.entries(detail).map(([key, value]) => ({
              key, label: fieldLabel(key), children: detailValue(value),
            }))} />
            {relatedEndpoint && (
              <Card size="small" title={endpoint === '/vehicles' ? 'Vehicle documents' : 'Driver licences'}
                extra={relatedConfig && hasPermission(relatedConfig.permission) ? <Button size="small" type="primary" icon={<PlusOutlined />}
                  onClick={() => setEditor({ title: `Add ${relatedConfig.title}`, endpoint: relatedEndpoint, method: 'post',
                    fields: relatedConfig.fields, queryKey: `${queryKey}-compliance` })}>Add</Button> : undefined}>
                {related.isLoading && <Spin size="small" />}
                {related.isError && <Alert type="error" showIcon message="Compliance records could not be loaded" />}
                {!related.isLoading && !related.isError && !related.data?.length && <Typography.Text type="secondary">No compliance records</Typography.Text>}
                {related.data?.map((record) => <Card key={record.id} size="small" className="resource-related-detail"
                  extra={relatedConfig && hasPermission(relatedConfig.permission) ? <Space>
                    <Button size="small" icon={<EditOutlined />} onClick={() => setEditor({ title: `Edit ${relatedConfig.title}`,
                      endpoint: `${relatedEndpoint}/${record.id}`, method: 'patch', fields: relatedConfig.fields,
                      initial: record, queryKey: `${queryKey}-compliance` })}>Edit</Button>
                    <Button size="small" danger icon={<DeleteOutlined />} onClick={() => modal.confirm({
                      title: `Delete ${relatedConfig.title}`, content: 'The backend will retain historical audit data.',
                      okButtonProps: { danger: true }, onOk: async () => {
                        await api.delete(`${relatedEndpoint}/${record.id}`);
                        await related.refetch();
                        void message.success(`${relatedConfig.title} removed`);
                      },
                    })}>Delete</Button>
                  </Space> : undefined}>
                  <Descriptions size="small" column={1} bordered
                    items={Object.entries(record).map(([key, value]) => ({ key, label: fieldLabel(key), children: detailValue(value) }))} />
                </Card>)}
              </Card>
            )}
            {endpoint === '/vehicles' && selected?.id && (
              <VehicleReadingsSection vehicleId={selected.id} />
            )}
          </Flex>
        )}
      </Drawer>
      {editor && <ResourceEditorModal open title={editor.title} endpoint={editor.endpoint} method={editor.method}
        fields={editor.fields} initial={editor.initial} queryKey={editor.queryKey} onClose={() => {
          setEditor(undefined);
          void resources.refetch();
          if (relatedEndpoint) void related.refetch();
        }} />}
    </Flex>
  );
}

export const resourcePages = {
  vehicles: {
    endpoint: '/vehicles', queryKey: 'vehicles-page', title: 'Vehicle registry', description: 'Live vehicle master data from the fleet module.',
    controlPermissions: ['VEHICLE_CREATE', 'VEHICLE_UPDATE', 'VEHICLE_STATUS_UPDATE', 'VEHICLE_DOCUMENT_MANAGE'],
    createPermission: 'VEHICLE_CREATE', updatePermission: 'VEHICLE_UPDATE', deactivatePermission: 'VEHICLE_STATUS_UPDATE',
    fields: [
      { name: 'registrationNumber', label: 'Registration number', required: true },
      { name: 'chassisNumber', label: 'Chassis number' }, { name: 'engineNumber', label: 'Engine number' },
      { name: 'categoryId', label: 'Category', kind: 'select', required: true, referenceEndpoint: '/vehicle-categories' },
      { name: 'typeId', label: 'Vehicle type', kind: 'select', required: true, referenceEndpoint: '/vehicle-types' },
      { name: 'manufacturer', label: 'Manufacturer' }, { name: 'model', label: 'Model' },
      { name: 'manufactureYear', label: 'Manufacture year', kind: 'number', positive: true },
      { name: 'ownershipType', label: 'Ownership', kind: 'select', required: true, options: [
        { value: 'COMPANY_OWNED', label: 'Company owned' }, { value: 'LEASED', label: 'Leased' },
      ] },
      { name: 'operationalStatus', label: 'Operational status', kind: 'select', required: true, options: [
        { value: 'AVAILABLE', label: 'Available' }, { value: 'ALLOCATED', label: 'Allocated' },
        { value: 'MAINTENANCE', label: 'Maintenance' }, { value: 'OUT_OF_SERVICE', label: 'Out of service' },
        { value: 'BROKEN_DOWN', label: 'Broken down' },
      ] },
      { name: 'currentOdometerKm', label: 'Current odometer (km)', kind: 'number', positive: true },
      { name: 'engineHours', label: 'Engine hours', kind: 'number', positive: true },
      { name: 'capacityKg', label: 'Capacity (kg)', kind: 'number', positive: true },
      { name: 'active', label: 'Active', kind: 'switch' },
    ] as ResourceField[],
    related: { title: 'vehicle document', permission: 'VEHICLE_DOCUMENT_MANAGE', fields: [
      { name: 'documentType', label: 'Document type', required: true },
      { name: 'documentNumber', label: 'Document number', required: true },
      { name: 'issueDate', label: 'Issue date', kind: 'date' },
      { name: 'expiryDate', label: 'Expiry date', kind: 'date' },
      { name: 'fileReference', label: 'File reference / URL' },
      { name: 'mandatoryForDispatch', label: 'Mandatory for dispatch', kind: 'switch' },
      { name: 'status', label: 'Status', kind: 'select', required: true, options: [
        { value: 'ACTIVE', label: 'Active' }, { value: 'INACTIVE', label: 'Inactive' },
      ] },
      { name: 'active', label: 'Active', kind: 'switch' },
    ] as ResourceField[] },
    columns: [
      { title: 'Registration', dataIndex: 'registrationNumber', render: text },
      { title: 'Manufacturer', dataIndex: 'manufacturer', render: text },
      { title: 'Model', dataIndex: 'model', render: text },
      { title: 'Capacity (kg)', dataIndex: 'capacityKg', render: text },
      { title: 'Operational status', dataIndex: 'operationalStatus', render: status },
      { title: 'State', dataIndex: 'active', render: state },
    ] as TableColumnsType<ResourceRecord>,
  },
  categories: {
    endpoint: '/vehicle-categories', queryKey: 'vehicle-categories-page', title: 'Vehicle categories', description: 'Fleet category definitions used by vehicle types.',
    controlPermissions: ['VEHICLE_CREATE', 'VEHICLE_UPDATE', 'VEHICLE_STATUS_UPDATE'],
    createPermission: 'VEHICLE_CREATE', updatePermission: 'VEHICLE_UPDATE', deactivatePermission: 'VEHICLE_STATUS_UPDATE',
    fields: [
      { name: 'code', label: 'Code', required: true }, { name: 'name', label: 'Name', required: true },
      { name: 'description', label: 'Description', kind: 'textarea' }, { name: 'active', label: 'Active', kind: 'switch' },
    ] as ResourceField[],
    columns: [
      { title: 'Code', dataIndex: 'code', render: text }, { title: 'Name', dataIndex: 'name', render: text },
      { title: 'Description', dataIndex: 'description', render: text }, { title: 'State', dataIndex: 'active', render: state },
    ] as TableColumnsType<ResourceRecord>,
  },
  types: {
    endpoint: '/vehicle-types', queryKey: 'vehicle-types-page', title: 'Vehicle types', description: 'Vehicle capabilities and their fleet category relationships.',
    controlPermissions: ['VEHICLE_CREATE', 'VEHICLE_UPDATE', 'VEHICLE_STATUS_UPDATE'],
    createPermission: 'VEHICLE_CREATE', updatePermission: 'VEHICLE_UPDATE', deactivatePermission: 'VEHICLE_STATUS_UPDATE',
    fields: [
      { name: 'categoryId', label: 'Category', kind: 'select', required: true, referenceEndpoint: '/vehicle-categories' },
      { name: 'code', label: 'Code', required: true }, { name: 'name', label: 'Name', required: true },
      { name: 'description', label: 'Description', kind: 'textarea' }, { name: 'active', label: 'Active', kind: 'switch' },
    ] as ResourceField[],
    columns: [
      { title: 'Code', dataIndex: 'code', render: text }, { title: 'Name', dataIndex: 'name', render: text },
      { title: 'Category', dataIndex: 'categoryId', render: shortId }, { title: 'Description', dataIndex: 'description', render: text },
      { title: 'State', dataIndex: 'active', render: state },
    ] as TableColumnsType<ResourceRecord>,
  },
  drivers: {
    endpoint: '/drivers', queryKey: 'drivers-page', title: 'Driver registry', description: 'Live driver profiles and operational state from the fleet module.',
    controlPermissions: ['DRIVER_CREATE', 'DRIVER_UPDATE', 'DRIVER_LICENSE_MANAGE'],
    createPermission: 'DRIVER_CREATE', updatePermission: 'DRIVER_UPDATE', deactivatePermission: 'DRIVER_UPDATE',
    fields: [
      { name: 'employeeNumber', label: 'Employee number', required: true },
      { name: 'firstName', label: 'First name', required: true }, { name: 'lastName', label: 'Last name', required: true },
      { name: 'phone', label: 'Phone' }, { name: 'email', label: 'Email' },
      { name: 'status', label: 'Operational status', kind: 'select', required: true, options: [
        { value: 'AVAILABLE', label: 'Available' }, { value: 'ASSIGNED', label: 'Assigned' },
        { value: 'UNAVAILABLE', label: 'Unavailable' }, { value: 'ON_LEAVE', label: 'On leave' },
      ] }, { name: 'active', label: 'Active', kind: 'switch' },
    ] as ResourceField[],
    related: { title: 'driver licence', permission: 'DRIVER_LICENSE_MANAGE', fields: [
      { name: 'licenseNumber', label: 'Licence number', required: true },
      { name: 'licenseClass', label: 'Licence class', required: true },
      { name: 'issueDate', label: 'Issue date', kind: 'date', required: true },
      { name: 'expiryDate', label: 'Expiry date', kind: 'date', required: true },
      { name: 'status', label: 'Status', kind: 'select', required: true, options: [
        { value: 'ACTIVE', label: 'Active' }, { value: 'INACTIVE', label: 'Inactive' },
      ] }, { name: 'active', label: 'Active', kind: 'switch' },
    ] as ResourceField[] },
    columns: [
      { title: 'Employee no.', dataIndex: 'employeeNumber', render: text },
      { title: 'Name', render: (_value, row) => `${text(row.firstName)} ${text(row.lastName)}` },
      { title: 'Phone', dataIndex: 'phone', render: text }, { title: 'Email', dataIndex: 'email', render: text },
      { title: 'Operational status', dataIndex: 'status', render: status }, { title: 'State', dataIndex: 'active', render: state },
    ] as TableColumnsType<ResourceRecord>,
  },
  routes: {
    endpoint: '/routes', queryKey: 'routes-page', title: 'Route library', description: 'Active route definitions, planned distance, duration, and ordered stops.',
    controlPermissions: ['ROUTE_CREATE', 'ROUTE_UPDATE'],
    createPermission: 'ROUTE_CREATE', updatePermission: 'ROUTE_UPDATE', deactivatePermission: 'ROUTE_UPDATE',
    fields: [
      { name: 'code', label: 'Code', required: true }, { name: 'name', label: 'Name', required: true },
      { name: 'originLocationId', label: 'Origin', kind: 'select', required: true, referenceEndpoint: '/locations' },
      { name: 'destinationLocationId', label: 'Destination', kind: 'select', required: true, referenceEndpoint: '/locations' },
      { name: 'plannedDistanceKm', label: 'Planned distance (km)', kind: 'number', positive: true, required: true },
      { name: 'estimatedDurationMinutes', label: 'Estimated duration (minutes)', kind: 'number', positive: true, required: true },
      { name: 'stops', label: 'Ordered stops', kind: 'multi-select', referenceEndpoint: '/locations' },
      { name: 'active', label: 'Active', kind: 'switch' },
    ] as ResourceField[],
    columns: [
      { title: 'Code', dataIndex: 'code', render: text }, { title: 'Name', dataIndex: 'name', render: text },
      { title: 'Distance (km)', dataIndex: 'plannedDistanceKm', render: text },
      { title: 'Duration (min)', dataIndex: 'estimatedDurationMinutes', render: text },
      { title: 'Stops', dataIndex: 'stopLocationIds', render: (value) => Array.isArray(value) ? value.length : 0 },
      { title: 'State', dataIndex: 'active', render: state },
    ] as TableColumnsType<ResourceRecord>,
  },
  users: {
    endpoint: '/users', queryKey: 'users-page', title: 'Users', description: 'Identity accounts and assigned backend roles.',
    controlPermissions: ['IDENTITY_MANAGE'],
    columns: [
      { title: 'Username', dataIndex: 'username', render: text },
      { title: 'Name', render: (_value, row) => `${text(row.firstName)} ${text(row.lastName)}` },
      { title: 'Email', dataIndex: 'email', render: text },
      { title: 'Roles', dataIndex: 'roles', render: (value) => Array.isArray(value) ? value.map((role) => <Tag key={String(role)}>{String(role)}</Tag>) : '—' },
      { title: 'State', dataIndex: 'active', render: state },
    ] as TableColumnsType<ResourceRecord>,
  },
  roles: {
    endpoint: '/roles', queryKey: 'roles-page', title: 'Roles', description: 'Backend roles and their granted business permissions.',
    controlPermissions: ['IDENTITY_MANAGE'],
    columns: [
      { title: 'Name', dataIndex: 'name', render: text }, { title: 'Description', dataIndex: 'description', render: text },
      { title: 'Permissions', dataIndex: 'permissions', render: (value) => Array.isArray(value) ? `${value.length} permissions` : '0 permissions' },
      { title: 'State', dataIndex: 'active', render: state },
    ] as TableColumnsType<ResourceRecord>,
  },
};
