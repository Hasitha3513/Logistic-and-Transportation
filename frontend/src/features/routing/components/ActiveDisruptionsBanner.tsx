import { AlertOutlined } from '@ant-design/icons';
import { Alert, Space, Tag } from 'antd';
import { useActiveDisruptions } from '../hooks/useRouteHistoryAndDisruptions';

export function ActiveDisruptionsBanner() {
  const { data: activeDisruptions } = useActiveDisruptions();

  if (!activeDisruptions || activeDisruptions.length === 0) {
    return null;
  }

  return (
    <Alert
      type="warning"
      showIcon
      icon={<AlertOutlined />}
      message={
        <Space wrap>
          <strong>{activeDisruptions.length} Active Route Disruption(s) in Network:</strong>
          {activeDisruptions.slice(0, 3).map((d) => (
            <Tag key={d.id} color="volcano">
              {d.disruptionType.replace('_', ' ')}: {d.description}
            </Tag>
          ))}
          {activeDisruptions.length > 3 && (
            <Tag>+{activeDisruptions.length - 3} more</Tag>
          )}
        </Space>
      }
      style={{ marginBottom: 16 }}
    />
  );
}
