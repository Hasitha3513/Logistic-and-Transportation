import { EyeOutlined, PlusOutlined, ReloadOutlined } from '@ant-design/icons';
import { Alert, Button, Card, DatePicker, Flex, Input, Select, Space, Table, Tag, Typography, type TableColumnsType } from 'antd';
import type { Dayjs } from 'dayjs';
import { useMemo, useState } from 'react';
import { Link, Navigate } from 'react-router-dom';
import { useAuth } from '../../../../auth/AuthContext';
import { PriorityTag } from '../../../../components/status/StatusTags';
import { useFreightCustomers, useFreightLocations, useFreightOrders } from '../hooks/useFreightOrders';
import type { FreightOrder, OrganizationReference } from '../types/freightOrder';

const referenceMap = (items?: OrganizationReference[]) => new Map((items ?? []).map((item) => [item.id, item]));
export default function FreightOrderListPage() {
  const { hasPermission } = useAuth(); const [page, setPage] = useState(1); const [limit, setLimit] = useState(10);
  const [search, setSearch] = useState<string>(); const [customerId, setCustomerId] = useState<string>();
  const [period, setPeriod] = useState<[Dayjs | null, Dayjs | null] | null>(null);
  const customers = useFreightCustomers(); const locations = useFreightLocations();
  const orders = useFreightOrders({ page: page - 1, limit, search, customerId,
    pickupFrom: period?.[0]?.startOf('day').toISOString(), pickupTo: period?.[1]?.endOf('day').toISOString() });
  const customerById = useMemo(() => referenceMap(customers.data), [customers.data]);
  const locationById = useMemo(() => referenceMap(locations.data), [locations.data]);
  if (!hasPermission('FREIGHT_ORDER_VIEW')) return <Navigate to="/workspace" replace />;
  const label = (map: Map<string, OrganizationReference>, id: string) => map.get(id)?.name ?? id;
  const columns: TableColumnsType<FreightOrder> = [
    { title: 'Order number', dataIndex: 'orderNumber', render: (value, row) => <Link to={`/freight/orders/${row.id}`}><Typography.Text strong>{value}</Typography.Text></Link> },
    { title: 'Customer', render: (_, row) => label(customerById, row.customerId) },
    { title: 'Origin', render: (_, row) => label(locationById, row.originLocationId) },
    { title: 'Destination', render: (_, row) => label(locationById, row.destinationLocationId) },
    { title: 'Pickup', dataIndex: 'requestedPickupAt', render: (value) => new Date(value).toLocaleString() },
    { title: 'Delivery', dataIndex: 'requestedDeliveryAt', responsive: ['lg'], render: (value) => new Date(value).toLocaleString() },
    { title: 'Service level', dataIndex: 'serviceLevel', render: (value) => <Tag>{value}</Tag> },
    { title: 'Priority', dataIndex: 'priority', render: (value) => <PriorityTag priority={value} /> },
    { title: 'Actions', render: (_, row) => <Link to={`/freight/orders/${row.id}`}><Button type="link" icon={<EyeOutlined />}>View</Button></Link> },
  ];
  return <Flex vertical gap={18}>
    <Flex justify="space-between" align="center" wrap gap={12}><Typography.Text type="secondary">Commercial shipment requests and their minimal cargo lines.</Typography.Text><Space>
      {hasPermission('FREIGHT_ORDER_MANAGE') && <Link to="/freight/orders/new"><Button type="primary" icon={<PlusOutlined />}>New freight order</Button></Link>}
      <Button icon={<ReloadOutlined />} loading={orders.isFetching} onClick={() => void orders.refetch()}>Refresh</Button>
    </Space></Flex>
    <Card variant="borderless"><Flex wrap gap={12}>
      <Input.Search aria-label="Freight order search" placeholder="Order, service or priority" allowClear onSearch={(value) => { setSearch(value || undefined); setPage(1); }} style={{ maxWidth: 260 }} />
      <Select aria-label="Customer" placeholder="All customers" allowClear showSearch optionFilterProp="label" loading={customers.isLoading} options={(customers.data ?? []).map((item) => ({ value: item.id, label: `${item.code} — ${item.name}` }))} onChange={(value) => { setCustomerId(value); setPage(1); }} style={{ minWidth: 230 }} />
      <DatePicker.RangePicker aria-label="Pickup period" value={period} onChange={(value) => { setPeriod(value); setPage(1); }} />
    </Flex></Card>
    {orders.isError && <Alert type="error" showIcon message="Freight orders could not be loaded" />}
    <Card><Table rowKey="id" columns={columns} dataSource={orders.data?.content ?? []} loading={orders.isLoading} scroll={{ x: 1100 }} locale={{ emptyText: 'No freight orders found' }} pagination={{ current: page, pageSize: limit, total: orders.data?.totalElements ?? 0, showSizeChanger: true, onChange: (next, size) => { setPage(next); setLimit(size); } }} /></Card>
  </Flex>;
}
