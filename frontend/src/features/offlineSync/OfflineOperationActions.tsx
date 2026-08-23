import { DeleteOutlined, FolderOpenOutlined, ReloadOutlined, SyncOutlined } from '@ant-design/icons';
import { App, Button, Space, Tooltip } from 'antd';
import { getOfflineOperationActions } from './presentation';
import type { OfflineOperation } from './types';
import { useOfflineOperationActions } from './useOfflineOperationActions';

export function OfflineOperationActions({ operation, compact = false }: {
  operation: OfflineOperation;
  compact?: boolean;
}) {
  const { message, modal } = App.useApp();
  const actions = getOfflineOperationActions(operation);
  const handlers = useOfflineOperationActions();
  const buttonType = compact ? 'text' : 'link';

  const run = async (action: 'retry' | 'refresh') => {
    try {
      await handlers[action](operation);
      void message.success(action === 'retry' ? 'Operation queued for retry' : 'Server data refreshed');
    } catch (error: unknown) {
      void message.error(error instanceof Error ? error.message : 'Offline operation action failed');
    }
  };

  return (
    <Space size={2} wrap aria-label="Offline operation actions">
      {actions.open && <Tooltip title="Open owning record"><Button type={buttonType} size="small" icon={<FolderOpenOutlined />}
        onClick={() => handlers.open(operation)}>Open</Button></Tooltip>}
      {actions.refresh && <Tooltip title="Refresh current server data"><Button type={buttonType} size="small" icon={<ReloadOutlined />}
        onClick={() => void run('refresh')}>Refresh</Button></Tooltip>}
      {actions.retry && <Tooltip title="Retry with the same operation ID and payload"><Button type={buttonType} size="small" icon={<SyncOutlined />}
        onClick={() => void run('retry')}>Retry</Button></Tooltip>}
      {actions.discard && <Tooltip title="Remove this unsynchronized local copy"><Button type={buttonType} danger size="small" icon={<DeleteOutlined />}
        onClick={() => modal.confirm({
          title: 'Discard unsynchronized operation?',
          content: 'This operation was not synchronized. Discarding removes only the local copy; server data is unchanged.',
          okText: 'Discard local copy',
          okButtonProps: { danger: true },
          onOk: async () => {
            const removed = await handlers.discard(operation);
            if (removed) void message.success('Local operation discarded');
          },
        })}>Discard</Button></Tooltip>}
    </Space>
  );
}
