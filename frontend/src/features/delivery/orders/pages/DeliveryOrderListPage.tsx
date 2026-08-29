import { PlusOutlined, SearchOutlined } from '@ant-design/icons';
import { Button, Card, Empty, Flex, Input, Select, Space, Table, Tag, Typography } from 'antd';
import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../../../../auth/AuthContext';
import { useDeliveryOrders } from '../hooks/useDeliveryOrders';
import type { DeliveryStatus } from '../types/deliveryOrder';

export default function DeliveryOrderListPage() {
  const { hasPermission } = useAuth(); const [page, setPage] = useState(0); const [search, setSearch] = useState(''); const [status, setStatus] = useState<DeliveryStatus>();
  const orders = useDeliveryOrders({ page, size: 20, search: search || undefined, status });
  return <Flex vertical gap={18}>
    <Flex justify="space-between" align="start"><div><Typography.Title level={3}>Delivery orders</Typography.Title><Typography.Text type="secondary">Tenant-scoped last-mile delivery requirements and readiness.</Typography.Text></div>{hasPermission('DELIVERY_CREATE') && <Link to="/deliveries/new"><Button type="primary" icon={<PlusOutlined />}>New delivery order</Button></Link>}</Flex>
    <Card><Space wrap style={{ marginBottom: 16 }}><Input allowClear prefix={<SearchOutlined />} placeholder="Search delivery number" aria-label="Search delivery orders" value={search} onChange={(event) => { setSearch(event.target.value); setPage(0); }} /><Select allowClear placeholder="All statuses" aria-label="Delivery status" value={status} onChange={(value) => { setStatus(value); setPage(0); }} options={[{ value: 'DRAFT', label: 'Draft' }, { value: 'READY_FOR_ASSIGNMENT', label: 'Ready for assignment' }]} style={{ width: 220 }} /></Space>
      <Table rowKey="id" loading={orders.isLoading} dataSource={orders.data?.content ?? []} locale={{ emptyText: <Empty description="No delivery orders found" /> }} pagination={{ current: page + 1, pageSize: 20, total: orders.data?.totalElements, onChange: (value) => setPage(value - 1) }} columns={[
        { title: 'Delivery number', dataIndex: 'deliveryNumber', render: (value: string, row) => <Link to={`/deliveries/${row.id}`}>{value}</Link> },
        { title: 'Priority', dataIndex: 'priority' }, { title: 'Service type', dataIndex: 'serviceType', render: (value: string) => value.replaceAll('_', ' ') },
        { title: 'Window start', dataIndex: 'windowStart', render: (value: string) => new Date(value).toLocaleString() },
        { title: 'Status', dataIndex: 'status', render: (value: string) => <Tag color={value === 'DRAFT' ? 'default' : 'green'}>{value.replaceAll('_', ' ')}</Tag> },
      ]} />
    </Card>
  </Flex>;
}
