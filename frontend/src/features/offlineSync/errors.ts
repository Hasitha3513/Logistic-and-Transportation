export type OfflineSyncErrorCode =
  | 'OFFLINE_SYNC_DATABASE_OPEN_FAILED'
  | 'OFFLINE_SYNC_TRANSACTION_FAILED'
  | 'OFFLINE_SYNC_LOCAL_CAPACITY_EXCEEDED'
  | 'OFFLINE_SYNC_OPERATION_NOT_FOUND'
  | 'OFFLINE_SYNC_OWNERSHIP_MISMATCH'
  | 'OFFLINE_SYNC_INVALID_STATE_TRANSITION'
  | 'OFFLINE_SYNC_INVALID_OPERATION';

export class OfflineSyncStorageError extends Error {
  readonly code: OfflineSyncErrorCode;

  constructor(code: OfflineSyncErrorCode, message: string, cause?: unknown) {
    super(message, { cause });
    this.name = 'OfflineSyncStorageError';
    this.code = code;
  }
}
