import { describe, expect, it } from 'vitest';
import { OfflineSyncStorageError } from './errors';
import { createPendingOfflineOperation, type OfflineSyncClock, type OfflineSyncUuidGenerator } from './queue';
import type { OfflineOperationInput } from './types';

const OWNER_ID = '10000000-0000-4000-8000-000000000001';
const AGGREGATE_ID = '20000000-0000-4000-8000-000000000001';
const CLIENT_ID = '30000000-0000-4000-8000-000000000001';
const OPERATION_ID = '40000000-0000-4000-8000-000000000001';
const NOW = '2026-08-22T10:00:00.000Z';

const clock: OfflineSyncClock = { now: () => NOW };
const uuid: OfflineSyncUuidGenerator = { randomUUID: () => OPERATION_ID };

const operations: OfflineOperationInput[] = [
  {
    ownerUserId: OWNER_ID,
    operationType: 'VEHICLE_READING_RECORD',
    aggregateType: 'VEHICLE',
    aggregateId: AGGREGATE_ID,
    payload: { readingType: 'ODOMETER', value: 1250.5, recordedAt: NOW, notes: 'Depot reading' },
  },
  {
    ownerUserId: OWNER_ID,
    operationType: 'TRIP_CHECKPOINT_RECORD',
    aggregateType: 'TRIP',
    aggregateId: AGGREGATE_ID,
    payload: { checkpointType: 'PICKUP', occurredAt: NOW, locationDescription: 'Main depot' },
  },
  {
    ownerUserId: OWNER_ID,
    operationType: 'TRIP_DELAY_RECORD',
    aggregateType: 'TRIP',
    aggregateId: AGGREGATE_ID,
    payload: { delayMinutes: 15, reason: 'Traffic', occurredAt: NOW },
  },
  {
    ownerUserId: OWNER_ID,
    operationType: 'TRIP_INCIDENT_RECORD',
    aggregateType: 'TRIP',
    aggregateId: AGGREGATE_ID,
    payload: { incidentSeverity: 'HIGH', description: 'Tyre damage', occurredAt: NOW },
  },
];

describe('createPendingOfflineOperation', () => {
  it.each(operations)('creates the frozen version-one envelope for $operationType', (input) => {
    const operation = createPendingOfflineOperation(input, CLIENT_ID, clock, uuid);

    expect(operation).toMatchObject({
      operationId: OPERATION_ID,
      operationVersion: 1,
      operationType: input.operationType,
      aggregateType: input.aggregateType,
      aggregateId: AGGREGATE_ID,
      payload: input.payload,
      clientCreatedAt: NOW,
      clientUpdatedAt: NOW,
      clientInstanceId: CLIENT_ID,
      idempotencyKey: OPERATION_ID,
      baseVersion: null,
      ownerUserId: OWNER_ID,
      status: 'PENDING',
      attemptCount: 0,
      createdAt: NOW,
      updatedAt: NOW,
    });
  });

  it('rejects an unsupported operation instead of persisting an ambiguous envelope', () => {
    const unsupported = {
      ...operations[0],
      operationType: 'TRIP_UNKNOWN_RECORD',
    } as unknown as OfflineOperationInput;

    expect(() => createPendingOfflineOperation(unsupported, CLIENT_ID, clock, uuid)).toThrowError(
      expect.objectContaining<Partial<OfflineSyncStorageError>>({ code: 'OFFLINE_SYNC_INVALID_OPERATION' }),
    );
  });

  it('rejects structurally invalid payload data', () => {
    const invalidDelay = {
      ...operations[2],
      payload: { delayMinutes: 0, reason: '', occurredAt: 'not-a-date' },
    } as OfflineOperationInput;

    expect(() => createPendingOfflineOperation(invalidDelay, CLIENT_ID, clock, uuid)).toThrowError(
      expect.objectContaining<Partial<OfflineSyncStorageError>>({ code: 'OFFLINE_SYNC_INVALID_OPERATION' }),
    );
  });

  it('accepts canonical persisted UUID text without requiring RFC version or variant bits', () => {
    const input = { ...operations[1], aggregateId: '60000000-0000-0000-0000-000000000006' } as OfflineOperationInput;

    expect(createPendingOfflineOperation(input, CLIENT_ID, clock, uuid).aggregateId)
      .toBe('60000000-0000-0000-0000-000000000006');
  });
});
