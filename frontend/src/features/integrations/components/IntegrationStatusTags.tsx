import { Space, Tag } from 'antd';
import type { IntegrationHealth, IntegrationLifecycle } from '../types/integration';

export function IntegrationStatusTags({ lifecycle, health }: { lifecycle: IntegrationLifecycle; health: IntegrationHealth }) {
  return <Space><Tag color={lifecycle === 'ACTIVE' ? 'green' : lifecycle === 'DISABLED' ? 'default' : 'blue'}>{lifecycle}</Tag>
    <Tag color={health === 'HEALTHY' ? 'green' : health === 'UNKNOWN' ? 'default' : 'red'}>{health}</Tag></Space>;
}
