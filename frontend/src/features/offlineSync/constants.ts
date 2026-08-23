export const OFFLINE_SYNC_DATABASE_NAME = 'transport-logistics-offline';
export const OFFLINE_SYNC_DATABASE_VERSION = 1;
export const OFFLINE_SYNC_OPERATION_STORE = 'operations';
export const OFFLINE_SYNC_METADATA_STORE = 'metadata';
export const OFFLINE_SYNC_MAX_NON_SYNCED_OPERATIONS = 1_000;
export const OFFLINE_SYNC_SYNCED_RETENTION_DAYS = 7;
export const OFFLINE_SYNC_BATCH_SIZE = 50;
export const OFFLINE_SYNC_CLAIM_LEASE_MILLISECONDS = 30_000;
export const OFFLINE_SYNC_MAX_AUTOMATIC_ATTEMPTS = 10;

export const OFFLINE_SYNC_INDEXES = {
  ownerUserId: 'ownerUserId',
  status: 'status',
  nextAttemptAt: 'nextAttemptAt',
  updatedAt: 'updatedAt',
  aggregate: 'aggregate',
} as const;
