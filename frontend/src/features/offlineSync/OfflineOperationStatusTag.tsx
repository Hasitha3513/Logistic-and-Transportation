import { Tag } from 'antd';
import { OFFLINE_STATUS_PRESENTATION } from './presentation';
import type { OfflineOperationStatus } from './types';

export function OfflineOperationStatusTag({ status }: { status: OfflineOperationStatus }) {
  const presentation = OFFLINE_STATUS_PRESENTATION[status];
  return <Tag color={presentation.color}>{presentation.label}</Tag>;
}
