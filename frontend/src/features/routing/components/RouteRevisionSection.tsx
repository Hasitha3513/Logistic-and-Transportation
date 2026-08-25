import { HistoryOutlined } from '@ant-design/icons';
import { Alert, Badge, Card, Descriptions, Empty, Flex, Spin, Tag } from 'antd';
import { useRouteRevisions } from '../hooks/useRouteHistoryAndDisruptions';

interface RouteRevisionSectionProps {
  routeId: string;
}

export function RouteRevisionSection({ routeId }: RouteRevisionSectionProps) {
  const { data: revisions, isLoading, isError } = useRouteRevisions(routeId);

  if (isLoading) {
    return (
      <Card size="small" title={<><HistoryOutlined /> Revision History</>}>
        <Flex justify="center" style={{ padding: 16 }}>
          <Spin size="small" aria-label="Loading revisions" />
        </Flex>
      </Card>
    );
  }

  if (isError) {
    return (
      <Card size="small" title={<><HistoryOutlined /> Revision History</>}>
        <Alert type="error" showIcon message="Route revisions could not be loaded" />
      </Card>
    );
  }

  if (!revisions || revisions.length === 0) {
    return (
      <Card size="small" title={<><HistoryOutlined /> Revision History</>}>
        <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="No revisions recorded yet" />
      </Card>
    );
  }

  return (
    <Card size="small" title={<><HistoryOutlined /> Revision History ({revisions.length})</>}>
      <Flex vertical gap={12}>
        {revisions.map((rev) => (
          <Card key={rev.id} size="small" type="inner" title={
            <Flex justify="space-between" align="center">
              <span>
                <Badge count={`v${rev.revisionNumber}`} style={{ backgroundColor: rev.revisionNumber === revisions[0].revisionNumber ? '#1677ff' : '#8c8c8c', marginRight: 8 }} />
                <strong>{rev.name}</strong> ({rev.code})
              </span>
              <Tag color={rev.active ? 'success' : 'default'}>{rev.active ? 'ACTIVE' : 'INACTIVE'}</Tag>
            </Flex>
          }>
            <Descriptions size="small" column={{ xs: 1, sm: 2 }} bordered>
              <Descriptions.Item label="Planned Distance">{rev.plannedDistanceKm} km</Descriptions.Item>
              <Descriptions.Item label="Estimated Duration">{rev.estimatedDurationMinutes} min</Descriptions.Item>
              <Descriptions.Item label="Stops">{rev.stopLocationIds?.length ?? 0} stop(s)</Descriptions.Item>
              <Descriptions.Item label="Changed By">{rev.changedBy}</Descriptions.Item>
              <Descriptions.Item label="Changed At" span={2}>
                {new Date(rev.changedAt).toLocaleString()}
              </Descriptions.Item>
            </Descriptions>
          </Card>
        ))}
      </Flex>
    </Card>
  );
}
