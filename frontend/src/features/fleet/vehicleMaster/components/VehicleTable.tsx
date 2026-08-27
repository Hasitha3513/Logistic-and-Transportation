import { DeleteOutlined, EditOutlined, EyeOutlined } from '@ant-design/icons';
import { Badge, Button, Space, Table, Tag, Tooltip, Typography, type TableColumnsType } from 'antd';
import { VehicleStatusTag } from '../../../../components/status/StatusTags';
import type { Vehicle } from '../types/vehicle';

interface VehicleTableProps {
  vehicles: Vehicle[];
  classificationByTypeId: Map<string, { category: string; type: string }>;
  loading: boolean;
  canEdit: boolean;
  canDeactivate: boolean;
  onView: (vehicle: Vehicle) => void;
  onEdit: (vehicle: Vehicle) => void;
  onDeactivate: (vehicle: Vehicle) => void;
}

const text = (item?: string | number | null) => item == null || item === '' ? '—' : String(item);

export function VehicleTable({
  vehicles,
  classificationByTypeId,
  loading,
  canEdit,
  canDeactivate,
  onView,
  onEdit,
  onDeactivate,
}: VehicleTableProps) {
  const columns: TableColumnsType<Vehicle> = [
    {
      title: 'Registration',
      dataIndex: 'registrationNumber',
      width: 115,
      sorter: (left, right) => left.registrationNumber.localeCompare(right.registrationNumber),
      render: (registration) => <Typography.Text strong>{registration}</Typography.Text>,
    },
    {
      title: 'Vehicle',
      key: 'vehicle',
      width: 180,
      responsive: ['sm'],
      sorter: (left, right) => `${left.manufacturer ?? ''} ${left.model ?? ''}`.localeCompare(`${right.manufacturer ?? ''} ${right.model ?? ''}`),
      render: (_, vehicle) => [vehicle.manufacturer, vehicle.model].filter(Boolean).join(' ') || '—',
    },
    {
      title: 'Ownership',
      dataIndex: 'ownershipType',
      responsive: ['lg'],
      width: 140,
      filters: [
        { text: 'Company owned', value: 'COMPANY_OWNED' },
        { text: 'Leased / rental', value: 'LEASED' },
      ],
      onFilter: (filter, vehicle) => vehicle.ownershipType === filter,
      render: (ownership?: string) => ownership ? <Tag>{ownership.replaceAll('_', ' ')}</Tag> : '—',
    },
    {
      title: 'Category / Type',
      dataIndex: 'typeId',
      responsive: ['lg'],
      width: 190,
      render: (typeId: string) => {
        const classification = classificationByTypeId.get(typeId);
        return classification ? (
          <Space direction="vertical" size={0}>
            <Typography.Text>{classification.category}</Typography.Text>
            <Typography.Text type="secondary">{classification.type}</Typography.Text>
          </Space>
        ) : '—';
      },
    },
    {
      title: 'Capacity (kg)',
      dataIndex: 'capacityKg',
      responsive: ['xl'],
      width: 110,
      sorter: (left, right) => (left.capacityKg ?? 0) - (right.capacityKg ?? 0),
      render: text,
    },
    {
      title: 'Operational status',
      dataIndex: 'operationalStatus',
      width: 105,
      filters: [
        { text: 'Available', value: 'AVAILABLE' },
        { text: 'Allocated', value: 'ALLOCATED' },
        { text: 'Maintenance', value: 'MAINTENANCE' },
        { text: 'Out of service', value: 'OUT_OF_SERVICE' },
        { text: 'Broken down', value: 'BROKEN_DOWN' },
      ],
      onFilter: (filter, vehicle) => vehicle.operationalStatus === filter,
      render: (status: string) => <VehicleStatusTag status={status} />,
    },
    {
      title: 'State',
      dataIndex: 'active',
      responsive: ['md'],
      width: 100,
      render: (active: boolean) => <Badge status={active ? 'success' : 'default'} text={active ? 'Active' : 'Inactive'} />,
    },
    {
      title: 'Actions',
      key: 'actions',
      fixed: 'right',
      width: 104,
      render: (_, vehicle) => (
        <Space size={2} wrap={false}>
          <Tooltip title="View details">
            <Button aria-label="View details" type="text" size="small" icon={<EyeOutlined />} onClick={() => onView(vehicle)} />
          </Tooltip>
          {canEdit && (
            <Tooltip title="Edit">
              <Button aria-label="Edit vehicle" type="text" size="small" icon={<EditOutlined />} onClick={() => onEdit(vehicle)} />
            </Tooltip>
          )}
          {canDeactivate && vehicle.active && (
            <Tooltip title="Deactivate">
              <Button aria-label="Deactivate vehicle" type="text" size="small" danger icon={<DeleteOutlined />} onClick={() => onDeactivate(vehicle)} />
            </Tooltip>
          )}
        </Space>
      ),
    },
  ];

  return (
    <Table<Vehicle>
      rowKey="id"
      columns={columns}
      dataSource={vehicles}
      loading={loading}
      pagination={{ pageSize: 10, showSizeChanger: true, showTotal: (total) => `${total} vehicles` }}
      scroll={{ x: 'max-content' }}
      locale={{ emptyText: 'No vehicles found' }}
    />
  );
}
