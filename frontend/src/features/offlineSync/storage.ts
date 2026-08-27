import {
  OFFLINE_SYNC_DATABASE_NAME,
  OFFLINE_SYNC_DATABASE_VERSION,
  OFFLINE_SYNC_INDEXES,
  OFFLINE_SYNC_MAX_NON_SYNCED_OPERATIONS,
  OFFLINE_SYNC_METADATA_STORE,
  OFFLINE_SYNC_OPERATION_STORE,
} from './constants';
import { OfflineSyncStorageError } from './errors';
import {
  createPendingOfflineOperation,
  cryptoOfflineSyncUuidGenerator,
  systemOfflineSyncClock,
  type OfflineSyncClock,
  type OfflineSyncUuidGenerator,
} from './queue';
import {
  OFFLINE_OPERATION_STATUSES,
  type MarkErrorResult,
  type MarkSyncedResult,
  type OfflineOperation,
  type OfflineOperationInput,
  type OfflineOperationStatus,
  type OfflineOperationStorage,
  type OfflineStatusCounts,
  type RetrySchedule,
} from './types';

interface MetadataRecord {
  key: string;
  value: string;
}

export interface IndexedDbOfflineOperationStorageOptions {
  databaseName?: string;
  indexedDb?: IDBFactory;
  clock?: OfflineSyncClock;
  uuidGenerator?: OfflineSyncUuidGenerator;
  onChange?: (ownerUserId: string) => void;
}

const CLIENT_INSTANCE_ID_KEY = 'clientInstanceId';
const NON_SYNCED_STATUSES: readonly OfflineOperationStatus[] = ['PENDING', 'SYNCING', 'FAILED', 'CONFLICT'];
const REMOVABLE_STATUSES: readonly OfflineOperationStatus[] = ['SYNCED', 'FAILED', 'CONFLICT'];

function requestResult<Result>(request: IDBRequest<Result>): Promise<Result> {
  return new Promise((resolve, reject) => {
    request.addEventListener('success', () => resolve(request.result), { once: true });
    request.addEventListener('error', () => reject(request.error), { once: true });
  });
}

function transactionComplete(transaction: IDBTransaction): Promise<void> {
  return new Promise((resolve, reject) => {
    transaction.addEventListener('complete', () => resolve(), { once: true });
    transaction.addEventListener('abort', () => reject(transaction.error), { once: true });
    transaction.addEventListener('error', () => reject(transaction.error), { once: true });
  });
}

function assertTimestamp(value: string, field: string): void {
  if (!Number.isFinite(Date.parse(value))) {
    throw new OfflineSyncStorageError('OFFLINE_SYNC_INVALID_OPERATION', `${field} must be an ISO-8601 timestamp`);
  }
}

function timestampAtOrBefore(value: string, boundary: string): boolean {
  return Date.parse(value) <= Date.parse(boundary);
}

function timestampBefore(value: string, boundary: string): boolean {
  return Date.parse(value) < Date.parse(boundary);
}

function assertOwner(ownerUserId: string): void {
  if (ownerUserId.trim().length === 0) {
    throw new OfflineSyncStorageError('OFFLINE_SYNC_INVALID_OPERATION', 'ownerUserId is required');
  }
}

function statusCounts(): OfflineStatusCounts {
  return {
    PENDING: 0,
    SYNCING: 0,
    SYNCED: 0,
    FAILED: 0,
    CONFLICT: 0,
  };
}

export class IndexedDbOfflineOperationStorage implements OfflineOperationStorage {
  private readonly databaseName: string;
  private readonly indexedDb: IDBFactory;
  private readonly clock: OfflineSyncClock;
  private readonly uuidGenerator: OfflineSyncUuidGenerator;
  private readonly onChange?: (ownerUserId: string) => void;
  private databasePromise?: Promise<IDBDatabase>;
  private clientInstancePromise?: Promise<string>;

  constructor(options: IndexedDbOfflineOperationStorageOptions = {}) {
    this.databaseName = options.databaseName ?? OFFLINE_SYNC_DATABASE_NAME;
    this.indexedDb = options.indexedDb ?? indexedDB;
    this.clock = options.clock ?? systemOfflineSyncClock;
    this.uuidGenerator = options.uuidGenerator ?? cryptoOfflineSyncUuidGenerator;
    this.onChange = options.onChange;
  }

  async initialize(): Promise<void> {
    await this.getClientInstanceId();
  }

  getClientInstanceId(): Promise<string> {
    this.clientInstancePromise ??= this.loadOrCreateClientInstanceId().catch((error: unknown) => {
      this.clientInstancePromise = undefined;
      throw error;
    });
    return this.clientInstancePromise;
  }

  async enqueue(input: OfflineOperationInput): Promise<OfflineOperation> {
    const operation = createPendingOfflineOperation(
      input,
      await this.getClientInstanceId(),
      this.clock,
      this.uuidGenerator,
    );
    const database = await this.database();
    const transaction = database.transaction(OFFLINE_SYNC_OPERATION_STORE, 'readwrite');
    const done = transactionComplete(transaction);
    const store = transaction.objectStore(OFFLINE_SYNC_OPERATION_STORE);
    const statusIndex = store.index(OFFLINE_SYNC_INDEXES.status);

    try {
      const counts = await Promise.all(
        NON_SYNCED_STATUSES.map((status) => requestResult(statusIndex.count(status))),
      );
      if (counts.reduce((total, count) => total + count, 0) >= OFFLINE_SYNC_MAX_NON_SYNCED_OPERATIONS) {
        transaction.abort();
        await done.catch(() => undefined);
        throw new OfflineSyncStorageError(
          'OFFLINE_SYNC_LOCAL_CAPACITY_EXCEEDED',
          `Offline operation capacity of ${OFFLINE_SYNC_MAX_NON_SYNCED_OPERATIONS} has been reached`,
        );
      }
      store.add(operation);
      await done;
      this.onChange?.(operation.ownerUserId);
      return operation;
    } catch (error: unknown) {
      if (error instanceof OfflineSyncStorageError) throw error;
      throw this.transactionError('Unable to enqueue offline operation', error);
    }
  }

  async getById(ownerUserId: string, operationId: string): Promise<OfflineOperation | undefined> {
    assertOwner(ownerUserId);
    const operation = await this.readOperation(operationId);
    return operation?.ownerUserId === ownerUserId ? operation : undefined;
  }

  async getForAggregate(
    ownerUserId: string,
    aggregateType: OfflineOperation['aggregateType'],
    aggregateId: string,
  ): Promise<OfflineOperation[]> {
    assertOwner(ownerUserId);
    const operations = await this.readByOwner(ownerUserId);
    return operations
      .filter((operation) => operation.aggregateType === aggregateType && operation.aggregateId === aggregateId)
      .sort((left, right) => Date.parse(right.createdAt) - Date.parse(left.createdAt));
  }

  async getAllForOwner(ownerUserId: string): Promise<OfflineOperation[]> {
    return (await this.readByOwner(ownerUserId))
      .sort((left, right) => Date.parse(right.updatedAt) - Date.parse(left.updatedAt));
  }

  async getPending(ownerUserId: string, now: string, limit: number): Promise<OfflineOperation[]> {
    assertOwner(ownerUserId);
    assertTimestamp(now, 'now');
    if (!Number.isInteger(limit) || limit < 0) {
      throw new OfflineSyncStorageError('OFFLINE_SYNC_INVALID_OPERATION', 'limit must be a non-negative integer');
    }
    const operations = await this.readByOwner(ownerUserId);
    return operations
      .filter((operation) => operation.status === 'PENDING' && (!operation.nextAttemptAt || timestampAtOrBefore(operation.nextAttemptAt, now)))
      .sort((left, right) => Date.parse(left.createdAt) - Date.parse(right.createdAt) || left.operationId.localeCompare(right.operationId))
      .slice(0, limit);
  }

  async getNextPendingAt(ownerUserId: string): Promise<string | undefined> {
    const scheduled = (await this.readByOwner(ownerUserId))
      .filter((operation) => operation.status === 'PENDING' && operation.nextAttemptAt !== undefined)
      .map((operation) => operation.nextAttemptAt as string)
      .sort((left, right) => Date.parse(left) - Date.parse(right));
    return scheduled[0];
  }

  async claimForSync(
    ownerUserId: string,
    operationIds: readonly string[],
    leaseUntil: string,
  ): Promise<OfflineOperation[]> {
    assertOwner(ownerUserId);
    assertTimestamp(leaseUntil, 'leaseUntil');
    if (operationIds.length === 0) return [];

    const database = await this.database();
    const transaction = database.transaction(OFFLINE_SYNC_OPERATION_STORE, 'readwrite');
    const done = transactionComplete(transaction);
    const store = transaction.objectStore(OFFLINE_SYNC_OPERATION_STORE);
    const now = this.clock.now();
    const leaseId = this.uuidGenerator.randomUUID();

    try {
      const uniqueIds = [...new Set(operationIds)];
      const candidates = await Promise.all(uniqueIds.map((id) => requestResult(store.get(id)) as Promise<OfflineOperation | undefined>));
      const claimed = candidates
        .filter((operation): operation is OfflineOperation =>
          operation !== undefined
          && operation.ownerUserId === ownerUserId
          && operation.status === 'PENDING'
          && (!operation.nextAttemptAt || timestampAtOrBefore(operation.nextAttemptAt, now)))
        .map((operation) => ({
          ...operation,
          status: 'SYNCING' as const,
          nextAttemptAt: undefined,
          syncLeaseId: leaseId,
          syncLeaseExpiresAt: leaseUntil,
          clientUpdatedAt: now,
          updatedAt: now,
        }));
      claimed.forEach((operation) => store.put(operation));
      await done;
      if (claimed.length > 0) this.onChange?.(ownerUserId);
      return claimed;
    } catch (error: unknown) {
      throw this.transactionError('Unable to claim offline operations', error);
    }
  }

  async recoverExpiredClaims(ownerUserId: string, now: string): Promise<number> {
    assertOwner(ownerUserId);
    assertTimestamp(now, 'now');
    return this.updateOwnedSet(ownerUserId, (operation) => {
      if (operation.status !== 'SYNCING' || (operation.syncLeaseExpiresAt && !timestampAtOrBefore(operation.syncLeaseExpiresAt, now))) {
        return undefined;
      }
      return {
        ...operation,
        status: 'PENDING',
        syncLeaseId: undefined,
        syncLeaseExpiresAt: undefined,
        clientUpdatedAt: now,
        updatedAt: now,
      };
    });
  }

  releaseClaim(ownerUserId: string, operationId: string): Promise<OfflineOperation> {
    return this.transition(ownerUserId, operationId, ['SYNCING'], (operation, now) => ({
      ...operation,
      status: 'PENDING',
      syncLeaseId: undefined,
      syncLeaseExpiresAt: undefined,
      clientUpdatedAt: now,
      updatedAt: now,
    }));
  }

  markSynced(ownerUserId: string, operationId: string, result: MarkSyncedResult): Promise<OfflineOperation> {
    assertTimestamp(result.serverProcessedAt, 'serverProcessedAt');
    this.assertAttemptMetadata(result.attemptCount, result.lastAttemptAt);
    return this.transition(ownerUserId, operationId, ['SYNCING'], (operation, now) => ({
      ...operation,
      status: 'SYNCED',
      serverProcessedAt: result.serverProcessedAt,
      serverAggregateId: result.serverAggregateId,
      serverResultStatus: result.serverResultStatus,
      attemptCount: result.attemptCount ?? operation.attemptCount,
      lastAttemptAt: result.lastAttemptAt ?? operation.lastAttemptAt,
      lastErrorCode: undefined,
      lastErrorMessage: undefined,
      nextAttemptAt: undefined,
      syncLeaseId: undefined,
      syncLeaseExpiresAt: undefined,
      clientUpdatedAt: now,
      updatedAt: now,
    }));
  }

  markFailed(ownerUserId: string, operationId: string, result: MarkErrorResult): Promise<OfflineOperation> {
    return this.markTerminalError(ownerUserId, operationId, 'FAILED', result);
  }

  markConflict(ownerUserId: string, operationId: string, result: MarkErrorResult): Promise<OfflineOperation> {
    return this.markTerminalError(ownerUserId, operationId, 'CONFLICT', result);
  }

  releaseForRetry(ownerUserId: string, operationId: string, retry: RetrySchedule): Promise<OfflineOperation> {
    if (!Number.isInteger(retry.attemptCount) || retry.attemptCount < 0) {
      throw new OfflineSyncStorageError('OFFLINE_SYNC_INVALID_OPERATION', 'attemptCount must be a non-negative integer');
    }
    assertTimestamp(retry.lastAttemptAt, 'lastAttemptAt');
    assertTimestamp(retry.nextAttemptAt, 'nextAttemptAt');
    return this.transition(ownerUserId, operationId, ['SYNCING', 'FAILED', 'CONFLICT'], (operation, now) => ({
      ...operation,
      status: 'PENDING',
      attemptCount: retry.attemptCount,
      lastAttemptAt: retry.lastAttemptAt,
      nextAttemptAt: retry.nextAttemptAt,
      lastErrorCode: retry.errorCode,
      lastErrorMessage: retry.errorMessage,
      syncLeaseId: undefined,
      syncLeaseExpiresAt: undefined,
      clientUpdatedAt: now,
      updatedAt: now,
    }));
  }

  retryOperation(ownerUserId: string, operationId: string): Promise<OfflineOperation> {
    return this.transition(ownerUserId, operationId, ['FAILED'], (operation, now) => ({
      ...operation,
      status: 'PENDING',
      nextAttemptAt: undefined,
      lastErrorCode: undefined,
      lastErrorMessage: undefined,
      serverProcessedAt: undefined,
      serverAggregateId: undefined,
      serverResultStatus: undefined,
      syncLeaseId: undefined,
      syncLeaseExpiresAt: undefined,
      clientUpdatedAt: now,
      updatedAt: now,
    }));
  }

  async remove(ownerUserId: string, operationId: string): Promise<boolean> {
    assertOwner(ownerUserId);
    const database = await this.database();
    const transaction = database.transaction(OFFLINE_SYNC_OPERATION_STORE, 'readwrite');
    const done = transactionComplete(transaction);
    const store = transaction.objectStore(OFFLINE_SYNC_OPERATION_STORE);
    try {
      const operation = await requestResult(store.get(operationId)) as OfflineOperation | undefined;
      if (!operation || operation.ownerUserId !== ownerUserId) {
        await done;
        return false;
      }
      if (!REMOVABLE_STATUSES.includes(operation.status)) {
        transaction.abort();
        await done.catch(() => undefined);
        throw new OfflineSyncStorageError(
          'OFFLINE_SYNC_INVALID_STATE_TRANSITION',
          `Cannot remove an operation in ${operation.status} status`,
        );
      }
      store.delete(operationId);
      await done;
      this.onChange?.(ownerUserId);
      return true;
    } catch (error: unknown) {
      if (error instanceof OfflineSyncStorageError) throw error;
      throw this.transactionError('Unable to remove offline operation', error);
    }
  }

  async countByStatus(ownerUserId: string): Promise<OfflineStatusCounts> {
    const counts = statusCounts();
    (await this.readByOwner(ownerUserId)).forEach((operation) => {
      counts[operation.status] += 1;
    });
    return counts;
  }

  async purgeSynced(ownerUserId: string, olderThan: string): Promise<number> {
    assertTimestamp(olderThan, 'olderThan');
    return this.deleteOwnedSet(ownerUserId, (operation) => operation.status === 'SYNCED' && timestampBefore(operation.updatedAt, olderThan));
  }

  async countNonSynced(ownerUserId: string): Promise<number> {
    return (await this.readByOwner(ownerUserId)).filter((operation) => operation.status !== 'SYNCED').length;
  }

  close(): void {
    void this.databasePromise?.then((database) => database.close());
    this.databasePromise = undefined;
    this.clientInstancePromise = undefined;
  }

  private database(): Promise<IDBDatabase> {
    this.databasePromise ??= this.openDatabase().catch((error: unknown) => {
      this.databasePromise = undefined;
      throw error;
    });
    return this.databasePromise;
  }

  private openDatabase(): Promise<IDBDatabase> {
    return new Promise((resolve, reject) => {
      let request: IDBOpenDBRequest;
      try {
        request = this.indexedDb.open(this.databaseName, OFFLINE_SYNC_DATABASE_VERSION);
      } catch (error: unknown) {
        reject(new OfflineSyncStorageError('OFFLINE_SYNC_DATABASE_OPEN_FAILED', 'Unable to open offline storage', error));
        return;
      }

      request.addEventListener('upgradeneeded', () => {
        const database = request.result;
        const operations = database.createObjectStore(OFFLINE_SYNC_OPERATION_STORE, { keyPath: 'operationId' });
        operations.createIndex(OFFLINE_SYNC_INDEXES.ownerUserId, 'ownerUserId');
        operations.createIndex(OFFLINE_SYNC_INDEXES.status, 'status');
        operations.createIndex(OFFLINE_SYNC_INDEXES.nextAttemptAt, 'nextAttemptAt');
        operations.createIndex(OFFLINE_SYNC_INDEXES.updatedAt, 'updatedAt');
        operations.createIndex(OFFLINE_SYNC_INDEXES.aggregate, ['aggregateType', 'aggregateId']);
        database.createObjectStore(OFFLINE_SYNC_METADATA_STORE, { keyPath: 'key' });
      }, { once: true });
      request.addEventListener('success', () => resolve(request.result), { once: true });
      request.addEventListener('error', () => reject(
        new OfflineSyncStorageError('OFFLINE_SYNC_DATABASE_OPEN_FAILED', 'Unable to open offline storage', request.error),
      ), { once: true });
    });
  }

  private async loadOrCreateClientInstanceId(): Promise<string> {
    const database = await this.database();
    const transaction = database.transaction(OFFLINE_SYNC_METADATA_STORE, 'readwrite');
    const done = transactionComplete(transaction);
    const store = transaction.objectStore(OFFLINE_SYNC_METADATA_STORE);
    try {
      const existing = await requestResult(store.get(CLIENT_INSTANCE_ID_KEY)) as MetadataRecord | undefined;
      if (existing) {
        await done;
        return existing.value;
      }
      const value = this.uuidGenerator.randomUUID().toLowerCase();
      store.add({ key: CLIENT_INSTANCE_ID_KEY, value } satisfies MetadataRecord);
      await done;
      return value;
    } catch (error: unknown) {
      throw this.transactionError('Unable to initialize the offline client instance', error);
    }
  }

  private async readOperation(operationId: string): Promise<OfflineOperation | undefined> {
    const database = await this.database();
    const transaction = database.transaction(OFFLINE_SYNC_OPERATION_STORE, 'readonly');
    const done = transactionComplete(transaction);
    try {
      const result = await requestResult(transaction.objectStore(OFFLINE_SYNC_OPERATION_STORE).get(operationId));
      await done;
      return result as OfflineOperation | undefined;
    } catch (error: unknown) {
      throw this.transactionError('Unable to read offline operation', error);
    }
  }

  private async readByOwner(ownerUserId: string): Promise<OfflineOperation[]> {
    assertOwner(ownerUserId);
    const database = await this.database();
    const transaction = database.transaction(OFFLINE_SYNC_OPERATION_STORE, 'readonly');
    const done = transactionComplete(transaction);
    try {
      const result = await requestResult(
        transaction.objectStore(OFFLINE_SYNC_OPERATION_STORE).index(OFFLINE_SYNC_INDEXES.ownerUserId).getAll(ownerUserId),
      );
      await done;
      return result as OfflineOperation[];
    } catch (error: unknown) {
      throw this.transactionError('Unable to read offline operations', error);
    }
  }

  private markTerminalError(
    ownerUserId: string,
    operationId: string,
    status: 'FAILED' | 'CONFLICT',
    result: MarkErrorResult,
  ): Promise<OfflineOperation> {
    if (result.serverProcessedAt !== undefined) assertTimestamp(result.serverProcessedAt, 'serverProcessedAt');
    this.assertAttemptMetadata(result.attemptCount, result.lastAttemptAt);
    return this.transition(ownerUserId, operationId, ['SYNCING'], (operation, now) => ({
      ...operation,
      status,
      lastErrorCode: result.errorCode,
      lastErrorMessage: result.errorMessage,
      serverProcessedAt: result.serverProcessedAt,
      serverAggregateId: result.serverAggregateId,
      serverResultStatus: result.serverResultStatus,
      attemptCount: result.attemptCount ?? operation.attemptCount,
      lastAttemptAt: result.lastAttemptAt ?? operation.lastAttemptAt,
      nextAttemptAt: undefined,
      syncLeaseId: undefined,
      syncLeaseExpiresAt: undefined,
      clientUpdatedAt: now,
      updatedAt: now,
    }));
  }

  private assertAttemptMetadata(attemptCount: number | undefined, lastAttemptAt: string | undefined): void {
    if (attemptCount !== undefined && (!Number.isInteger(attemptCount) || attemptCount < 0)) {
      throw new OfflineSyncStorageError('OFFLINE_SYNC_INVALID_OPERATION', 'attemptCount must be a non-negative integer');
    }
    if (lastAttemptAt !== undefined) assertTimestamp(lastAttemptAt, 'lastAttemptAt');
  }

  private async transition(
    ownerUserId: string,
    operationId: string,
    allowedStatuses: readonly OfflineOperationStatus[],
    update: (operation: OfflineOperation, now: string) => OfflineOperation,
  ): Promise<OfflineOperation> {
    assertOwner(ownerUserId);
    const database = await this.database();
    const transaction = database.transaction(OFFLINE_SYNC_OPERATION_STORE, 'readwrite');
    const done = transactionComplete(transaction);
    const store = transaction.objectStore(OFFLINE_SYNC_OPERATION_STORE);
    try {
      const operation = await requestResult(store.get(operationId)) as OfflineOperation | undefined;
      if (!operation) {
        transaction.abort();
        await done.catch(() => undefined);
        throw new OfflineSyncStorageError('OFFLINE_SYNC_OPERATION_NOT_FOUND', 'Offline operation is unavailable');
      }
      if (operation.ownerUserId !== ownerUserId) {
        transaction.abort();
        await done.catch(() => undefined);
        throw new OfflineSyncStorageError('OFFLINE_SYNC_OWNERSHIP_MISMATCH', 'Offline operation is unavailable');
      }
      if (!allowedStatuses.includes(operation.status)) {
        transaction.abort();
        await done.catch(() => undefined);
        throw new OfflineSyncStorageError(
          'OFFLINE_SYNC_INVALID_STATE_TRANSITION',
          `Cannot transition an operation in ${operation.status} status`,
        );
      }
      const updated = update(operation, this.clock.now());
      store.put(updated);
      await done;
      this.onChange?.(ownerUserId);
      return updated;
    } catch (error: unknown) {
      if (error instanceof OfflineSyncStorageError) throw error;
      throw this.transactionError('Unable to update offline operation', error);
    }
  }

  private async updateOwnedSet(
    ownerUserId: string,
    update: (operation: OfflineOperation) => OfflineOperation | undefined,
  ): Promise<number> {
    assertOwner(ownerUserId);
    const database = await this.database();
    const transaction = database.transaction(OFFLINE_SYNC_OPERATION_STORE, 'readwrite');
    const done = transactionComplete(transaction);
    const store = transaction.objectStore(OFFLINE_SYNC_OPERATION_STORE);
    try {
      const operations = await requestResult(store.index(OFFLINE_SYNC_INDEXES.ownerUserId).getAll(ownerUserId)) as OfflineOperation[];
      let updatedCount = 0;
      operations.forEach((operation) => {
        const updated = update(operation);
        if (updated) {
          store.put(updated);
          updatedCount += 1;
        }
      });
      await done;
      if (updatedCount > 0) this.onChange?.(ownerUserId);
      return updatedCount;
    } catch (error: unknown) {
      throw this.transactionError('Unable to update offline operations', error);
    }
  }

  private async deleteOwnedSet(ownerUserId: string, predicate: (operation: OfflineOperation) => boolean): Promise<number> {
    assertOwner(ownerUserId);
    const database = await this.database();
    const transaction = database.transaction(OFFLINE_SYNC_OPERATION_STORE, 'readwrite');
    const done = transactionComplete(transaction);
    const store = transaction.objectStore(OFFLINE_SYNC_OPERATION_STORE);
    try {
      const operations = await requestResult(store.index(OFFLINE_SYNC_INDEXES.ownerUserId).getAll(ownerUserId)) as OfflineOperation[];
      const matches = operations.filter(predicate);
      matches.forEach((operation) => store.delete(operation.operationId));
      await done;
      if (matches.length > 0) this.onChange?.(ownerUserId);
      return matches.length;
    } catch (error: unknown) {
      throw this.transactionError('Unable to purge offline operations', error);
    }
  }

  private transactionError(message: string, cause: unknown): OfflineSyncStorageError {
    return new OfflineSyncStorageError('OFFLINE_SYNC_TRANSACTION_FAILED', message, cause);
  }
}

export { OFFLINE_OPERATION_STATUSES };
