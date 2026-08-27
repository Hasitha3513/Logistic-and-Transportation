import type { OfflineOperation, OfflineOperationStatus } from './types';

export const OFFLINE_STATUS_PRESENTATION: Record<OfflineOperationStatus, { label: string; detailLabel: string; color: string }> = {
  PENDING: { label: 'Pending', detailLabel: 'Pending sync', color: 'gold' },
  SYNCING: { label: 'Syncing', detailLabel: 'Syncing', color: 'processing' },
  SYNCED: { label: 'Synced', detailLabel: 'Synced', color: 'success' },
  FAILED: { label: 'Failed', detailLabel: 'Failed', color: 'error' },
  CONFLICT: { label: 'Conflict', detailLabel: 'Conflict', color: 'warning' },
};

export interface OfflineOperationActions {
  open: boolean;
  refresh: boolean;
  retry: boolean;
  discard: boolean;
}

const NON_RETRYABLE_CODES = [
  'FORBIDDEN',
  'CONFLICT',
  'IDEMPOTENCY_MISMATCH',
  'PAYLOAD_INVALID',
  'VALIDATION',
];

export function getOfflineOperationActions(operation: OfflineOperation): OfflineOperationActions {
  const code = operation.lastErrorCode?.toUpperCase() ?? '';
  const terminal = operation.status === 'FAILED' || operation.status === 'CONFLICT';
  return {
    open: terminal,
    refresh: terminal,
    retry: operation.status === 'FAILED' && !NON_RETRYABLE_CODES.some((value) => code.includes(value)),
    discard: terminal,
  };
}

export function offlineOperationLabel(operation: OfflineOperation): string {
  const labels: Record<OfflineOperation['operationType'], string> = {
    VEHICLE_READING_RECORD: 'Vehicle reading',
    TRIP_CHECKPOINT_RECORD: 'Trip checkpoint',
    TRIP_DELAY_RECORD: 'Trip delay',
    TRIP_INCIDENT_RECORD: 'Trip incident',
  };
  return labels[operation.operationType];
}

export function offlineOperationSummary(operation: OfflineOperation): string {
  if (operation.operationType === 'VEHICLE_READING_RECORD') {
    return `${operation.payload.readingType.replace('_', ' ')} · ${operation.payload.value}`;
  }
  if (operation.operationType === 'TRIP_CHECKPOINT_RECORD') {
    return operation.payload.locationDescription ?? operation.payload.checkpointType.replace('_', ' ');
  }
  if (operation.operationType === 'TRIP_DELAY_RECORD') {
    return `${operation.payload.delayMinutes} min · ${operation.payload.reason}`;
  }
  return `${operation.payload.incidentSeverity} · ${operation.payload.description}`;
}
