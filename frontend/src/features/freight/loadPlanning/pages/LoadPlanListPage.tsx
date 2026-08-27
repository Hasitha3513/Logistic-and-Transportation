import { EyeOutlined, PlusOutlined, ReloadOutlined } from '@ant-design/icons';
import { Alert, Button, Card, Flex, Space, Table, Tag, Typography, type TableColumnsType } from 'antd';
import { Link, Navigate } from 'react-router-dom';
import { useAuth } from '../../../../auth/AuthContext';
import { useLoadPlans } from '../hooks/useLoadPlans';
import type { LoadPlan } from '../types/loadPlan';

export default function LoadPlanListPage() {
  const { hasPermission } = useAuth();
  const query = useLoadPlans();

  if (!hasPermission('LOAD_PLAN_VIEW')) {
    return <Navigate to="/workspace" replace />;
  }

  const columns: TableColumnsType<LoadPlan> = [
    {
      title: 'Plan reference',
      dataIndex: 'loadPlanNumber',
      render: (v, row) => (
        <Link to={`/freight/load-plans/${row.id}`}>
          <Typography.Text strong>{v}</Typography.Text>
        </Link>
      ),
    },
    {
      title: 'Status',
      dataIndex: 'readinessStatus',
      render: (v) =>
        v === 'STRUCTURALLY_READY' ? (
          <Tag color="green">STRUCTURALLY READY</Tag>
        ) : (
          <Tag color="default">DRAFT</Tag>
        ),
    },
    {
      title: 'Manifest ID',
      dataIndex: 'cargoManifestId',
      render: (v) => <Typography.Text code>{String(v).substring(0, 8)}</Typography.Text>,
    },
    {
      title: 'Vehicle ID',
      dataIndex: 'vehicleId',
      render: (v) => <Typography.Text code>{String(v).substring(0, 8)}</Typography.Text>,
    },
    {
      title: 'Placements',
      render: (_, r) => r.placements.length,
    },
    {
      title: 'Updated',
      dataIndex: 'updatedAt',
      render: (v) => new Date(v).toLocaleString(),
    },
    {
      title: 'Actions',
      render: (_, r) => (
        <Link to={`/freight/load-plans/${r.id}`}>
          <Button type="link" icon={<EyeOutlined />}>
            View
          </Button>
        </Link>
      ),
    },
  ];

  return (
    <Flex vertical gap={18}>
      <Flex justify="space-between" wrap gap={12}>
        <Typography.Text type="secondary">
          Physical placement, loading sequence, and compatibility plans for finalized Cargo Manifests.
        </Typography.Text>
        <Space>
          {hasPermission('LOAD_PLAN_MANAGE') && (
            <Link to="/freight/load-plans/new">
              <Button type="primary" icon={<PlusOutlined />}>
                New load plan
              </Button>
            </Link>
          )}
          <Button icon={<ReloadOutlined />} onClick={() => void query.refetch()} loading={query.isFetching}>
            Refresh
          </Button>
        </Space>
      </Flex>

      {query.isError && <Alert type="error" showIcon message="Load plans could not be loaded" />}

      <Card>
        <Table<LoadPlan>
          rowKey="id"
          columns={columns}
          dataSource={query.data ?? []}
          loading={query.isLoading}
          locale={{ emptyText: 'No load plans found' }}
          pagination={{ pageSize: 10, showSizeChanger: true }}
        />
      </Card>
    </Flex>
  );
}
