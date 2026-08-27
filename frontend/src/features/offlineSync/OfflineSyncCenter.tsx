import { CloudSyncOutlined, ExclamationCircleOutlined, PauseCircleOutlined, WifiOutlined } from '@ant-design/icons';
import { Alert, Badge, Button, Descriptions, Drawer, Empty, Flex, List, Space, Tooltip, Typography } from 'antd';
import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { useAuth } from '../../auth/AuthContext';
import { OfflineOperationActions } from './OfflineOperationActions';
import { OfflineOperationStatusTag } from './OfflineOperationStatusTag';
import { useOptionalOfflineSync, type OfflineSyncContextValue } from './OfflineSyncProvider';
import { offlineOperationLabel, offlineOperationSummary } from './presentation';
import type { OfflineOperation, OfflineStatusCounts } from './types';

const ZERO_COUNTS: OfflineStatusCounts = { PENDING: 0, SYNCING: 0, SYNCED: 0, FAILED: 0, CONFLICT: 0 };

export function OfflineSyncCenter() {
  const sync = useOptionalOfflineSync?.();
  if (!sync) return null;
  return <OfflineSyncCenterContent sync={sync} />;
}

function OfflineSyncCenterContent({ sync }: { sync: OfflineSyncContextValue }) {
  const { user } = useAuth();
  const [open, setOpen] = useState(false);
  const operationsQuery = useQuery({
    queryKey: ['offline-sync-operations', user?.id, sync.operationsRevision],
    queryFn: () => sync.getOperations?.() ?? Promise.resolve([]),
    enabled: Boolean(user?.id),
    refetchOnWindowFocus: true,
  });
  const operations: OfflineOperation[] = operationsQuery.data ?? [];

  const actionable = operations.filter((operation) => operation.status !== 'SYNCED');
  const counts = operations.reduce((result, operation) => {
    result[operation.status] += 1;
    return result;
  }, { ...ZERO_COUNTS });
  const issueCount = counts.CONFLICT + counts.FAILED;
  const canSync = Boolean(user?.id && sync.onlineHint && !sync.authPaused && !sync.syncing && counts.PENDING > 0);
  const connectivity = !sync.onlineHint ? 'Offline'
    : sync.authPaused ? 'Authentication paused'
      : sync.backendReachable === false ? 'Backend unavailable'
        : sync.syncing ? 'Synchronizing' : 'Online';

  return (
    <>
      <Tooltip title={`Offline synchronization: ${connectivity}`}>
        <Badge count={issueCount} size="small" offset={[-3, 4]}>
          <Button type="text" icon={sync.authPaused ? <PauseCircleOutlined /> : <CloudSyncOutlined spin={sync.syncing} />}
            aria-label={`Offline synchronization status: ${connectivity}`}
            onClick={() => { setOpen(true); void operationsQuery.refetch(); }}>
            <span className="offline-sync-indicator__label">{connectivity}</span>
            {(counts.PENDING + counts.SYNCING) > 0 && <Badge count={counts.PENDING + counts.SYNCING} color="#1677ff" />}
          </Button>
        </Badge>
      </Tooltip>
      <Drawer title="Offline synchronization" open={open} width={560} onClose={() => setOpen(false)}
        extra={<Button type="primary" icon={<CloudSyncOutlined />} loading={sync.syncing} disabled={!canSync}
          onClick={() => void sync.syncNow()}>Sync now</Button>}>
        <Flex vertical gap={16}>
          <Alert type={sync.authPaused || !sync.onlineHint || sync.backendReachable === false ? 'warning' : 'info'} showIcon
            icon={sync.onlineHint ? <WifiOutlined /> : <ExclamationCircleOutlined />}
            message={connectivity}
            description={sync.authPaused
              ? 'Synchronization is paused until your authenticated session is restored. Local operations are retained.'
              : !sync.onlineHint ? 'Reconnect to synchronize pending operations.'
                : sync.backendReachable === false ? 'The backend could not be reached. Pending operations remain local.'
                  : 'Owner-scoped local operations are shown below.'} />
          <Descriptions size="small" bordered column={2} items={[
            { key: 'pending', label: 'Pending', children: counts.PENDING },
            { key: 'syncing', label: 'Syncing', children: counts.SYNCING },
            { key: 'conflict', label: 'Conflicts', children: counts.CONFLICT },
            { key: 'failed', label: 'Failed', children: counts.FAILED },
          ]} />
          <List loading={operationsQuery.isLoading} dataSource={actionable}
            locale={{ emptyText: <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="No offline operations need attention" /> }}
            renderItem={(operation) => <List.Item key={operation.operationId}
              actions={[<OfflineOperationActions key="actions" operation={operation} compact />]}>
              <List.Item.Meta
                title={<Space wrap><Typography.Text strong>{offlineOperationLabel(operation)}</Typography.Text>
                  <OfflineOperationStatusTag status={operation.status} /></Space>}
                description={<Flex vertical gap={3}>
                  <Typography.Text>{offlineOperationSummary(operation)}</Typography.Text>
                  <Typography.Text type="secondary">{operation.aggregateType} · {operation.aggregateId}</Typography.Text>
                  {operation.lastErrorCode && <Typography.Text type="danger" code>{operation.lastErrorCode}</Typography.Text>}
                  {operation.lastErrorMessage && <Typography.Text type="secondary">{operation.lastErrorMessage}</Typography.Text>}
                </Flex>}
              />
            </List.Item>} />
        </Flex>
      </Drawer>
    </>
  );
}
