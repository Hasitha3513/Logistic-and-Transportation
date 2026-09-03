import { PlusOutlined } from '@ant-design/icons';
import { Button, Card, Flex, Table, Typography } from 'antd';
import { Link, Navigate } from 'react-router-dom';
import { useAuth } from '../../../auth/AuthContext';
import { IntegrationStatusTags } from '../components/IntegrationStatusTags';
import { useIntegrations } from '../hooks/useIntegrations';
import type { Integration } from '../types/integration';

export default function IntegrationListPage() {
  const { hasPermission } = useAuth(); const query = useIntegrations();
  if (!hasPermission('INTEGRATION_VIEW')) return <Navigate to="/" replace />;
  const columns = [
    { title: 'Name', dataIndex: 'name', render: (name: string, item: Integration) => <Link to={`/integrations/${item.id}`}>{name}</Link> },
    { title: 'Type', dataIndex: 'type' }, { title: 'Protocol', dataIndex: 'protocol' },
    { title: 'Direction', dataIndex: 'direction' },
    { title: 'State', render: (_: unknown, item: Integration) => <IntegrationStatusTags lifecycle={item.lifecycle} health={item.health} /> },
    { title: 'Last test', dataIndex: 'lastTestedAt', render: (value?: string) => value ? new Date(value).toLocaleString() : 'Never' },
    { title: 'Last success', dataIndex: 'lastSuccessfulExchangeAt', render: (value?: string) => value ? new Date(value).toLocaleString() : 'Never' },
  ];
  return <Flex vertical gap={18}><Flex justify="space-between" align="center"><Typography.Text type="secondary">
    Governed outbound file exchanges. Endpoint paths and credentials are never shown.
  </Typography.Text>{hasPermission('INTEGRATION_MANAGE') && <Link to="/integrations/new"><Button type="primary" icon={<PlusOutlined />}>New integration</Button></Link>}</Flex>
    <Card><Table rowKey="id" loading={query.isLoading} dataSource={query.data?.content ?? []} columns={columns} pagination={false} /></Card>
  </Flex>;
}
