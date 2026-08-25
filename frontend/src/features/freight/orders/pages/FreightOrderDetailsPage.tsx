import { Alert, Button, Card, Descriptions, Flex, List, Space, Tag, Typography } from 'antd';
import { Link, Navigate, useParams } from 'react-router-dom';
import { useAuth } from '../../../../auth/AuthContext';
import { PriorityTag } from '../../../../components/status/StatusTags';
import { useFreightCustomers, useFreightLocations, useFreightOrder } from '../hooks/useFreightOrders';

export default function FreightOrderDetailsPage() {
  const { freightOrderId = '' } = useParams(); const { hasPermission } = useAuth();
  const query = useFreightOrder(freightOrderId); const customers = useFreightCustomers(); const locations = useFreightLocations();
  if (!hasPermission('FREIGHT_ORDER_VIEW')) return <Navigate to="/workspace" replace />;
  const order = query.data; const ref = (id?: string) => [...(customers.data ?? []), ...(locations.data ?? [])].find((item) => item.id === id)?.name ?? id ?? '—';
  return <Flex vertical gap={18}>
    <Flex justify="space-between" wrap gap={12}><div><Typography.Title level={3}>{order?.orderNumber ?? 'Freight order'}</Typography.Title><Typography.Text type="secondary">Commercial shipment request. Operational lifecycle is outside this foundation.</Typography.Text></div>
      {order && hasPermission('FREIGHT_ORDER_MANAGE') && <Link to={`/freight/orders/${order.id}/edit`}><Button type="primary">Edit order</Button></Link>}
    </Flex>
    {query.isError && <Alert type="error" showIcon message="Freight order could not be loaded" />}
    {order && <>
      <Card title="Order overview" extra={<Space><Tag>{order.serviceLevel}</Tag><PriorityTag priority={order.priority} /></Space>}><Descriptions bordered column={{ xs: 1, md: 2 }} items={[
        { key: 'customer', label: 'Customer', children: ref(order.customerId) }, { key: 'number', label: 'Order number', children: order.orderNumber },
        { key: 'origin', label: 'Origin', children: ref(order.originLocationId) }, { key: 'destination', label: 'Destination', children: ref(order.destinationLocationId) },
        { key: 'pickup', label: 'Requested pickup', children: new Date(order.requestedPickupAt).toLocaleString() }, { key: 'delivery', label: 'Requested delivery', children: new Date(order.requestedDeliveryAt).toLocaleString() },
        { key: 'handling', label: 'Special handling', span: 2, children: order.specialHandlingInstructions ?? '—' },
      ]} /></Card>
      <Card title="Shipment lines"><List dataSource={order.lines} renderItem={(line, index) => <List.Item><List.Item.Meta title={`${index + 1}. ${line.description}`} description={`Quantity: ${line.quantity}`} /></List.Item>} /></Card>
      <Card title="Audit"><Descriptions column={{ xs: 1, md: 2 }} items={[
        { key: 'created', label: 'Created', children: `${new Date(order.createdAt).toLocaleString()} by ${order.createdBy}` },
        { key: 'updated', label: 'Last updated', children: `${new Date(order.updatedAt).toLocaleString()} by ${order.updatedBy}` },
        { key: 'version', label: 'Version', children: order.version },
      ]} /></Card>
    </>}
  </Flex>;
}
