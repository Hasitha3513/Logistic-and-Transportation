export const OFFLINE_OPERATION_TYPES = [
  'VEHICLE_READING_RECORD',
  'TRIP_CHECKPOINT_RECORD',
  'TRIP_DELAY_RECORD',
  'TRIP_INCIDENT_RECORD',
] as const;

export type OfflineOperationType = (typeof OFFLINE_OPERATION_TYPES)[number];

export const OFFLINE_AGGREGATE_TYPES = ['VEHICLE', 'TRIP'] as const;

export type OfflineAggregateType = (typeof OFFLINE_AGGREGATE_TYPES)[number];

export const OFFLINE_OPERATION_STATUSES = [
  'PENDING',
  'SYNCING',
  'SYNCED',
  'FAILED',
  'CONFLICT',
] as const;

export type OfflineOperationStatus = (typeof OFFLINE_OPERATION_STATUSES)[number];

export type VehicleReadingType = 'ODOMETER' | 'ENGINE_HOURS';

export interface VehicleReadingPayload {
  readingType: VehicleReadingType;
  value: number;
  recordedAt: string;
  notes?: string;
}

export type TripCheckpointType =
  | 'DEPARTURE'
  | 'ARRIVAL'
  | 'PICKUP'
  | 'DELIVERY'
  | 'REST_STOP'
  | 'CUSTOM';

export interface TripCheckpointPayload {
  checkpointType: TripCheckpointType;
  occurredAt: string;
  locationId?: string;
  locationDescription?: string;
  remarks?: string;
}

export interface TripDelayPayload {
  delayMinutes: number;
  reason: string;
  occurredAt: string;
  locationId?: string;
  locationDescription?: string;
  remarks?: string;
}

export type TripIncidentSeverity = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';

export interface TripIncidentPayload {
  incidentSeverity: TripIncidentSeverity;
  description: string;
  occurredAt: string;
  locationId?: string;
  locationDescription?: string;
  remarks?: string;
}

export interface OfflineOperationPayloadMap {
  VEHICLE_READING_RECORD: VehicleReadingPayload;
  TRIP_CHECKPOINT_RECORD: TripCheckpointPayload;
  TRIP_DELAY_RECORD: TripDelayPayload;
  TRIP_INCIDENT_RECORD: TripIncidentPayload;
}

export type OfflineOperationInput = {
  [Type in OfflineOperationType]: {
    ownerUserId: string;
    operationType: Type;
    aggregateType: Type extends 'VEHICLE_READING_RECORD' ? 'VEHICLE' : 'TRIP';
    aggregateId: string;
    payload: OfflineOperationPayloadMap[Type];
  };
}[OfflineOperationType];

export type OfflineServerResultStatus =
  | 'APPLIED'
  | 'ALREADY_APPLIED'
  | 'REJECTED'
  | 'CONFLICT'
  | 'RETRYABLE_ERROR';

export type OfflineServerOperation = Pick<
  OfflineOperation,
  | 'operationId'
  | 'operationVersion'
  | 'operationType'
  | 'aggregateType'
  | 'aggregateId'
  | 'payload'
  | 'clientCreatedAt'
  | 'clientUpdatedAt'
  | 'clientInstanceId'
  | 'idempotencyKey'
  | 'baseVersion'
>;

export interface OfflineSyncBatchRequest {
  operations: OfflineServerOperation[];
}

export interface OfflineSyncOperationResult {
  operationId: string;
  status: OfflineServerResultStatus;
  serverTimestamp: string;
  aggregateId: string;
  currentVersion: number | null;
  errorCode: string | null;
  message: string | null;
}

export interface OfflineSyncBatchResponse {
  serverTimestamp: string;
  results: OfflineSyncOperationResult[];
}

interface OfflineOperationBase<Type extends OfflineOperationType> {
  operationId: string;
  operationVersion: 1;
  operationType: Type;
  aggregateType: Type extends 'VEHICLE_READING_RECORD' ? 'VEHICLE' : 'TRIP';
  aggregateId: string;
  payload: OfflineOperationPayloadMap[Type];
  clientCreatedAt: string;
  clientUpdatedAt: string;
  clientInstanceId: string;
  idempotencyKey: string;
  baseVersion: null;
  ownerUserId: string;
  status: OfflineOperationStatus;
  attemptCount: number;
  lastAttemptAt?: string;
  nextAttemptAt?: string;
  lastErrorCode?: string;
  lastErrorMessage?: string;
  serverProcessedAt?: string;
  serverAggregateId?: string;
  serverResultStatus?: OfflineServerResultStatus;
  createdAt: string;
  updatedAt: string;
  discardedAt?: string;
  syncLeaseId?: string;
  syncLeaseExpiresAt?: string;
}

export type OfflineOperation = {
  [Type in OfflineOperationType]: OfflineOperationBase<Type>;
}[OfflineOperationType];

export type OfflineStatusCounts = Record<OfflineOperationStatus, number>;

export interface MarkSyncedResult {
  serverProcessedAt: string;
  serverAggregateId?: string;
  serverResultStatus: OfflineServerResultStatus;
  attemptCount?: number;
  lastAttemptAt?: string;
}

export interface MarkErrorResult {
  errorCode: string;
  errorMessage: string;
  serverProcessedAt?: string;
  serverAggregateId?: string;
  serverResultStatus?: OfflineServerResultStatus;
  attemptCount?: number;
  lastAttemptAt?: string;
}

export interface RetrySchedule extends MarkErrorResult {
  attemptCount: number;
  lastAttemptAt: string;
  nextAttemptAt: string;
}

export interface OfflineOperationStorage {
  initialize(): Promise<void>;
  getClientInstanceId(): Promise<string>;
  enqueue(input: OfflineOperationInput): Promise<OfflineOperation>;
  getById(ownerUserId: string, operationId: string): Promise<OfflineOperation | undefined>;
  getForAggregate(
    ownerUserId: string,
    aggregateType: OfflineAggregateType,
    aggregateId: string,
  ): Promise<OfflineOperation[]>;
  getAllForOwner(ownerUserId: string): Promise<OfflineOperation[]>;
  getPending(ownerUserId: string, now: string, limit: number): Promise<OfflineOperation[]>;
  getNextPendingAt(ownerUserId: string): Promise<string | undefined>;
  claimForSync(
    ownerUserId: string,
    operationIds: readonly string[],
    leaseUntil: string,
  ): Promise<OfflineOperation[]>;
  recoverExpiredClaims(ownerUserId: string, now: string): Promise<number>;
  releaseClaim(ownerUserId: string, operationId: string): Promise<OfflineOperation>;
  markSynced(ownerUserId: string, operationId: string, result: MarkSyncedResult): Promise<OfflineOperation>;
  markFailed(ownerUserId: string, operationId: string, result: MarkErrorResult): Promise<OfflineOperation>;
  markConflict(ownerUserId: string, operationId: string, result: MarkErrorResult): Promise<OfflineOperation>;
  releaseForRetry(ownerUserId: string, operationId: string, retry: RetrySchedule): Promise<OfflineOperation>;
  retryOperation(ownerUserId: string, operationId: string): Promise<OfflineOperation>;
  remove(ownerUserId: string, operationId: string): Promise<boolean>;
  countByStatus(ownerUserId: string): Promise<OfflineStatusCounts>;
  purgeSynced(ownerUserId: string, olderThan: string): Promise<number>;
  countNonSynced(ownerUserId: string): Promise<number>;
}
