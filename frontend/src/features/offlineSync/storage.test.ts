import { IDBFactory } from 'fake-indexeddb';
import { beforeEach, describe, expect, it } from 'vitest';
import { OFFLINE_SYNC_MAX_NON_SYNCED_OPERATIONS } from './constants';
import { OfflineSyncStorageError } from './errors';
import type { OfflineSyncClock, OfflineSyncUuidGenerator } from './queue';
import { IndexedDbOfflineOperationStorage } from './storage';
import type { OfflineOperationInput } from './types';

const OWNER_A = '10000000-0000-4000-8000-000000000001';
const OWNER_B = '10000000-0000-4000-8000-000000000002';
const VEHICLE_ID = '20000000-0000-4000-8000-000000000001';

class MutableClock implements OfflineSyncClock {
  constructor(private value: string) {}
  now(): string { return this.value; }
  set(value: string): void { this.value = value; }
}

class SequentialUuidGenerator implements OfflineSyncUuidGenerator {
  private sequence = 1;
  randomUUID(): string {
    const suffix = this.sequence.toString(16).padStart(12, '0');
    this.sequence += 1;
    return `00000000-0000-4000-8000-${suffix}`;
  }
}

function vehicleReading(ownerUserId = OWNER_A): OfflineOperationInput {
  return {
    ownerUserId,
    operationType: 'VEHICLE_READING_RECORD',
    aggregateType: 'VEHICLE',
    aggregateId: VEHICLE_ID,
    payload: {
      readingType: 'ODOMETER',
      value: 100,
      recordedAt: '2026-08-22T10:00:00.000Z',
    },
  };
}

describe('IndexedDbOfflineOperationStorage', () => {
  let indexedDb: IDBFactory;
  let clock: MutableClock;
  let uuid: SequentialUuidGenerator;
  let databaseName: string;

  beforeEach(() => {
    indexedDb = new IDBFactory();
    clock = new MutableClock('2026-08-22T10:00:00.000Z');
    uuid = new SequentialUuidGenerator();
    databaseName = `offline-sync-${crypto.randomUUID()}`;
  });

  function storage(): IndexedDbOfflineOperationStorage {
    return new IndexedDbOfflineOperationStorage({ databaseName, indexedDb, clock, uuidGenerator: uuid });
  }

  function openDatabase(name: string): Promise<IDBDatabase> {
    return new Promise((resolve, reject) => {
      const request = indexedDb.open(name, 1);
      request.onsuccess = () => resolve(request.result);
      request.onerror = () => reject(request.error);
    });
  }

  it('creates the frozen version-one schema and required indexes', async () => {
    const adapter = storage();
    await adapter.initialize();
    const database = await openDatabase(databaseName);
    const transaction = database.transaction('operations', 'readonly');
    const indexes = [...transaction.objectStore('operations').indexNames];

    expect([...database.objectStoreNames]).toEqual(['metadata', 'operations']);
    expect(indexes).toEqual(['aggregate', 'nextAttemptAt', 'ownerUserId', 'status', 'updatedAt']);
    database.close();
    adapter.close();
  });

  it('generates one client instance per database and reuses it on repeated initialization', async () => {
    const first = storage();
    const firstId = await first.getClientInstanceId();
    await first.initialize();
    await expect(first.getClientInstanceId()).resolves.toBe(firstId);

    const second = new IndexedDbOfflineOperationStorage({
      databaseName: `${databaseName}-other`,
      indexedDb,
      clock,
      uuidGenerator: uuid,
    });
    await expect(second.getClientInstanceId()).resolves.not.toBe(firstId);
    first.close();
    second.close();
  });

  it('persists a stable client instance and queued operation across adapter restart', async () => {
    const first = storage();
    const clientInstanceId = await first.getClientInstanceId();
    const enqueued = await first.enqueue(vehicleReading());
    first.close();

    const reopened = storage();
    await expect(reopened.getClientInstanceId()).resolves.toBe(clientInstanceId);
    await expect(reopened.getById(OWNER_A, enqueued.operationId)).resolves.toEqual(enqueued);
    reopened.close();
  });

  it('filters pending work by owner, due time, ordering, and limit', async () => {
    const adapter = storage();
    const first = await adapter.enqueue(vehicleReading());
    clock.set('2026-08-22T10:01:00.000Z');
    const second = await adapter.enqueue(vehicleReading());
    const ownerB = await adapter.enqueue(vehicleReading(OWNER_B));
    await adapter.claimForSync(OWNER_A, [second.operationId], '2026-08-22T10:05:00.000Z');
    await adapter.releaseForRetry(OWNER_A, second.operationId, {
      attemptCount: 1,
      lastAttemptAt: '2026-08-22T10:01:00.000Z',
      nextAttemptAt: '2026-08-22T11:00:00.000Z',
      errorCode: 'NETWORK',
      errorMessage: 'Unavailable',
    });

    await expect(adapter.getPending(OWNER_A, '2026-08-22T10:30:00.000Z', 1)).resolves.toEqual([first]);
    await expect(adapter.getPending(OWNER_A, '2026-08-22T11:00:00.000Z', 10)).resolves.toEqual([first, expect.objectContaining({ operationId: second.operationId })]);
    await expect(adapter.getById(OWNER_A, ownerB.operationId)).resolves.toBeUndefined();
    adapter.close();
  });

  it('lists aggregate operations for only the authenticated owner', async () => {
    const adapter = storage();
    const ownerA = await adapter.enqueue(vehicleReading(OWNER_A));
    await adapter.enqueue(vehicleReading(OWNER_B));

    await expect(adapter.getForAggregate(OWNER_A, 'VEHICLE', VEHICLE_ID)).resolves.toEqual([ownerA]);
    await expect(adapter.getForAggregate(OWNER_B, 'VEHICLE', VEHICLE_ID)).resolves.toHaveLength(1);
    await expect(adapter.getForAggregate(OWNER_A, 'VEHICLE', crypto.randomUUID())).resolves.toEqual([]);
    adapter.close();
  });

  it('lists all operations only for the authenticated owner', async () => {
    const adapter = storage();
    await adapter.enqueue(vehicleReading(OWNER_A));
    await adapter.enqueue(vehicleReading(OWNER_B));

    await expect(adapter.getAllForOwner(OWNER_A)).resolves.toHaveLength(1);
    await expect(adapter.getAllForOwner(OWNER_B)).resolves.toHaveLength(1);
    adapter.close();
  });

  it('retries a failed operation without changing its identity, payload, owner, history, or attempt count', async () => {
    const adapter = storage();
    const operation = await adapter.enqueue(vehicleReading());
    await adapter.claimForSync(OWNER_A, [operation.operationId], '2026-08-22T10:05:00.000Z');
    await adapter.markFailed(OWNER_A, operation.operationId, {
      errorCode: 'OFFLINE_SYNC_BACKEND_UNAVAILABLE',
      errorMessage: 'Backend unavailable',
      attemptCount: 5,
      lastAttemptAt: '2026-08-22T10:01:00.000Z',
    });
    clock.set('2026-08-22T10:10:00.000Z');

    const retried = await adapter.retryOperation(OWNER_A, operation.operationId);

    expect(retried).toMatchObject({
      operationId: operation.operationId,
      idempotencyKey: operation.idempotencyKey,
      ownerUserId: OWNER_A,
      payload: operation.payload,
      createdAt: operation.createdAt,
      status: 'PENDING',
      attemptCount: 5,
      lastAttemptAt: '2026-08-22T10:01:00.000Z',
      lastErrorCode: undefined,
      lastErrorMessage: undefined,
      nextAttemptAt: undefined,
    });
    await expect(adapter.retryOperation(OWNER_B, operation.operationId)).rejects.toMatchObject({
      code: 'OFFLINE_SYNC_OWNERSHIP_MISMATCH',
    });
    adapter.close();
  });

  it('atomically prevents two adapters from claiming the same operation', async () => {
    const first = storage();
    const second = storage();
    const operation = await first.enqueue(vehicleReading());

    const [firstClaim, secondClaim] = await Promise.all([
      first.claimForSync(OWNER_A, [operation.operationId], '2026-08-22T10:05:00.000Z'),
      second.claimForSync(OWNER_A, [operation.operationId], '2026-08-22T10:05:00.000Z'),
    ]);

    expect(firstClaim.length + secondClaim.length).toBe(1);
    expect([...firstClaim, ...secondClaim][0]).toMatchObject({ status: 'SYNCING' });
    first.close();
    second.close();
  });

  it('recovers an expired claim without incrementing attempts or losing data', async () => {
    const adapter = storage();
    const operation = await adapter.enqueue(vehicleReading());
    await adapter.claimForSync(OWNER_A, [operation.operationId], '2026-08-22T10:05:00.000Z');

    await expect(adapter.recoverExpiredClaims(OWNER_A, '2026-08-22T10:05:01.000Z')).resolves.toBe(1);
    await expect(adapter.getById(OWNER_A, operation.operationId)).resolves.toMatchObject({
      status: 'PENDING',
      attemptCount: 0,
      payload: operation.payload,
      syncLeaseId: undefined,
      syncLeaseExpiresAt: undefined,
    });
    adapter.close();
  });

  it('does not recover a claim whose lease is still active', async () => {
    const adapter = storage();
    const operation = await adapter.enqueue(vehicleReading());
    await adapter.claimForSync(OWNER_A, [operation.operationId], '2026-08-22T10:05:00.000Z');

    await expect(adapter.recoverExpiredClaims(OWNER_A, '2026-08-22T10:04:59.000Z')).resolves.toBe(0);
    await expect(adapter.getById(OWNER_A, operation.operationId)).resolves.toMatchObject({ status: 'SYNCING' });
    adapter.close();
  });

  it('enforces legal synced, failed, conflict, and retry transitions', async () => {
    const adapter = storage();
    const synced = await adapter.enqueue(vehicleReading());
    await expect(adapter.markSynced(OWNER_A, synced.operationId, {
      serverProcessedAt: '2026-08-22T10:02:00.000Z',
      serverResultStatus: 'APPLIED',
    })).rejects.toMatchObject({ code: 'OFFLINE_SYNC_INVALID_STATE_TRANSITION' });
    await adapter.claimForSync(OWNER_A, [synced.operationId], '2026-08-22T10:05:00.000Z');
    await expect(adapter.markSynced(OWNER_A, synced.operationId, {
      serverProcessedAt: '2026-08-22T10:02:00.000Z',
      serverAggregateId: VEHICLE_ID,
      serverResultStatus: 'APPLIED',
    })).resolves.toMatchObject({ status: 'SYNCED', serverResultStatus: 'APPLIED' });

    const failed = await adapter.enqueue(vehicleReading());
    await adapter.claimForSync(OWNER_A, [failed.operationId], '2026-08-22T10:05:00.000Z');
    await expect(adapter.markFailed(OWNER_A, failed.operationId, { errorCode: 'SERVER', errorMessage: 'Rejected' }))
      .resolves.toMatchObject({ status: 'FAILED', lastErrorCode: 'SERVER' });
    await expect(adapter.releaseForRetry(OWNER_A, failed.operationId, {
      attemptCount: 1,
      lastAttemptAt: '2026-08-22T10:02:00.000Z',
      nextAttemptAt: '2026-08-22T10:10:00.000Z',
      errorCode: 'SERVER',
      errorMessage: 'Retry later',
    })).resolves.toMatchObject({ status: 'PENDING', attemptCount: 1 });

    const conflict = await adapter.enqueue(vehicleReading());
    await adapter.claimForSync(OWNER_A, [conflict.operationId], '2026-08-22T10:05:00.000Z');
    await expect(adapter.markConflict(OWNER_A, conflict.operationId, { errorCode: 'VERSION', errorMessage: 'Conflict' }))
      .resolves.toMatchObject({ status: 'CONFLICT' });
    await expect(adapter.countByStatus(OWNER_A)).resolves.toEqual({
      PENDING: 1,
      SYNCING: 0,
      SYNCED: 1,
      FAILED: 0,
      CONFLICT: 1,
    });
    adapter.close();
  });

  it('keeps all reads, counts, claims, transitions, and deletion owner-scoped', async () => {
    const adapter = storage();
    const operation = await adapter.enqueue(vehicleReading(OWNER_A));

    await expect(adapter.getById(OWNER_B, operation.operationId)).resolves.toBeUndefined();
    await expect(adapter.getPending(OWNER_B, clock.now(), 10)).resolves.toEqual([]);
    await expect(adapter.claimForSync(OWNER_B, [operation.operationId], '2026-08-22T10:05:00.000Z')).resolves.toEqual([]);
    await expect(adapter.countNonSynced(OWNER_B)).resolves.toBe(0);
    await expect(adapter.countByStatus(OWNER_B)).resolves.toEqual({
      PENDING: 0,
      SYNCING: 0,
      SYNCED: 0,
      FAILED: 0,
      CONFLICT: 0,
    });
    await expect(adapter.remove(OWNER_B, operation.operationId)).resolves.toBe(false);
    await expect(adapter.markFailed(OWNER_B, operation.operationId, { errorCode: 'X', errorMessage: 'X' }))
      .rejects.toMatchObject({ code: 'OFFLINE_SYNC_OWNERSHIP_MISMATCH' });
    await expect(adapter.getById(OWNER_A, operation.operationId)).resolves.toEqual(operation);
    adapter.close();
  });

  it('allows deletion only for terminal operations', async () => {
    const adapter = storage();
    const pending = await adapter.enqueue(vehicleReading());
    await expect(adapter.remove(OWNER_A, pending.operationId)).rejects.toMatchObject({
      code: 'OFFLINE_SYNC_INVALID_STATE_TRANSITION',
    });
    await adapter.claimForSync(OWNER_A, [pending.operationId], '2026-08-22T10:05:00.000Z');
    await adapter.markFailed(OWNER_A, pending.operationId, { errorCode: 'X', errorMessage: 'X' });
    await expect(adapter.remove(OWNER_A, pending.operationId)).resolves.toBe(true);
    await expect(adapter.getById(OWNER_A, pending.operationId)).resolves.toBeUndefined();
    adapter.close();
  });

  it('purges only owner-scoped synced records older than the retention threshold', async () => {
    const adapter = storage();
    const oldSynced = await adapter.enqueue(vehicleReading());
    await adapter.claimForSync(OWNER_A, [oldSynced.operationId], '2026-08-22T10:05:00.000Z');
    clock.set('2026-08-15T09:00:00.000Z');
    await adapter.markSynced(OWNER_A, oldSynced.operationId, {
      serverProcessedAt: '2026-08-15T09:00:00.000Z',
      serverResultStatus: 'APPLIED',
    });
    const pending = await adapter.enqueue(vehicleReading());
    const failed = await adapter.enqueue(vehicleReading());
    await adapter.claimForSync(OWNER_A, [failed.operationId], '2026-08-22T10:05:00.000Z');
    await adapter.markFailed(OWNER_A, failed.operationId, { errorCode: 'OLD', errorMessage: 'Retain me' });
    const conflict = await adapter.enqueue(vehicleReading());
    await adapter.claimForSync(OWNER_A, [conflict.operationId], '2026-08-22T10:05:00.000Z');
    await adapter.markConflict(OWNER_A, conflict.operationId, { errorCode: 'OLD', errorMessage: 'Retain me' });
    clock.set('2026-08-22T10:00:00.000Z');
    const recentSynced = await adapter.enqueue(vehicleReading());
    await adapter.claimForSync(OWNER_A, [recentSynced.operationId], '2026-08-22T10:05:00.000Z');
    await adapter.markSynced(OWNER_A, recentSynced.operationId, {
      serverProcessedAt: '2026-08-22T10:01:00.000Z',
      serverResultStatus: 'APPLIED',
    });
    const ownerBSynced = await adapter.enqueue(vehicleReading(OWNER_B));
    await adapter.claimForSync(OWNER_B, [ownerBSynced.operationId], '2026-08-22T10:05:00.000Z');
    await adapter.markSynced(OWNER_B, ownerBSynced.operationId, {
      serverProcessedAt: '2026-08-22T10:01:00.000Z',
      serverResultStatus: 'APPLIED',
    });

    await expect(adapter.purgeSynced(OWNER_A, '2026-08-15T10:00:00.000Z')).resolves.toBe(1);
    await expect(adapter.getById(OWNER_A, oldSynced.operationId)).resolves.toBeUndefined();
    await expect(adapter.getById(OWNER_A, pending.operationId)).resolves.toBeDefined();
    await expect(adapter.getById(OWNER_A, failed.operationId)).resolves.toMatchObject({ status: 'FAILED' });
    await expect(adapter.getById(OWNER_A, conflict.operationId)).resolves.toMatchObject({ status: 'CONFLICT' });
    await expect(adapter.getById(OWNER_A, recentSynced.operationId)).resolves.toMatchObject({ status: 'SYNCED' });
    await expect(adapter.getById(OWNER_B, ownerBSynced.operationId)).resolves.toBeDefined();
    adapter.close();
  });

  it('accepts operation 1000 and rejects operation 1001 without losing queued data', async () => {
    const adapter = storage();
    const firstBatch = Array.from({ length: OFFLINE_SYNC_MAX_NON_SYNCED_OPERATIONS - 1 }, () => adapter.enqueue(vehicleReading()));
    const operations = await Promise.all(firstBatch);
    await expect(adapter.countNonSynced(OWNER_A)).resolves.toBe(999);
    await expect(adapter.enqueue(vehicleReading())).resolves.toMatchObject({ status: 'PENDING' });
    await expect(adapter.countNonSynced(OWNER_A)).resolves.toBe(1_000);
    await expect(adapter.enqueue(vehicleReading())).rejects.toEqual(
      expect.objectContaining<Partial<OfflineSyncStorageError>>({ code: 'OFFLINE_SYNC_LOCAL_CAPACITY_EXCEEDED' }),
    );
    await expect(adapter.countNonSynced(OWNER_A)).resolves.toBe(1_000);
    await adapter.claimForSync(OWNER_A, [operations[0].operationId], '2026-08-22T10:05:00.000Z');
    await adapter.markSynced(OWNER_A, operations[0].operationId, {
      serverProcessedAt: '2026-08-22T10:01:00.000Z',
      serverResultStatus: 'APPLIED',
    });
    await expect(adapter.enqueue(vehicleReading())).resolves.toMatchObject({ status: 'PENDING' });
    await expect(adapter.countNonSynced(OWNER_A)).resolves.toBe(1_000);
    adapter.close();
  }, 30_000);
});
