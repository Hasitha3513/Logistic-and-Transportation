import { Card, Empty, List, Space, Spin, Tag, Typography } from 'antd';
import { useDeliveryNotifications } from '../hooks/useDeliveryNotifications';

const label = (eventType: string) => eventType.replace(/^DELIVERY_/, '').replaceAll('_', ' ');

export function DeliveryNotificationTimeline({ deliveryId }: Readonly<{ deliveryId: string }>) {
  const timeline = useDeliveryNotifications(deliveryId);
  return <Card title="Customer notification timeline">
    {timeline.isLoading ? <Spin aria-label="Loading notification timeline" /> : null}
    {!timeline.isLoading && !timeline.data?.length ? <Empty description="No customer notifications" /> : null}
    <List dataSource={timeline.data ?? []} renderItem={(item) => <List.Item>
      <List.Item.Meta
        title={<Space><Typography.Text>{label(item.eventType)}</Typography.Text>
          <Tag>{item.channel}</Tag><Tag color={item.status === 'SENT' ? 'green' : item.status === 'FAILED' ? 'red' : 'blue'}>{item.status}</Tag>
        </Space>}
        description={<Space direction="vertical" size={0}>
          <Typography.Text type="secondary">Destination: {item.recipient ?? 'Unavailable'}</Typography.Text>
          <Typography.Text type="secondary">Created: {new Date(item.createdAt).toLocaleString()}</Typography.Text>
          {item.sentAt ? <Typography.Text type="secondary">Sent: {new Date(item.sentAt).toLocaleString()}</Typography.Text> : null}
          <Typography.Text type="secondary">Attempts: {item.attemptCount}</Typography.Text>
          {item.failureCategory ? <Typography.Text type="danger">Failure: {item.failureCategory}</Typography.Text> : null}
        </Space>}
      />
    </List.Item>} />
  </Card>;
}
