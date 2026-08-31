import { CheckCircleOutlined, EditOutlined } from '@ant-design/icons';
import { Alert, App as AntApp, Button, Card, Descriptions, Flex, Result, Space, Spin, Tag, Typography } from 'antd';
import { isAxiosError } from 'axios'; import { Link, useParams } from 'react-router-dom'; import { useAuth } from '../../../../auth/AuthContext';
import { useDeliveryOrder, useValidateDeliveryReadiness } from '../hooks/useDeliveryOrders';
import { ProofOfDeliverySection } from '../components/ProofOfDeliverySection';
import { FailedDeliverySection } from '../components/FailedDeliverySection';

interface ErrorBody { message?: string }
export default function DeliveryOrderDetailsPage() {
  const { deliveryId } = useParams(); const { hasPermission } = useAuth(); const { message } = AntApp.useApp(); const order = useDeliveryOrder(deliveryId); const validate = useValidateDeliveryReadiness();
  if (order.isLoading) return <Flex justify="center"><Spin aria-label="Loading delivery order" /></Flex>;
  if (order.isError || !order.data) return <Result status="404" title="Delivery order not found" extra={<Link to="/deliveries"><Button>Back to delivery orders</Button></Link>} />;
  const data = order.data; const markReady = async () => { try { await validate.mutateAsync({ id: data.id, version: data.version }); void message.success('Delivery order is ready for assignment'); } catch (error) { void message.error(isAxiosError<ErrorBody>(error) ? error.response?.data?.message ?? 'Readiness validation failed' : 'Readiness validation failed'); } };
  return <Flex vertical gap={18}>
    <Flex justify="space-between" align="start"><div><Typography.Title level={3}>{data.deliveryNumber}</Typography.Title><Tag color={data.status === 'DRAFT' ? 'default' : data.status === 'DELIVERED' ? 'green' : data.status === 'FAILED_ATTEMPT' ? 'blue' : data.status === 'RETURN_TO_BASE' ? 'red' : data.status === 'ESCALATED' ? 'orange' : 'green'}>{data.status.replaceAll('_', ' ')}</Tag></div><Space>{hasPermission('DELIVERY_UPDATE') && <Link to={`/deliveries/${data.id}/edit`}><Button icon={<EditOutlined />}>Edit</Button></Link>}{hasPermission('DELIVERY_ASSIGN') && data.status === 'DRAFT' && <Button type="primary" icon={<CheckCircleOutlined />} loading={validate.isPending} onClick={() => void markReady()}>Validate readiness</Button>}</Space></Flex>
    {data.status === 'DRAFT' && <Alert showIcon type="info" message="Draft requirements" description="Validate readiness after confirming the active customer, origin, destination, and delivery window." />}
    <Card><Descriptions bordered column={{ xs: 1, md: 2 }} items={[
      { key: 'number', label: 'Delivery number', children: data.deliveryNumber }, { key: 'status', label: 'Status', children: data.status.replaceAll('_', ' ') },
      { key: 'customer', label: 'Customer ID', children: data.customerId }, { key: 'origin', label: 'Origin location ID', children: data.originLocationId },
      { key: 'destination', label: 'Destination location ID', children: data.destinationLocationId }, { key: 'priority', label: 'Priority', children: data.priority },
      { key: 'service', label: 'Service type', children: data.serviceType.replaceAll('_', ' ') }, { key: 'window', label: 'Delivery window', children: `${new Date(data.windowStart).toLocaleString()} — ${new Date(data.windowEnd).toLocaleString()}` },
      { key: 'instructions', label: 'Instructions', span: 2, children: data.instructions || 'None' }, { key: 'updated', label: 'Last updated', children: new Date(data.updatedAt).toLocaleString() },
    ]} /></Card>
    <ProofOfDeliverySection delivery={data} />
    <FailedDeliverySection delivery={data} />
  </Flex>;
}
