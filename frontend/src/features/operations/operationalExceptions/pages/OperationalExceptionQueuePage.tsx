import { Alert, Card, Drawer, Flex, Select, Space, Table, Tag, Typography } from 'antd';
import { useState } from 'react';
import { Navigate } from 'react-router-dom';
import { useAuth } from '../../../../auth/AuthContext';
import { OperationalExceptionDetailPanel } from '../components/OperationalExceptionDetailPanel';
import { useOperationalException, useOperationalExceptions } from '../hooks/useOperationalExceptions';
import type { OperationalExceptionCase } from '../types/operationalException';

export default function OperationalExceptionQueuePage() {
  const { hasPermission } = useAuth();
  const [selectedId, setSelectedId] = useState<string>();
  const [status, setStatus] = useState<string>();
  const [severity, setSeverity] = useState<string>();
  const query = useOperationalExceptions({ status, severity });
  const detail = useOperationalException(selectedId);
  if (!hasPermission('OPERATIONAL_EXCEPTION_VIEW')) return <Navigate to="/" replace />;

  const columns = [
    { title: 'Reference', dataIndex: 'caseReference' },
    { title: 'Source', render: (_: unknown, item: OperationalExceptionCase) => `${item.sourceModule} · ${item.summaryCode}` },
    { title: 'Category', dataIndex: 'category' },
    { title: 'Severity', dataIndex: 'severity', render: (value: string) => <Tag color={value === 'CRITICAL' ? 'red' : value === 'HIGH' ? 'orange' : 'blue'}>{value}</Tag> },
    { title: 'Status', dataIndex: 'status' },
    { title: 'Assignment', render: (_: unknown, item: OperationalExceptionCase) => item.assignedRoleCode ?? item.assignedUserId ?? 'Unassigned' },
    { title: 'Response due', dataIndex: 'responseDueAt', render: (value: string) => new Date(value).toLocaleString() },
    { title: 'Resolution due', dataIndex: 'resolutionDueAt', render: (value: string) => new Date(value).toLocaleString() },
    { title: 'SLA', dataIndex: 'slaStatus', render: (value: string) => <Tag color={value === 'BREACHED' ? 'red' : value === 'AT_RISK' ? 'orange' : 'green'}>{value}</Tag> },
  ];

  return <Flex vertical gap={16}>
    <Typography.Text type="secondary">Tenant-scoped cross-domain exception queue with server-calculated SLA.</Typography.Text>
    <Space wrap>
      <Select allowClear placeholder="Status" style={{ width: 180 }} onChange={setStatus} options={['OPEN','ACKNOWLEDGED','IN_PROGRESS','RESOLVED','CLOSED'].map(value => ({ value }))} />
      <Select allowClear placeholder="Severity" style={{ width: 160 }} onChange={setSeverity} options={['LOW','MEDIUM','HIGH','CRITICAL'].map(value => ({ value }))} />
    </Space>
    {query.isError && <Alert type="error" message="Operational exceptions could not be loaded" />}
    <Card><Table rowKey="id" loading={query.isLoading} dataSource={query.data?.content ?? []} columns={columns}
      pagination={{ pageSize: 20, total: query.data?.totalElements }} onRow={(record) => ({ onClick: () => setSelectedId(record.id) })} /></Card>
    <Drawer width={760} open={Boolean(selectedId)} onClose={() => setSelectedId(undefined)}
      title={detail.data?.exceptionCase.caseReference ?? 'Operational exception'} loading={detail.isLoading}>
      {detail.data && <OperationalExceptionDetailPanel detail={detail.data} />}
    </Drawer>
  </Flex>;
}
