import { OfflineSyncStorageError } from './errors';
import type {
  DeliveryPodOfflineSyncPayload,
  OfflineOperation,
  OfflineOperationInput,
  TripCheckpointPayload,
  TripDelayPayload,
  TripIncidentPayload,
  VehicleReadingPayload,
} from './types';

export interface OfflineSyncClock {
  now(): string;
}

export interface OfflineSyncUuidGenerator {
  randomUUID(): string;
}

export const systemOfflineSyncClock: OfflineSyncClock = {
  now: () => new Date().toISOString(),
};

export const cryptoOfflineSyncUuidGenerator: OfflineSyncUuidGenerator = {
  randomUUID: () => crypto.randomUUID(),
};

const UUID_PATTERN = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;
const OFFSET_TIMESTAMP_PATTERN = /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(?:\.\d+)?(?:Z|[+-]\d{2}:\d{2})$/;

function invalid(message: string): never {
  throw new OfflineSyncStorageError('OFFLINE_SYNC_INVALID_OPERATION', message);
}

function requireUuid(value: string, field: string): string {
  const normalized = value.trim().toLowerCase();
  if (!UUID_PATTERN.test(normalized)) {
    invalid(`${field} must be a UUID`);
  }
  return normalized;
}

function requireText(value: string, field: string): string {
  const normalized = value.trim();
  if (normalized.length === 0) {
    invalid(`${field} is required`);
  }
  return normalized;
}

function optionalText(value: string | undefined, field: string, maximumLength: number): void {
  if (value !== undefined && value.length > maximumLength) {
    invalid(`${field} cannot exceed ${maximumLength} characters`);
  }
}

function requireTimestamp(value: string, field: string): string {
  if (!OFFSET_TIMESTAMP_PATTERN.test(value) || !Number.isFinite(Date.parse(value))) {
    invalid(`${field} must be an ISO-8601 offset timestamp`);
  }
  return value;
}

function validateVehicleReading(payload: VehicleReadingPayload): VehicleReadingPayload {
  if (!payload || typeof payload !== 'object') invalid('payload is required');
  if (!['ODOMETER', 'ENGINE_HOURS'].includes(payload.readingType)) {
    invalid('payload.readingType is unsupported');
  }
  if (!Number.isFinite(payload.value) || payload.value < 0) {
    invalid('payload.value must be a non-negative number');
  }
  const scaledValue = payload.value * 1_000;
  if (Math.abs(scaledValue - Math.round(scaledValue)) > Number.EPSILON * Math.max(1, Math.abs(scaledValue))) {
    invalid('payload.value cannot exceed three decimal places');
  }
  requireTimestamp(payload.recordedAt, 'payload.recordedAt');
  optionalText(payload.notes, 'payload.notes', 1_000);
  return payload;
}

function validateCheckpoint(payload: TripCheckpointPayload): TripCheckpointPayload {
  if (!payload || typeof payload !== 'object') invalid('payload is required');
  if (!['DEPARTURE', 'ARRIVAL', 'PICKUP', 'DELIVERY', 'REST_STOP', 'CUSTOM'].includes(payload.checkpointType)) {
    invalid('payload.checkpointType is unsupported');
  }
  requireTimestamp(payload.occurredAt, 'payload.occurredAt');
  if (payload.locationId !== undefined) {
    requireUuid(payload.locationId, 'payload.locationId');
  }
  optionalText(payload.locationDescription, 'payload.locationDescription', 255);
  optionalText(payload.remarks, 'payload.remarks', 2_000);
  return payload;
}

function validateDelay(payload: TripDelayPayload): TripDelayPayload {
  if (!payload || typeof payload !== 'object') invalid('payload is required');
  if (!Number.isInteger(payload.delayMinutes) || payload.delayMinutes <= 0) {
    invalid('payload.delayMinutes must be a positive integer');
  }
  if (requireText(payload.reason, 'payload.reason').length > 500) invalid('payload.reason cannot exceed 500 characters');
  requireTimestamp(payload.occurredAt, 'payload.occurredAt');
  if (payload.locationId !== undefined) {
    requireUuid(payload.locationId, 'payload.locationId');
  }
  optionalText(payload.locationDescription, 'payload.locationDescription', 255);
  optionalText(payload.remarks, 'payload.remarks', 2_000);
  return payload;
}

function validateIncident(payload: TripIncidentPayload): TripIncidentPayload {
  if (!payload || typeof payload !== 'object') invalid('payload is required');
  if (!['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'].includes(payload.incidentSeverity)) {
    invalid('payload.incidentSeverity is unsupported');
  }
  if (requireText(payload.description, 'payload.description').length > 500) invalid('payload.description cannot exceed 500 characters');
  requireTimestamp(payload.occurredAt, 'payload.occurredAt');
  if (payload.locationId !== undefined) {
    requireUuid(payload.locationId, 'payload.locationId');
  }
  optionalText(payload.locationDescription, 'payload.locationDescription', 255);
  optionalText(payload.remarks, 'payload.remarks', 2_000);
  return payload;
}

function validateDeliveryPodOfflineSync(
  payload: DeliveryPodOfflineSyncPayload,
): DeliveryPodOfflineSyncPayload {
  if (!payload || typeof payload !== 'object') invalid('payload is required');
  requireUuid(payload.deliveryId, 'payload.deliveryId');
  if (!Number.isInteger(payload.deliveryVersion) || payload.deliveryVersion < 0) {
    invalid('payload.deliveryVersion must be a non-negative integer');
  }
  if (!Array.isArray(payload.evidenceList) || payload.evidenceList.length === 0) {
    invalid('payload.evidenceList must contain at least one evidence item');
  }
  optionalText(payload.signerName, 'payload.signerName', 200);
  optionalText(payload.signerRelationship, 'payload.signerRelationship', 100);
  return payload;
}

export function createPendingOfflineOperation(
  input: OfflineOperationInput,
  clientInstanceId: string,
  clock: OfflineSyncClock,
  uuidGenerator: OfflineSyncUuidGenerator,
): OfflineOperation {
  if (!input || typeof input !== 'object') invalid('operation input is required');
  const ownerUserId = requireUuid(input.ownerUserId, 'ownerUserId');
  const aggregateId = requireUuid(input.aggregateId, 'aggregateId');
  const normalizedClientInstanceId = requireUuid(clientInstanceId, 'clientInstanceId');
  const operationId = requireUuid(uuidGenerator.randomUUID(), 'operationId');
  const now = requireTimestamp(clock.now(), 'clock.now()');

  const common = {
    operationId,
    operationVersion: 1 as const,
    aggregateId,
    clientCreatedAt: now,
    clientUpdatedAt: now,
    clientInstanceId: normalizedClientInstanceId,
    idempotencyKey: operationId,
    baseVersion: null,
    ownerUserId,
    status: 'PENDING' as const,
    attemptCount: 0,
    createdAt: now,
    updatedAt: now,
  };

  switch (input.operationType) {
    case 'VEHICLE_READING_RECORD':
      if (input.aggregateType !== 'VEHICLE') invalid('vehicle readings require a VEHICLE aggregate');
      return { ...common, operationType: input.operationType, aggregateType: 'VEHICLE', payload: validateVehicleReading(input.payload) };
    case 'TRIP_CHECKPOINT_RECORD':
      if (input.aggregateType !== 'TRIP') invalid('trip checkpoints require a TRIP aggregate');
      return { ...common, operationType: input.operationType, aggregateType: 'TRIP', payload: validateCheckpoint(input.payload) };
    case 'TRIP_DELAY_RECORD':
      if (input.aggregateType !== 'TRIP') invalid('trip delays require a TRIP aggregate');
      return { ...common, operationType: input.operationType, aggregateType: 'TRIP', payload: validateDelay(input.payload) };
    case 'TRIP_INCIDENT_RECORD':
      if (input.aggregateType !== 'TRIP') invalid('trip incidents require a TRIP aggregate');
      return { ...common, operationType: input.operationType, aggregateType: 'TRIP', payload: validateIncident(input.payload) };
    case 'DELIVERY_POD_OFFLINE_SYNC':
      if (input.aggregateType !== 'DELIVERY') invalid('delivery POD sync requires a DELIVERY aggregate');
      return { ...common, operationType: input.operationType, aggregateType: 'DELIVERY', payload: validateDeliveryPodOfflineSync(input.payload) };
    default:
      return invalid('operationType is unsupported');
  }
}
