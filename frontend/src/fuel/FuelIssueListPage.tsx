import { useState } from 'react';
import { EyeOutlined, PlusOutlined, ReloadOutlined } from '@ant-design/icons';
import { Alert, Button, Card, DatePicker, Flex, Input, Select, Space, Table, Typography, type TableColumnsType } from 'antd';
import type { Dayjs } from 'dayjs';
import { Link, Navigate } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';
import { FuelIssueStatusTag } from '../components/status/StatusTags';
import { useFuelIssues } from './hooks/useFuelIssues';
import type { FuelIssue } from './types';

const { RangePicker } = DatePicker;
const statuses = ['DRAFT', 'PENDING_AUTHORIZATION', 'AUTHORIZED', 'ISSUED', 'CANCELLED']
  .map((value) => ({ value, label: value.replaceAll('_', ' ') }));

export default function FuelIssueListPage() {
  const { hasPermission } = useAuth();
  const [page, setPage] = useState(1);
  const [limit, setLimit] = useState(10);
  const [status, setStatus] = useState<string>();
  const [vehicleId, setVehicleId] = useState<string>();
  const [tripId, setTripId] = useState<string>();
  const [voucherNumber, setVoucherNumber] = useState<string>();
  const [period, setPeriod] = useState<[Dayjs | null, Dayjs | null] | null>(null);
  const query = useFuelIssues({
    page: page - 1, limit, status, voucherNumber, vehicleId, tripId,
    fromDate: period?.[0]?.format('YYYY-MM-DD'), toDate: period?.[1]?.format('YYYY-MM-DD'),
  });

  if (!hasPermission('FUEL_ISSUE_VIEW')) return <Navigate to="/workspace" replace />;

  const columns: TableColumnsType<FuelIssue> = [
    { title: 'Voucher', dataIndex: 'voucherNumber', key: 'voucherNumber', render: (value, row) => <Link to={`/fuel/issues/${row.id}`}><Typography.Text strong>{value}</Typography.Text></Link> },
    { title: 'Issue date', dataIndex: 'issueDateTime', key: 'issueDateTime', render: (value: string) => new Date(value).toLocaleString() },
    { title: 'Vehicle', key: 'vehicle', render: (_, row) => row.vehicle.id.slice(0, 8).toUpperCase() },
    { title: 'Trip', key: 'trip', responsive: ['lg'], render: (_, row) => row.trip?.id.slice(0, 8).toUpperCase() ?? '—' },
    { title: 'Station', key: 'station', render: (_, row) => row.station.name },
    { title: 'Fuel type', dataIndex: 'fuelType', key: 'fuelType' },
    { title: 'Quantity', dataIndex: 'quantity', key: 'quantity', align: 'right' },
    { title: 'Total', dataIndex: 'totalAmount', key: 'totalAmount', align: 'right', responsive: ['md'], render: (value) => value ?? '—' },
    { title: 'Authorized by', dataIndex: 'authorizedBy', key: 'authorizedBy', responsive: ['xl'], render: (value?: string) => value ? value.slice(0, 8).toUpperCase() : '—' },
    { title: 'Status', dataIndex: 'status', key: 'status', render: (value) => <FuelIssueStatusTag status={value} /> },
    { title: 'Actions', key: 'actions', render: (_, row) => <Link to={`/fuel/issues/${row.id}`}><Button type="link" icon={<EyeOutlined />}>View</Button></Link> },
  ];

  return <Flex vertical gap={18}>
    <Flex justify="space-between" align="center" wrap gap={12}>
      <div><Typography.Title level={3}>Fuel issues</Typography.Title><Typography.Text type="secondary">Authorize and record controlled fuel dispensing.</Typography.Text></div>
      <Space>{hasPermission('FUEL_ISSUE_CREATE') && <Link to="/fuel/issues/new"><Button type="primary" icon={<PlusOutlined />}>Create fuel issue</Button></Link>}
        <Button icon={<ReloadOutlined />} loading={query.isFetching} onClick={() => void query.refetch()}>Refresh</Button></Space>
    </Flex>
    <Card variant="borderless"><Flex wrap gap={12}>
      <Input.Search aria-label="Voucher number" placeholder="Voucher number" allowClear onSearch={(value) => { setVoucherNumber(value || undefined); setPage(1); }} style={{ maxWidth: 240 }} />
      <Input.Search aria-label="Vehicle ID" placeholder="Vehicle ID" allowClear onSearch={(value) => { setVehicleId(value || undefined); setPage(1); }} style={{ maxWidth: 220 }} />
      <Input.Search aria-label="Trip ID" placeholder="Trip ID" allowClear onSearch={(value) => { setTripId(value || undefined); setPage(1); }} style={{ maxWidth: 220 }} />
      <Select aria-label="Fuel issue status" placeholder="All statuses" allowClear options={statuses} onChange={(value) => { setStatus(value); setPage(1); }} style={{ minWidth: 210 }} />
      <RangePicker value={period} onChange={(value) => { setPeriod(value); setPage(1); }} />
    </Flex></Card>
    {query.isError && <Alert type="error" showIcon message="Fuel issues could not be loaded" description="Check the backend connection and your permission." />}
    <Card><Table rowKey="id" columns={columns} dataSource={query.data?.content ?? []} loading={query.isLoading}
      scroll={{ x: 1000 }} pagination={{ current: page, pageSize: limit, total: query.data?.totalElements ?? 0, showSizeChanger: true,
        onChange: (next, size) => { setPage(next); setLimit(size); } }} locale={{ emptyText: 'No fuel issues found' }} /></Card>
  </Flex>;
}
