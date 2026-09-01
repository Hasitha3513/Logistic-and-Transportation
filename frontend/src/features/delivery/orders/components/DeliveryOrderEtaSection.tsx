import { ReloadOutlined } from '@ant-design/icons';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Alert, Button, Card, Descriptions, Flex, Spin, Tag, Typography, message } from 'antd';
import { deliveryEtaApi } from '../../eta/api/deliveryEtaApi';
import { useAuth } from '../../../../auth/AuthContext';

export function DeliveryOrderEtaSection({ orderId }: { orderId: string }) {
  const { hasPermission } = useAuth();
  const queryClient = useQueryClient();
  const queryKey = ['delivery-order-eta', orderId];
  const eta = useQuery({
    queryKey,
    queryFn: () => deliveryEtaApi.getOrderEta(orderId),
    retry: false,
    enabled: hasPermission('DELIVERY_VIEW'),
  });
  const recalculate = useMutation({
    mutationFn: () => deliveryEtaApi.calculateOrderEta(orderId),
    onSuccess: (value) => {
      queryClient.setQueryData(queryKey, value);
      void message.success('Delivery ETA recalculated');
    },
    onError: () => void message.error('Unable to recalculate delivery ETA'),
  });

  if (!hasPermission('DELIVERY_VIEW')) return null;
  return <Card title="Estimated Arrival">
    {eta.isLoading && <Spin aria-label="Loading delivery ETA" />}
    {eta.isError && <Alert type="info" showIcon message="ETA unavailable" description="Assign an eligible Rider with a configured transport mode and ensure delivery coordinates are available." />}
    {eta.data && <Flex vertical gap={12}>
      <Descriptions bordered column={{ xs: 1, md: 2 }} items={[
        { key: 'arrival', label: 'Estimated Arrival', children: new Date(eta.data.estimatedArrivalAt).toLocaleString() },
        { key: 'duration', label: 'Travel Duration', children: `${Math.ceil(eta.data.travelDurationSeconds / 60)} min` },
        { key: 'distance', label: 'Distance', children: `${(eta.data.distanceMeters / 1000).toFixed(1)} km` },
        { key: 'sla', label: 'SLA Status', children: eta.data.slaStatus ? <Tag>{eta.data.slaStatus}</Tag> : 'Not available' },
        { key: 'calculated', label: 'Calculated At', children: new Date(eta.data.calculatedAt).toLocaleString() },
        { key: 'freshness', label: 'Fresh/Stale', children: <Tag color={eta.data.isStale ? 'orange' : 'green'}>{eta.data.isStale ? 'Stale' : 'Fresh'}</Tag> },
      ]} />
      <Typography.Text type="secondary">Source: {eta.data.source}</Typography.Text>
    </Flex>}
    {hasPermission('DELIVERY_UPDATE') && <Button style={{ marginTop: 12 }} icon={<ReloadOutlined />} loading={recalculate.isPending} onClick={() => recalculate.mutate()}>Recalculate</Button>}
  </Card>;
}
